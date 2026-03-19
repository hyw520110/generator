#if($VUE)
package ${controllerPackage};

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

import org.springframework.web.servlet.ModelAndView;

import com.alibaba.fastjson.JSON;
import ${rootPackage}.${projectName}.api.enums.StatusCode;
import ${rootPackage}.${projectName}.api.vo.Result;
import ${rootPackage}.${projectName}.${moduleName}.service.TokenService;
import ${rootPackage}.${projectName}.${moduleName}.vo.LoginResponseVo;
import ${rootPackage}.${projectName}.${moduleName}.vo.LoginVo;
import ${rootPackage}.${projectName}.${moduleName}.vo.ResourceResponseVo;
import ${rootPackage}.${projectName}.${moduleName}.vo.UserInfo;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

#set($comment="登录示例(jwt、shiro)")
#parse('/templates/comments/comment.vm')
@Api(value = "登录", tags = "登录相关")
@RestController
public class LoginController {
	private static final Logger logger=LoggerFactory.getLogger(LoginController.class);
	protected static final String AUTH_TYPE_WEB = "web";
	protected static final String AUTH_TYPE_APP = "app";
	
	@Autowired
	private TokenService tokenService;
	private static UserInfo user;
	private static List<ResourceResponseVo> userResources;
	
	static{init();}
	public static void init() {
		if(null==user||null==userResources) {
			load();
		}
	}
	public static void load() {
		user = loadTestData(UserInfo.class);
		userResources = (List<ResourceResponseVo>) loadTestListData(ResourceResponseVo.class);
	}
	// TODO 测试方法
	public static <T> T loadTestData(Class<T> t) {
		try {
			return JSON.parseObject(loadTestData(t.getSimpleName()), t);
		} catch (IOException ignore) {
		}
		return null;
	}

	public static List<?> loadTestListData(Class<?> t) {
		try {
			return JSON.parseArray(loadTestData(t.getSimpleName()), t);
		} catch (IOException e) {
		}
		return null;
	}

	private static <T> String loadTestData(String fileName) throws IOException {
		URL url = LoginController.class.getResource("/data/" + fileName + ".json");
		if (null == url) {
			return null;
		}
		String content = FileUtils.readFileToString(new File(url.getPath()));
		logger.info("load test data:{}",content);
		return content;
	}

	@RequestMapping(value ="/auth/login", method = { RequestMethod.POST, RequestMethod.PUT })
	public Result<Object> login(@Valid LoginVo login, HttpServletRequest req, HttpServletResponse resp) {
		init();
		if (!user.getUserName().equals(login.getUserName()) || !user.getPassword().equals(login.getPassword())) {
			return Result.error(401, "账户或密码错误");
		}
		String toke = tokenService.refreshToken(String.valueOf(user.getUserId()), AUTH_TYPE_WEB);
		return Result.ok(new LoginResponseVo(toke, user));
	}

	@ApiOperation(value = "用户注销", notes = "用户注销")
	@RequestMapping(value ="/auth/logout/{userId}", method = { RequestMethod.POST, RequestMethod.PUT })
	public Result<Object> logout(@ApiParam(value = "用户ID", required = false) @PathVariable("userId") String userId) {
		if (StringUtils.isNotBlank(userId)) {
			tokenService.clearToken(String.valueOf(userId), AUTH_TYPE_WEB);
		}
		return Result.ok("注销成功");
	}

	@RequestMapping(value = "/unauth")
	@ApiOperation(value = "未授权的访问", notes = "未授权的访问")
	public Result<?> unauth() {
		return Result.error(StatusCode.UNAUTH_ERROR.getCode(), StatusCode.UNAUTH_ERROR.getDesc());
	}

	@RequestMapping(value="/user/info",method = RequestMethod.GET)
	public Result<Object> info(HttpServletRequest req) {
		return Result.ok(user);
	}

	@RequestMapping(value = "/user/authcache/{userId}", method = { RequestMethod.POST, RequestMethod.PUT })
	@ApiOperation(value = "刷新用户权限相关缓存(角色、权限)", notes = "刷新用户权限相关缓存(角色、权限)")
	public Result<?> refreshUserAuthCache(
			@ApiParam(value = "用户ID", required = true) @PathVariable("userId") Long userId) {
		// TODO
		return Result.ok();
	}

	@RequestMapping(value = "/resource/{userId}/list", method = RequestMethod.GET)
	@ApiOperation(value = "获取指定用户的树状结构资源列表", notes = "获取指定用户的树状结构资源列表")
	public Result<List<ResourceResponseVo>> getUserResources(@PathVariable("userId") Long userId) {
		init();
		return new Result<>(userResources);
	}

	@RequestMapping(value = "/user/{userId}", method = RequestMethod.GET)
	@ApiOperation(value = "获取用户信息", notes = "获取用户信息")
	public Result<UserInfo> getUser(@ApiParam(value = "用户ID", required = true) @PathVariable("userId") Long userId) {
		return new Result<>(user);
	}

}
#end