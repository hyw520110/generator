<#if VUE>
package ${securityPackage!};

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.SimpleAuthenticationInfo;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.subject.PrincipalCollection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import com.alibaba.fastjson.JSON;
<#if global.modules?? && global.modules?size gt 1>
import ${api_dtoPackage!}.StatusCode;
import ${api_dtoPackage!}.ResourceResponseDto;
import ${api_dtoPackage!}.Role;
import ${api_dtoPackage!}.UserAuthCache;
<#else>
import ${dtoPackage!}.StatusCode;
import ${dtoPackage!}.ResourceResponseDto;
import ${dtoPackage!}.Role;
import ${dtoPackage!}.UserAuthCache;
</#if>
import ${servicePackage!}.TokenService;
import ${dtoPackage!}.TokenDto;

public class ShiroRealm extends AuthorizingRealm {

	private static final Logger log = LoggerFactory.getLogger(ShiroRealm.class);

	@Autowired
	private TokenService tokenService;
	private List<ResourceResponseDto> resources;

	@Override
	public boolean supports(AuthenticationToken token) {
	        return token instanceof TokenDto;
	}

		public void loadTestData() {
		        if (resources != null) {
		                return;
		        }
		        try {
		                ClassPathResource resource = new ClassPathResource("data/ResourceResponseDto.json");
		                if (resource.exists()) {
		                        try (InputStream is = resource.getInputStream()) {
		                                byte[] bytes = is.readAllBytes();
		                                resources = JSON.parseArray(new String(bytes), ResourceResponseDto.class);
		                        }
		                }
		        } catch (IOException ignore) {
		                log.error("loadTestData error", ignore);
		        }
		}
	
		/**
		 * 授权模块，获取用户角色和权限
		 *
		 * @param principals 身份
		 * @return AuthorizationInfo 权限信息
		 */
		@Override
		protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principals) {
	        Set<String> roleSet = new HashSet<>();
	        Set<String> permissionSet = new HashSet<>();
	        SimpleAuthorizationInfo simpleAuthorizationInfo = new SimpleAuthorizationInfo();
	        loadTestData();
	        if (CollectionUtils.isEmpty(resources)) {
	                return simpleAuthorizationInfo;
	        }

	        collectPermissions(resources, permissionSet);

	        simpleAuthorizationInfo.setRoles(roleSet);
	        simpleAuthorizationInfo.setStringPermissions(permissionSet);
	        return simpleAuthorizationInfo;
	}

	private void collectPermissions(List<ResourceResponseDto> resourceList, Set<String> permissionSet) {
	        if (CollectionUtils.isEmpty(resourceList)) {
	                return;
	        }
	        for (ResourceResponseDto resource : resourceList) {
	                if (!StringUtils.isEmpty(resource.getResourcePerms())) {
	                        permissionSet.add(resource.getResourcePerms());
	                }
	                collectPermissions(resource.getChildResources(), permissionSet);
	        }
	}
	/**
	 * 用户认证
	 *
	 * @param authenticationToken 身份认证 token
	 * @return AuthenticationInfo 身份认证信息
	 * @throws AuthenticationException 认证相关异常
	 */
	@Override
	protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken authenticationToken) {
		// 这里的 token是从 JWTFilter 的 executeLogin 方法传递过来的,已经经过了解密
		String token = (String) authenticationToken.getCredentials();

		// 校验令牌有效性
		if (!StringUtils.isEmpty(token) && !tokenService.verifyToken(token)) {
			throw new RuntimeException(StatusCode.TOKEN_ERROR.getDesc());
		}

		TokenDto tokenVo = tokenService.parseToken(token);
		String userId = tokenVo.getUserId();
		String type = tokenVo.getType();

		// 校验是否与缓存中的令牌一致
		String cacheToken = tokenService.getCacheToken(userId, type);
		if (!token.equals(cacheToken)) {
			throw new RuntimeException(StatusCode.TOKEN_ERROR.getDesc());
		}

		return new SimpleAuthenticationInfo(userId, token, getName());
	}
}
</#if>