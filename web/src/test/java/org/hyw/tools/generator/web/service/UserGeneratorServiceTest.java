package org.hyw.tools.generator.web.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Map;

import javax.servlet.http.Cookie;

import org.apache.commons.lang3.StringUtils;
import org.hyw.tools.generator.Generator;
import org.hyw.tools.generator.enums.Component;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;

public class UserGeneratorServiceTest {

	private File tempDir;

	@Before
	public void setUp() throws Exception {
		tempDir = Files.createTempDirectory("generator-user-service-test").toFile();
	}

	@After
	public void tearDown() throws Exception {
		deleteRecursively(tempDir);
	}

	@Test
	public void ipStrategyUsesRemoteAddress() throws Exception {
		UserGeneratorService service = service("ip", false);
		MockHttpServletRequest first = request("10.0.0.1", null, null);
		MockHttpServletRequest second = request("10.0.0.2", null, null);

		assertEquals(service.resolveClientKey(first), service.resolveClientKey(first));
		assertNotEquals(service.resolveClientKey(first), service.resolveClientKey(second));
	}

	@Test
	public void clientStrategyUsesStableBrowserId() throws Exception {
		UserGeneratorService service = service("client", false);
		MockHttpServletRequest first = request("10.0.0.1", "browser-a", null);
		MockHttpServletRequest second = request("10.0.0.2", "browser-a", null);
		MockHttpServletRequest third = request("10.0.0.2", "browser-b", null);

		assertEquals(service.resolveClientKey(first), service.resolveClientKey(second));
		assertNotEquals(service.resolveClientKey(first), service.resolveClientKey(third));
	}

	@Test
	public void clientStrategyIssuesCookieWhenBrowserIdIsMissing() throws Exception {
		UserGeneratorService service = service("client", false);
		MockHttpServletRequest first = request("10.0.0.1", null, null);
		MockHttpServletResponse response = new MockHttpServletResponse();

		String clientId = service.ensureClientId(first, response);
		String firstKey = service.resolveClientKey(first);
		Cookie cookie = response.getCookie("GENERATOR_CLIENT_ID");

		assertTrue(StringUtils.isNotBlank(clientId));
		assertTrue(firstKey.startsWith("client-"));
		assertEquals(clientId, cookie.getValue());
		assertTrue(cookie.isHttpOnly());
		assertEquals("/", cookie.getPath());

		MockHttpServletRequest second = request("10.0.0.2", null, null);
		second.setCookies(new Cookie("GENERATOR_CLIENT_ID", clientId));

		assertEquals(firstKey, service.resolveClientKey(second));
	}

	@Test
	public void sessionStrategyUsesSessionId() throws Exception {
		UserGeneratorService service = service("session", false);
		MockHttpServletRequest first = request("10.0.0.1", null, "session-a");
		MockHttpServletRequest second = request("10.0.0.1", null, "session-b");

		assertNotEquals(service.resolveClientKey(first), service.resolveClientKey(second));
	}

	@Test
	public void saveGeneratorOmitsSensitiveValuesWhenPasswordPersistenceDisabled() throws Exception {
		UserGeneratorService service = service("client", false);
		MockHttpServletRequest request = request("10.0.0.1", "browser-a", null);
		Generator generator = service.getGenerator(request);
		generator.getDataSource().setPwd("db_password_should_not_be_saved");
		Map<String, Object> redis = generator.getComponents().get(Component.REDIS);
		redis.put("spring_redis_password", "redis_password_should_not_be_saved");
		redis.put("apiToken", "token_should_not_be_saved");

		service.saveGenerator(request);

		String saved = new String(Files.readAllBytes(service.userConfigFile(service.resolveClientKey(request)).toPath()),
				StandardCharsets.UTF_8);
		assertFalse(saved.contains("db_password_should_not_be_saved"));
		assertFalse(saved.contains("redis_password_should_not_be_saved"));
		assertFalse(saved.contains("token_should_not_be_saved"));
	}

	@Test
	public void newUserInheritsGlobalDefaultFile() throws Exception {
		UserGeneratorService first = service("client", false);
		MockHttpServletRequest admin = request("10.0.0.1", "admin-browser", null);
		first.getGenerator(admin).getGlobal().setOutputDir(new File(tempDir, "global-default-project").getAbsolutePath());
		first.saveGlobalDefault(admin);

		UserGeneratorService second = service("client", false);
		MockHttpServletRequest user = request("10.0.0.2", "user-browser", null);

		assertEquals("global-default-project", second.getGenerator(user).getGlobal().getProjectName());
	}

	private UserGeneratorService service(String strategy, boolean persistPassword) throws Exception {
		UserGeneratorService service = new UserGeneratorService();
		setField(service, "userConfigDir", new File(tempDir, "users").getAbsolutePath());
		setField(service, "userConfigStrategy", strategy);
		setField(service, "globalConfigFile", new File(tempDir, "default.yml").getAbsolutePath());
		setField(service, "persistPassword", persistPassword);
		setField(service, "trustedProxyEnabled", false);
		service.init();
		return service;
	}

	private MockHttpServletRequest request(String remoteAddr, String clientId, String sessionId) {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setRemoteAddr(remoteAddr);
		if (clientId != null) {
			request.addHeader("X-Generator-Client-Id", clientId);
		}
		if (sessionId != null) {
			request.setSession(new MockHttpSession(null, sessionId));
		}
		return request;
	}

	private void setField(Object target, String name, Object value) throws Exception {
		Field field = target.getClass().getDeclaredField(name);
		field.setAccessible(true);
		field.set(target, value);
	}

	private void deleteRecursively(File file) throws Exception {
		if (file == null || !file.exists()) {
			return;
		}
		if (file.isDirectory()) {
			File[] children = file.listFiles();
			if (children != null) {
				for (File child : children) {
					deleteRecursively(child);
				}
			}
		}
		assertTrue(file.delete());
	}
}
