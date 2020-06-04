package ${voPackage};

import org.apache.shiro.authc.AuthenticationToken;

public class TokenVo implements AuthenticationToken {

	private static final long serialVersionUID = 3078816579864008822L;
	private String userId;
	private String type;
	private String token;

	public TokenVo() {
	}

	public TokenVo(String userId, String token) {
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

	public String getToken() {
		return token;
	}

	// shiro
	@Override
	public Object getPrincipal() {
		return getUserId();
	}

	@Override
	public Object getCredentials() {
		return getToken();
	}
}
