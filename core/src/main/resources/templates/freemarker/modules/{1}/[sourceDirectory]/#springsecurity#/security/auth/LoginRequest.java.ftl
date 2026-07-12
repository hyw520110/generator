package ${packagePath};

import java.io.Serializable;

/**
 * 登录请求体
 */
public class LoginRequest implements Serializable {

    private String username;
    private String password;
    private String phone;
    private String smsCode;

    // Getters and Setters
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getSmsCode() { return smsCode; }
    public void setSmsCode(String smsCode) { this.smsCode = smsCode; }
}
