#if($VUE)
package ${shiroPackage};

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.FileUtils;
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
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import com.alibaba.fastjson.JSON;
import ${enumsPackage}.StatusCode;
import ${rootPackage}.${projectName}.${moduleName}.service.TokenService;
import ${rootPackage}.${projectName}.${moduleName}.vo.Resource;
import ${rootPackage}.${projectName}.${moduleName}.vo.Role;
import ${rootPackage}.${projectName}.${moduleName}.vo.TokenVo;
import ${rootPackage}.${projectName}.${moduleName}.vo.UserAuthCache;

public class ShiroRealm extends AuthorizingRealm {

	private static final Logger log = LoggerFactory.getLogger(ShiroRealm.class);

	@Autowired
	private TokenService tokenService;
	private UserAuthCache userAuthCache;

	@Override
	public boolean supports(AuthenticationToken token) {
		return token instanceof TokenVo;
	}

	public UserAuthCache loadTestData() {
		if (userAuthCache != null) {
			return userAuthCache;
		}
		try {
			URL url = ShiroRealm.class.getResource("/data/Resource.json");
			if (null == url) {
				return null;
			}
			List<Resource> resource = JSON.parseArray(FileUtils.readFileToString(new File(url.getPath())), Resource.class);
			userAuthCache=new UserAuthCache(resource);
		} catch (IOException ignore) {
		}
		return userAuthCache;

	}

	/**
	 * 授权模块，获取用户角色和权限
	 *
	 * @param principals 身份
	 * @return AuthorizationInfo 权限信息
	 */
	@Override
	protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principals) {
		String userId = (String) principals.getPrimaryPrincipal();
		Set<String> roleSet = new HashSet<>();
		Set<String> permissionSet = new HashSet<>();
		SimpleAuthorizationInfo simpleAuthorizationInfo = new SimpleAuthorizationInfo();
		if (loadTestData() == null) {
			return simpleAuthorizationInfo;
		}
		List<Role> userRoles = userAuthCache.getUserRoles();
		List<Resource> userResources = userAuthCache.getUserResources();
		if (!CollectionUtils.isEmpty(userRoles)) {
			for (Role userRole : userRoles) {
				roleSet.add(userRole.getRoleKey());
			}
		}
		simpleAuthorizationInfo.setRoles(roleSet);
		if (!CollectionUtils.isEmpty(userResources)) {
			for (Resource userResource : userResources) {
				String resourcePerms = userResource.getResourcePerms();
				if (!StringUtils.isEmpty(resourcePerms)) {
					permissionSet.add(resourcePerms);
				}
			}
		}
		simpleAuthorizationInfo.setStringPermissions(permissionSet);
		return simpleAuthorizationInfo;
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

		TokenVo tokenVo = tokenService.parseToken(token);
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
#end