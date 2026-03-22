package ${voPackage};

import java.io.Serializable;
import java.util.Date;
import java.util.Objects;

import io.swagger.annotations.ApiModelProperty;

public class Role {

	private static final long serialVersionUID = 1L;

	private Long roleId;

	@ApiModelProperty(value = "角色名称")
	private String roleName;

	@ApiModelProperty(value = "角色权限标识")
	private String roleKey;

	@ApiModelProperty(value = "角色描述")
	private String roleDesc;

	@ApiModelProperty(value = "创建时间", hidden = true)
	private Date ct;

	@ApiModelProperty(value = "更新时间", hidden = true)
	private Date ut;

	@ApiModelProperty(value = "删除标识位 0 未删除  1 已删除", hidden = true)
	private Integer isDel;

	@ApiModelProperty(value = "乐观锁标识", hidden = true)
	private Long version;

	public Long getRoleId() {
		return roleId;
	}

	public void setRoleId(Long roleId) {
		this.roleId = roleId;
	}

	public String getRoleName() {
		return roleName;
	}

	public void setRoleName(String roleName) {
		this.roleName = roleName;
	}

	public String getRoleKey() {
		return roleKey;
	}

	public void setRoleKey(String roleKey) {
		this.roleKey = roleKey;
	}

	public String getRoleDesc() {
		return roleDesc;
	}

	public void setRoleDesc(String roleDesc) {
		this.roleDesc = roleDesc;
	}

	public Date getCt() {
		return ct;
	}

	public void setCt(Date ct) {
		this.ct = ct;
	}

	public Date getUt() {
		return ut;
	}

	public void setUt(Date ut) {
		this.ut = ut;
	}

	public Integer getIsDel() {
		return isDel;
	}

	public void setIsDel(Integer isDel) {
		this.isDel = isDel;
	}

	public Long getVersion() {
		return version;
	}

	public void setVersion(Long version) {
		this.version = version;
	}

	@Override
	public String toString() {
		return "Role{" + "roleId=" + roleId + ", roleName=" + roleName + ", roleKey=" + roleKey + ", roleDesc="
				+ roleDesc + ", ct=" + ct + ", ut=" + ut + ", isDel=" + isDel + ", version=" + version + "}";
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		Role role = (Role) o;
		return roleId.equals(role.roleId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(roleId);
	}
}
