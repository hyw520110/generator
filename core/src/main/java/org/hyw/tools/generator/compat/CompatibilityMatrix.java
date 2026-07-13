package org.hyw.tools.generator.compat;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.hyw.tools.generator.enums.JavaVersion;
import org.yaml.snakeyaml.Yaml;

public class CompatibilityMatrix {

	private static final String DEFAULT_RESOURCE = "/compatibility.yml";

	private int defaultJava = 17;
	private List<CompatibilityProfile> profiles = new ArrayList<>();

	public static CompatibilityMatrix loadDefault() {
		try (InputStream inputStream = CompatibilityMatrix.class.getResourceAsStream(DEFAULT_RESOURCE)) {
			if (inputStream == null) {
				return new CompatibilityMatrix();
			}
			CompatibilityMatrix matrix = new Yaml().loadAs(inputStream, CompatibilityMatrix.class);
			return matrix == null ? new CompatibilityMatrix() : matrix;
		} catch (Exception e) {
			throw new IllegalStateException("加载兼容性矩阵失败: " + DEFAULT_RESOURCE, e);
		}
	}

	public CompatibilityProfile resolveByJava(String javaVersion) {
		JavaVersion version = JavaVersion.from(javaVersion == null || javaVersion.trim().isEmpty()
				? String.valueOf(defaultJava)
				: javaVersion);
		return resolveByJava(version.getVersion());
	}

	public CompatibilityProfile resolveByJava(int javaVersion) {
		CompatibilityProfile firstMatch = null;
		System.out.println("[CompatibilityMatrix] resolveByJava: javaVersion=" + javaVersion + ", searching profiles...");
		for (CompatibilityProfile profile : profiles) {
			if (profile.getJava() != javaVersion) {
				continue;
			}
			System.out.println("[CompatibilityMatrix] resolveByJava: matched profile.id=" + profile.getId()
				+ ", defaultProfile=" + profile.isDefaultProfile());
			if (profile.isDefaultProfile()) {
				CompatibilityProfile resolved = resolve(profile);
				System.out.println("[CompatibilityMatrix] resolveByJava: default match → resolved.id=" + resolved.getId()
					+ ", templateFamily=" + resolved.getTemplateFamily());
				return resolved;
			}
			if (firstMatch == null) {
				firstMatch = profile;
			}
		}
		if (firstMatch != null) {
			CompatibilityProfile resolved = resolve(firstMatch);
			System.out.println("[CompatibilityMatrix] resolveByJava: firstMatch fallback → resolved.id=" + resolved.getId()
				+ ", templateFamily=" + resolved.getTemplateFamily());
			return resolved;
		}
		throw new IllegalArgumentException("未配置 Java " + javaVersion + " 的兼容性矩阵");
	}

	public CompatibilityProfile resolveById(String id) {
		for (CompatibilityProfile profile : profiles) {
			if (id != null && id.equals(profile.getId())) {
				System.out.println("[CompatibilityMatrix] resolveById: id=" + id + " → found profile, calling resolve()");
				CompatibilityProfile resolved = resolve(profile);
				System.out.println("[CompatibilityMatrix] resolveById: id=" + id + " → resolved.id=" + resolved.getId()
					+ ", templateFamily=" + resolved.getTemplateFamily() + ", java=" + resolved.getJava()
					+ ", namespace=" + resolved.getNamespace() + ", defaultProfile=" + resolved.isDefaultProfile());
				return resolved;
			}
		}
		throw new IllegalArgumentException("未配置兼容性档位: " + id);
	}

	private CompatibilityProfile resolve(CompatibilityProfile profile) {
		String parentId = profile.getExtends();
		System.out.println("[CompatibilityMatrix] resolve: profile.id=" + profile.getId() + ", parentId=" + parentId);
		if (parentId == null || parentId.trim().isEmpty()) {
			System.out.println("[CompatibilityMatrix] resolve: no parent, returning copy of " + profile.getId());
			return profile.copy();
		}
		System.out.println("[CompatibilityMatrix] resolve: merging into parent " + parentId);
		CompatibilityProfile parent = resolveById(parentId);
		parent.mergeFrom(profile);
		System.out.println("[CompatibilityMatrix] resolve: after merge → id=" + parent.getId()
			+ ", templateFamily=" + parent.getTemplateFamily() + ", namespace=" + parent.getNamespace());
		return parent;
	}

	public int getDefaultJava() {
		return defaultJava;
	}

	public void setDefaultJava(int defaultJava) {
		this.defaultJava = defaultJava;
	}

	public List<CompatibilityProfile> getProfiles() {
		return profiles;
	}

	public void setProfiles(List<CompatibilityProfile> profiles) {
		this.profiles = profiles;
	}
}
