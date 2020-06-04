package ${voPackage};

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(value = "登录成功用户信息")
public class LoginResponseVo {
	@ApiModelProperty(value = "用户身份令牌")
	private String userToken;
	@ApiModelProperty(value = "用户信息")
	private UserInfo userInfo;

	public LoginResponseVo(String token, UserInfo user) {
		this.userToken = token;
		this.userInfo = user;
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
}
