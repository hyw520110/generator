package org.hyw.tools.generator.compat;

import java.util.HashMap;
import java.util.Map;

import org.hyw.tools.generator.enums.Component;

public class CompatibilityProfile {

	private int java;
	private String id;
	private String name;
	private String parent;
	private String templateFamily;
	private String namespace;
	private Integer release;
	private boolean defaultProfile;
	private Map<Component, Map<String, Object>> versions = new HashMap<>();
	private Map<String, String> allowOverride = new HashMap<>();
	private Map<Component, ComponentConstraint> componentConstraints = new HashMap<>();

	public CompatibilityProfile copy() {
		CompatibilityProfile copy = new CompatibilityProfile();
		copy.java = java;
		copy.id = id;
		copy.name = name;
		copy.parent = parent;
		copy.templateFamily = templateFamily;
		copy.namespace = namespace;
		copy.release = release;
		copy.defaultProfile = defaultProfile;
		copy.versions = deepCopy(versions);
		copy.allowOverride = allowOverride == null ? new HashMap<>() : new HashMap<>(allowOverride);
		copy.componentConstraints = componentConstraints == null
				? new HashMap<>() : new HashMap<>(componentConstraints);
		return copy;
	}

	public void mergeFrom(CompatibilityProfile child) {
		if (child == null) {
			return;
		}
		System.out.println("[CompatibilityProfile] mergeFrom: child.id=" + child.getId() + " into parent.id=" + this.id);
		if (child.templateFamily != null) {
			System.out.println("[CompatibilityProfile] mergeFrom: child.templateFamily='" + child.templateFamily + "' → override parent.templateFamily='" + this.templateFamily + "'");
			this.templateFamily = child.templateFamily;
		}
		if (child.namespace != null) {
			System.out.println("[CompatibilityProfile] mergeFrom: child.namespace='" + child.namespace + "' → override parent.namespace='" + this.namespace + "'");
			this.namespace = child.namespace;
		}
		if (child.release != null) {
			System.out.println("[CompatibilityProfile] mergeFrom: child.release=" + child.release + " → override parent.release=" + this.release);
			this.release = child.release;
		}
		if (child.versions != null) {
			for (Map.Entry<Component, Map<String, Object>> entry : child.versions.entrySet()) {
				Map<String, Object> target = this.versions.get(entry.getKey());
				if (target == null) {
					target = new HashMap<>();
					this.versions.put(entry.getKey(), target);
				}
				if (entry.getValue() != null) {
					System.out.println("[CompatibilityProfile] mergeFrom: child.versions[" + entry.getKey() + "]=" + entry.getValue());
					target.putAll(entry.getValue());
				}
			}
		}
		if (child.allowOverride != null) {
			System.out.println("[CompatibilityProfile] mergeFrom: child.allowOverride added: " + child.allowOverride.size() + " entries");
			this.allowOverride.putAll(child.allowOverride);
		}
		if (child.componentConstraints != null && !child.componentConstraints.isEmpty()) {
			if (this.componentConstraints == null) {
				this.componentConstraints = new HashMap<>();
			}
			System.out.println("[CompatibilityProfile] mergeFrom: child.componentConstraints added: " + child.componentConstraints.size() + " entries");
			this.componentConstraints.putAll(child.componentConstraints);
		}
	}
	private static Map<Component, Map<String, Object>> deepCopy(Map<Component, Map<String, Object>> source) {
		Map<Component, Map<String, Object>> copy = new HashMap<>();
		if (source == null) {
			return copy;
		}
		for (Map.Entry<Component, Map<String, Object>> entry : source.entrySet()) {
			copy.put(entry.getKey(), entry.getValue() == null ? new HashMap<>() : new HashMap<>(entry.getValue()));
		}
		return copy;
	}

	public int getJava() {
		return java;
	}

	public void setJava(int java) {
		this.java = java;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getExtends() {
		return parent;
	}

	public void setExtends(String parent) {
		this.parent = parent;
	}

	public String getTemplateFamily() {
		return templateFamily;
	}

	public void setTemplateFamily(String templateFamily) {
		this.templateFamily = templateFamily;
	}

	public String getNamespace() {
		return namespace;
	}

	public void setNamespace(String namespace) {
		this.namespace = namespace;
	}

	public Integer getRelease() {
		return release;
	}

	public void setRelease(Integer release) {
		this.release = release;
	}

	public Map<Component, Map<String, Object>> getVersions() {
		return versions;
	}

	public void setVersions(Map<Component, Map<String, Object>> versions) {
		this.versions = versions;
	}

	public Map<String, String> getAllowOverride() {
		return allowOverride;
	}

	public void setAllowOverride(Map<String, String> allowOverride) {
		this.allowOverride = allowOverride;
	}

	public boolean isDefaultProfile() {
		return defaultProfile;
	}

	public void setDefaultProfile(boolean defaultProfile) {
		this.defaultProfile = defaultProfile;
	}

	public Map<Component, ComponentConstraint> getComponentConstraints() {
		return componentConstraints;
	}

	public void setComponentConstraints(Map<Component, ComponentConstraint> componentConstraints) {
		this.componentConstraints = componentConstraints == null ? new HashMap<>() : componentConstraints;
	}
}
