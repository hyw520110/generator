package ${filterPackage!};

import java.io.IOException;
import java.io.PrintWriter;

import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.shiro.authz.UnauthorizedException;
import org.apache.shiro.util.AntPathMatcher;
import org.apache.shiro.web.filter.authc.BasicHttpAuthenticationFilter;
import org.apache.shiro.web.util.WebUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMethod;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
<#if global.modules?? && global.modules?size gt 1>
import ${api_dtoPackage!}.StatusCode;
import ${api_dtoPackage!}.Result;
<#else>
import ${dtoPackage!}.StatusCode;
import ${dtoPackage!}.Result;
</#if>
<#if global.modules?? && global.modules?size gt 1>
import ${rootPackage!}.${projectName!}.api.service.TokenService;
<#else>
import ${servicePackage!}.TokenService;
</#if>
import ${rootPackage!}.${projectName!}.${moduleName!}.utils.SpringUtils;
import ${securityPackage!}.ShiroAuthenticationToken;
<#if global.modules?? && global.modules?size gt 1>
import ${api_dtoPackage!}.Token;
<#else>
import ${dtoPackage!}.Token;
</#if>

public class JWTFilter extends BasicHttpAuthenticationFilter {

	private Logger log = LoggerFactory.getLogger(JWTFilter.class);

	private AntPathMatcher pathMatcher = new AntPathMatcher();

	private String[] excludes = { "/webjars/**", "/swagger-resources/**", "/swagger-ui.html", "/error", "/v2/api-docs", "/v3/api-docs/**",
			"/auth/login", "/auth/logout/**","/druid/**", "/resource/**", "/user/**", "/sys/**"};
	private String HEADER_TOKEN = "X-USER-TOKEN";
	private String CHARSET = "UTF-8";

	@Override
	protected boolean isAccessAllowed(ServletRequest request, ServletResponse response, Object mappedValue)
			throws UnauthorizedException {
		HttpServletRequest httpServletRequest = (HttpServletRequest) request;
		// 跨域时,option请求直接返回正常状态
		if (httpServletRequest.getMethod().equals(RequestMethod.OPTIONS.name())) {
			return true;
		}
		String uri = httpServletRequest.getRequestURI();
		// 读取白名单路由列表
		for (String url : excludes) {
			if (pathMatcher.match(url, uri)) {
				return true;
			}
		}
		// 如果请求头不存在 Token，则可能是执行登陆操作或者是游客状态访问，无需检查 token，直接返回 true
		if (isLoginAttempt(request, response)) {
			return executeLogin(request, response);
		}
		log.warn("not allow uri:{}", uri);
		return false;
	}

	@Override
	protected boolean isLoginAttempt(ServletRequest request, ServletResponse response) {
		HttpServletRequest req = (HttpServletRequest) request;
		String token = req.getHeader(HEADER_TOKEN);
		return !StringUtils.isEmpty(token);
	}

	@Override
	protected boolean executeLogin(ServletRequest request, ServletResponse response) {
		TokenService tokenService = SpringUtils.getBean(TokenService.class);
		HttpServletRequest httpServletRequest = (HttpServletRequest) request;
		String token = httpServletRequest.getHeader(HEADER_TOKEN);
		try {
			Token tokenInfo = tokenService.parseToken(token);
			// 因为登录时未将令牌放入,所以此时需要将令牌放入以便后续Realm进行用户认证
			tokenInfo.setToken(token);
			ShiroAuthenticationToken tokenVo = new ShiroAuthenticationToken(tokenInfo.getUserId(), tokenInfo.getToken());
			getSubject(request, response).login(tokenVo);
			return true;
		} catch (Exception e) {
			log.error("token:{}", token, e);
			return false;
		}
	}

	/**
	 * 构建未授权的请求返回,filter层的异常不受exceptionAdvice控制,这里返回401,把返回的json丢到response中
	 */
	@Override
	protected boolean sendChallenge(ServletRequest request, ServletResponse response) {
		HttpServletResponse httpResponse = WebUtils.toHttp(response);
		String contentType = "application/json;charset=" + CHARSET;
		httpResponse.setContentType(contentType);
		try {
			PrintWriter printWriter = httpResponse.getWriter();
			printWriter.append(JSON.toJSONString(
					Result.error(StatusCode.UNAUTH_ERROR.getCode(), StatusCode.UNAUTH_ERROR.getDesc()),
					SerializerFeature.WriteMapNullValue));
		} catch (IOException e) {
			log.error("sendChallenge error,can not resolve httpServletResponse");
		}

		return false;
	}

	@Override
	protected boolean preHandle(ServletRequest request, ServletResponse response) throws Exception {
		HttpServletRequest httpServletRequest = (HttpServletRequest) request;
		HttpServletResponse httpServletResponse = (HttpServletResponse) response;
		// 跨域时,option请求直接返回正常状态
		if (httpServletRequest.getMethod().equals(RequestMethod.OPTIONS.name())) {
			httpServletResponse.setStatus(HttpStatus.OK.value());
			return false;
		}

		return super.preHandle(request, response);
	}
}
