package ${voPackage};

import java.util.List;

import ${voPackage}.Role;

public class UserAuthCache {
	private List<Role> userRoles;
	private List<Resource> userResources;

	public UserAuthCache(List<Resource> resource) {
		this.userResources = resource;
	}

	public List<Role> getUserRoles() {
		return userRoles;
	}

	public void setUserRoles(List<Role> userRoles) {
		this.userRoles = userRoles;
	}

	public List<Resource> getUserResources() {
		return userResources;
	}

	public void setUserResources(List<Resource> userResources) {
		this.userResources = userResources;
	}
}
