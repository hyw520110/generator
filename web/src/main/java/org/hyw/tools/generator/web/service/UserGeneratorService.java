package org.hyw.tools.generator.web.service;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.PostConstruct;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.hyw.tools.generator.Generator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class UserGeneratorService {

	private static final Logger logger = LoggerFactory.getLogger(UserGeneratorService.class);

	@Value("${app.user-config-dir:${user.home}/.generator/users}")
	private String userConfigDir;

	@Value("${app.user-config-strategy:ip}")
	private String userConfigStrategy;

	@Value("${app.global-config-file:${user.home}/.generator/default.yml}")
	private String globalConfigFile;

	@Value("${app.persist-password:false}")
	private boolean persistPassword;

	@Value("${app.trusted-proxy-enabled:false}")
	private boolean trustedProxyEnabled;

	private Generator defaultGenerator;
	private final Map<String, Generator> userGenerators = new ConcurrentHashMap<>();
	private final Map<String, Object> userLocks = new ConcurrentHashMap<>();

	@PostConstruct
	public void init() {
		Generator globalDefault = Generator.loadFrom(new File(globalConfigFile));
		defaultGenerator = globalDefault != null ? globalDefault : Generator.getInstance();
	}

	public Generator getGenerator(HttpServletRequest request) {
		String clientKey = resolveClientKey(request);
		return userGenerators.computeIfAbsent(clientKey, this::loadUserGenerator);
	}

	public void saveGenerator(HttpServletRequest request) {
		String clientKey = resolveClientKey(request);
		getGenerator(request).save(userConfigFile(clientKey), persistPassword);
	}

	public void saveGlobalDefault(HttpServletRequest request) {
		getGenerator(request).save(new File(globalConfigFile), persistPassword);
		Generator loaded = Generator.loadFrom(new File(globalConfigFile));
		if (loaded != null) {
			defaultGenerator = loaded;
		}
	}

	public <T> T withGeneratorLock(HttpServletRequest request, LockedGeneratorAction<T> action) throws Exception {
		Object lock = lockFor(request);
		synchronized (lock) {
			return action.apply(getGenerator(request));
		}
	}

	public Object lockFor(HttpServletRequest request) {
		return userLocks.computeIfAbsent(resolveClientKey(request), key -> new Object());
	}

	public File userConfigFile(String clientKey) {
		return new File(userConfigDir, clientKey + ".yml");
	}

	public String resolveClientKey(HttpServletRequest request) {
		String strategy = StringUtils.defaultString(userConfigStrategy, "ip").toLowerCase(Locale.ROOT);
		if ("session".equals(strategy) && request != null) {
			return "session-" + hash(request.getSession(true).getId());
		}
		if ("client".equals(strategy)) {
			String clientId = resolveClientId(request);
			if (StringUtils.isNotBlank(clientId)) {
				return "client-" + hash(clientId);
			}
		}
		return "ip-" + hash(resolveClientIp(request));
	}

	public String getUserConfigStrategy() {
		return userConfigStrategy;
	}

	public boolean isPersistPassword() {
		return persistPassword;
	}

	public boolean isTrustedProxyEnabled() {
		return trustedProxyEnabled;
	}

	private Generator loadUserGenerator(String clientKey) {
		File configFile = userConfigFile(clientKey);
		Generator loaded = Generator.loadFrom(configFile);
		if (loaded != null) {
			logger.info("已加载用户配置: {}", configFile.getAbsolutePath());
			return loaded;
		}
		Generator copied = defaultGenerator != null ? defaultGenerator.copy() : Generator.getInstance().copy();
		copied.save(configFile, persistPassword);
		logger.info("已基于全局默认配置创建用户配置: {}", configFile.getAbsolutePath());
		return copied;
	}

	private String resolveClientId(HttpServletRequest request) {
		if (request == null) {
			return null;
		}
		String clientId = request.getHeader("X-Generator-Client-Id");
		if (StringUtils.isNotBlank(clientId)) {
			return clientId;
		}
		Cookie[] cookies = request.getCookies();
		if (cookies != null) {
			for (Cookie cookie : cookies) {
				if ("GENERATOR_CLIENT_ID".equals(cookie.getName())) {
					return cookie.getValue();
				}
			}
		}
		return null;
	}

	private String resolveClientIp(HttpServletRequest request) {
		if (request == null) {
			return "local";
		}
		if (trustedProxyEnabled) {
			String forwardedFor = request.getHeader("X-Forwarded-For");
			if (StringUtils.isNotBlank(forwardedFor)) {
				String[] ips = StringUtils.split(forwardedFor, ",");
				if (ips != null && ips.length > 0) {
					return StringUtils.trim(ips[0]);
				}
			}
			String realIp = request.getHeader("X-Real-IP");
			if (StringUtils.isNotBlank(realIp)) {
				return realIp;
			}
		}
		return request.getRemoteAddr();
	}

	private String hash(String value) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] bytes = digest.digest(StringUtils.defaultString(value, "unknown").getBytes(StandardCharsets.UTF_8));
			StringBuilder builder = new StringBuilder();
			for (int i = 0; i < 8 && i < bytes.length; i++) {
				builder.append(String.format("%02x", bytes[i]));
			}
			return builder.toString();
		} catch (Exception e) {
			return StringUtils.defaultString(value, "unknown").replaceAll("[^a-zA-Z0-9._-]", "_");
		}
	}
}
