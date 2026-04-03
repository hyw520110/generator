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

import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

import org.springframework.web.servlet.ModelAndView;

import com.alibaba.fastjson.JSON;
#if($global.modules && $global.modules.size() > 1)
import ${api_dtoPackage}.StatusCode;
import ${api_dtoPackage}.Result;
import ${api_dtoPackage}.LoginResponseDto;
import ${api_dtoPackage}.LoginDto;
import ${api_dtoPackage}.ResourceResponseDto;
import ${api_dtoPackage}.UserInfo;
import ${api_dtoPackage}.Token;
#else
import ${dtoPackage}.StatusCode;
import ${dtoPackage}.Result;
import ${dtoPackage}.LoginResponseDto;
import ${dtoPackage}.LoginDto;
import ${dtoPackage}.ResourceResponseDto;
import ${dtoPackage}.UserInfo;
import ${dtoPackage}.Token;
#end
import ${servicePackage}.TokenService;

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
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;

#set($comment="登录示例(jwt、shiro)")
#parse('/templates/comments/comment.vm')
@Tag(name = "登录")
@RestController
public class LoginController {
	private static final Logger logger=LoggerFactory.getLogger(LoginController.class);
	protected static final String AUTH_TYPE_WEB = "web";
	protected static final String AUTH_TYPE_APP = "app";
	
	@Autowired
	private TokenService tokenService;
	private static UserInfo user;
	private static List<ResourceResponseDto> userResources;
	
	static{init();}
	public static void init() {
		if(null==user||null==userResources) {
			load();
		}
	}
	public static void load() {
		user = loadTestData(UserInfo.class);
		userResources = (List<ResourceResponseDto>) loadTestListData(ResourceResponseDto.class);
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
		URL url = LoginController.class.getResource("/springboot/data/" + fileName + ".json");
		if (null == url) {
			return null;
		}
		String content = FileUtils.readFileToString(new File(url.getPath()));
		logger.info("load test data:{}",content);
		return content;
	}

	@RequestMapping(value ="/auth/login", method = { RequestMethod.POST, RequestMethod.PUT })
	public Result<Object> login(@Valid LoginDto login, HttpServletRequest req, HttpServletResponse resp) {
		init();
		if (!user.getUserName().equals(login.getUserName()) || !user.getPassword().equals(login.getPassword())) {
			return Result.error(401, "账户或密码错误");
		}
		String toke = tokenService.refreshToken(String.valueOf(user.getUserId()), AUTH_TYPE_WEB);
		return Result.ok(new LoginResponseDto(toke, user, userResources));
	}

	@Operation(summary = "用户注销", description = "用户注销")
	@RequestMapping(value ="/auth/logout", method = { RequestMethod.POST, RequestMethod.PUT })
	public Result<Object> logout(HttpServletRequest req) {
		String token = req.getHeader("X-USER-TOKEN");
		if (StringUtils.isNotBlank(token)) {
			Token tokenInfo = tokenService.parseToken(token);
			if (tokenInfo != null && StringUtils.isNotBlank(tokenInfo.getUserId())) {
				tokenService.clearToken(tokenInfo.getUserId(), AUTH_TYPE_WEB);
			}
		}
		return Result.ok("注销成功");
	}

	@RequestMapping(value = "/unauth")
	@Operation(summary = "未授权的访问", description = "未授权的访问")
	public Result<?> unauth() {
		return Result.error(StatusCode.UNAUTH_ERROR.getCode(), StatusCode.UNAUTH_ERROR.getDesc());
	}

	@RequestMapping(value="/user/info",method = RequestMethod.GET)
	public Result<Object> info(HttpServletRequest req) {
		return Result.ok(user);
	}

	@RequestMapping(value = "/resource/{userId}/list", method = RequestMethod.GET)
	@Operation(summary = "获取指定用户的树状结构资源列表", description = "获取指定用户的树状结构资源列表")
	public Result<List<ResourceResponseDto>> getUserResources(@PathVariable("userId") Long userId) {
		init();
		return new Result<>(userResources);
	}

	@RequestMapping(value = "/user/detail/{userId}", method = RequestMethod.GET)
	@Operation(summary = "获取用户信息", description = "获取用户信息")
	public Result<UserInfo> getUserDetail(@Parameter(description = "用户ID", required = true) @PathVariable("userId") Long userId) {
		return new Result<>(user);
	}

}
#end