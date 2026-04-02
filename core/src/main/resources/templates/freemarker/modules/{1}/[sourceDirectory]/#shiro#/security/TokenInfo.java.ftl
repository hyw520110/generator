package ${securityPackage!};

import java.io.Serializable;
import java.util.Date;
import ${dtoPackage!}.Token;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 令牌信息对象
 */
@Schema(name = "TokenInfo", description = "令牌信息")
public class TokenInfo implements Token, Serializable {

	private static final long serialVersionUID = 1L;
	
	@Schema(description = "用户ID")
	private String userId;
	
	@Schema(description = "令牌类型")
	private String type;
	
	@Schema(description = "访问令牌")
	private String token;
	
	@Schema(description = "用户名")
	private String username;
	
	@Schema(description = "过期时间")
	private Date expireTime;

	public TokenInfo() {
	}

	public TokenInfo(String userId, String token) {
		this.userId = userId;
		this.token = token;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public void setToken(String token) {
		this.token = token;
	}

	@Override
	public String getToken() {
		return token;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public Date getExpireTime() {
		return expireTime;
	}

	public void setExpireTime(Date expireTime) {
		this.expireTime = expireTime;
	}
}
