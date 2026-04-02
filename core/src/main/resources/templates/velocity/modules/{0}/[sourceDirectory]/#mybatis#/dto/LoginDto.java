package ${dtoPackage};

import jakarta.validation.constraints.NotNull;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "LoginDto", description = "登录请求实体")
public class LoginDto {
    @Schema(name = "loginType", description = "登录方式:1:用户名密码登录;2:手机验证码登录")
    @NotNull(message = "登录方式必填")
    private String loginType;
    @Schema(name = "userName", description = "用户名")
    private String userName;
    @Schema(name = "password", description = "密码(32位MD5加密)")
    private String password;
    @Schema(name = "mobile", description = "手机号")
    private String mobile;
    @Schema(name = "authCode", description = "登录验证码")
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
