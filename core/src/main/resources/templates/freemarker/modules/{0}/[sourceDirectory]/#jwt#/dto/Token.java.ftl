package ${dtoPackage!};

import java.util.Date;

/**
 * Token 接口定义
 */
public interface Token {
	String getToken();
	String getUsername();
	Date getExpireTime();
	String getUserId();
	String getType();
	void setToken(String token);
	void setUsername(String username);
	void setExpireTime(Date expireTime);
	void setUserId(String userId);
	void setType(String type);
}
