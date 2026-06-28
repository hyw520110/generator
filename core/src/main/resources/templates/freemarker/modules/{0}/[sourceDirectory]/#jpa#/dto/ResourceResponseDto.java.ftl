package ${dtoPackage!};

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

/*
 * FIXME: 示例数据实体类
 * 此文件为本地测试使用的示例资源数据
 * 生成的 jar 部署时，需要在 classpath (src/main/resources/data/) 中放置实际的 ResourceResponseDto.json 文件
 * 否则 ShiroRealm 读取权限会为空
 * 
 * 字段说明:
 * - resourceType: G(菜单组), M(菜单), O(按钮)
 * - resourceView: 对应前端组件名称，如 BasicLayout, RouteView, IframeView, 或具体页面组件
 */
@Schema(name = "ResourceResponseDto", description = "资源响应对象")
public class ResourceResponseDto {
	@Schema(name = "resourceId", description = "资源ID")
	private Long resourceId;
	@Schema(name = "resourceName", description = "资源名称")
	private String resourceName;
	@Schema(name = "resourceType", description = "资源类型(M 菜单  O 按钮)")
	private String resourceType;
	@Schema(name = "resourceUri", description = "资源路径")
	private String resourceUri;
	@Schema(name = "resourceRedirect", description = "资源重定向")
	private String resourceRedirect;
	@Schema(name = "resourceView", description = "资源视图")
	private String resourceView;
	@Schema(name = "resourceKey", description = "资源标识符")
	private String resourceKey;
	@Schema(name = "resourceIcon", description = "资源图标")
	private String resourceIcon;
	@Schema(name = "resourcePerms", description = "资源权限标识")
	private String resourcePerms;
	@Schema(name = "resourceLevel", description = "资源等级 0-99 0为顶级")
	private Integer resourceLevel;
	@Schema(name = "parentResourceId", description = "父类资源ID")
	private Long parentResourceId;
	@Schema(name = "sort", description = "排序")
	private Integer sort;
	@Schema(name = "childResources", description = "子级资源列表")
	private List<ResourceResponseDto> childResources;

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

	public String getResourceKey() {
		return resourceKey;
	}

	public void setResourceKey(String resourceKey) {
		this.resourceKey = resourceKey;
	}

	public String getResourceIcon() {
		return resourceIcon;
	}

	public void setResourceIcon(String resourceIcon) {
		this.resourceIcon = resourceIcon;
	}

	public String getResourcePerms() {
		return resourcePerms;
	}

	public void setResourcePerms(String resourcePerms) {
		this.resourcePerms = resourcePerms;
	}

	public Integer getResourceLevel() {
		return resourceLevel;
	}

	public void setResourceLevel(Integer resourceLevel) {
		this.resourceLevel = resourceLevel;
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

	public List<ResourceResponseDto> getChildResources() {
		return childResources;
	}

	public void setChildResources(List<ResourceResponseDto> childResources) {
		this.childResources = childResources;
	}

	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
	}
}
