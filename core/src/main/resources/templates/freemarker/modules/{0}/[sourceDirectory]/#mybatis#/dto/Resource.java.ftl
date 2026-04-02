package ${dtoPackage!};

import java.util.Date;

import ${dtoPackage!}.BaseModel;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 资源信息
 */
@Schema(name = "Resource", description = "资源信息")
public class Resource extends BaseModel<Resource> {

	private Long resourceId;

	@Schema(name = "resourceName", description = "资源名称")
	private String resourceName;

	@Schema(name = "resourceType", description = "资源类型(G 菜单组 M 菜单  O 按钮)")
	private String resourceType;

	@Schema(name = "resourceKey", description = "资源标识符")
	private String resourceKey;

	@Schema(name = "resourceUri", description = "资源路径")
	private String resourceUri;

	@Schema(name = "resourceRedirect", description = "资源重定向")
	private String resourceRedirect;

	@Schema(name = "resourceView", description = "资源视图")
	private String resourceView;

	@Schema(name = "resourceIcon", description = "资源图标")
	private String resourceIcon;

	@Schema(name = "resourcePerms", description = "资源权限标识")
	private String resourcePerms;

	@Schema(name = "resourceLevel", description = "资源等级")
	private Integer resourceLevel;

	@Schema(name = "parentResourceId", description = "父级资源ID")
	private Long parentResourceId;

	@Schema(name = "sort", description = "排序")
	private Integer sort;

	@Schema(name = "ct", description = "创建时间", hidden = true)
	private Date ct;

	@Schema(name = "ut", description = "更新时间", hidden = true)
	private Date ut;

	@Schema(name = "isDel", description = "删除标识位 0 未删除  1 已删除", hidden = true)
	private Integer isDel;

	@Schema(name = "version", description = "乐观锁标识", hidden = true)
	private Long version;

	public Long getResourceId() {
		return resourceId;
	}

	public void setResourceId(Long resourceId) {
		this.resourceId = resourceId;
	}

	public String getResourceName() {
		return resourceName;
	}

	public void setResourceName(String resourceName) {
		this.resourceName = resourceName;
	}

	public String getResourceType() {
		return resourceType;
	}

	public void setResourceType(String resourceType) {
		this.resourceType = resourceType;
	}

	public String getResourceKey() {
		return resourceKey;
	}

	public void setResourceKey(String resourceKey) {
		this.resourceKey = resourceKey;
	}

	public String getResourceUri() {
		return resourceUri;
	}

	public void setResourceUri(String resourceUri) {
		this.resourceUri = resourceUri;
	}

	public String getResourceRedirect() {
		return resourceRedirect;
	}

	public void setResourceRedirect(String resourceRedirect) {
		this.resourceRedirect = resourceRedirect;
	}

	public String getResourceView() {
		return resourceView;
	}

	public void setResourceView(String resourceView) {
		this.resourceView = resourceView;
	}

	public String getResourceIcon() {
		return resourceIcon;
	}

	public void setResourceIcon(String resourceIcon) {
		this.resourceIcon = resourceIcon;
	}

	public Integer getResourceLevel() {
		return resourceLevel;
	}

	public void setResourceLevel(Integer resourceLevel) {
		this.resourceLevel = resourceLevel;
	}

	public String getResourcePerms() {
		return resourcePerms;
	}

	public void setResourcePerms(String resourcePerms) {
		this.resourcePerms = resourcePerms;
	}

	public Long getParentResourceId() {
		return parentResourceId;
	}

	public void setParentResourceId(Long parentResourceId) {
		this.parentResourceId = parentResourceId;
	}

	public Integer getSort() {
		return sort;
	}

	public void setSort(Integer sort) {
		this.sort = sort;
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
		return "Resource{" + "resourceId=" + resourceId + ", resourceName=" + resourceName + ", resourceType="
				+ resourceType + ", resourceKey=" + resourceKey + ", resourceUri=" + resourceUri + ", resourceRedirect="
				+ resourceRedirect + ", resourceView=" + resourceView + ", resourceIcon=" + resourceIcon
				+ ", resourcePerms=" + resourcePerms + ", resourceLevel=" + resourceLevel + ", parentResourceId="
				+ parentResourceId + ", sort=" + sort + ", ct=" + ct + ", ut=" + ut + ", isDel=" + isDel + ", version="
				+ version + "}";
	}
}
