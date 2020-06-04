package ${voPackage};

import javax.validation.constraints.NotNull;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel("登录请求实体")
public class LoginVo {
    @ApiModelProperty("登录方式:1:用户名密码登录;2:手机验证码登录")
    @NotNull(message = "登录方式必填")
    private String loginType;
    @ApiModelProperty("用户名")
    private String userName;
    @ApiModelProperty("密码(32位MD5加密)")
    private String password;
    @ApiModelProperty("手机号")
    private String mobile;
    @ApiModelProperty("登录验证码")
    private String authCode;

    public String getLoginType() {
        return loginType;
    }

    public void setLoginType(String loginType) {
        this.loginType = loginType;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public String getAuthCode() {
        return authCode;
    }

    public void setAuthCode(String authCode) {
        this.authCode = authCode;
    }
}
