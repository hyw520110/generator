package ${dtoPackage!};

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "LoginResponseDto", description = "登录成功用户信息")
public class LoginResponseDto {
	@Schema(name = "userToken", description = "用户身份令牌")
	private String userToken;
	@Schema(name = "userInfo", description = "用户信息")
	private UserInfo userInfo;
	@Schema(name = "userResources", description = "用户资源列表")
	private List<ResourceResponseDto> userResources;

	public LoginResponseDto(String token, UserInfo user) {
		this.userToken = token;
		this.userInfo = user;
	}

	public LoginResponseDto(String token, UserInfo user, List<ResourceResponseDto> userResources) {
		this.userToken = token;
		this.userInfo = user;
		this.userResources = userResources;
	}

	public String getUserToken() {
		return userToken;
	}

	public void setUserToken(String userToken) {
		this.userToken = userToken;
	}

	public UserInfo getUserInfo() {
		return userInfo;
	}

	public void setUserInfo(UserInfo userInfo) {
		this.userInfo = userInfo;
	}

	public List<ResourceResponseDto> getUserResources() {
		return userResources;
	}

	public void setUserResources(List<ResourceResponseDto> userResources) {
		this.userResources = userResources;
	}
}
