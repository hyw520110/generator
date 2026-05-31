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
		for (CompatibilityProfile profile : profiles) {
			if (profile.getJava() != javaVersion) {
				continue;
			}
			if (profile.isDefaultProfile()) {
				return resolve(profile);
			}
			if (firstMatch == null) {
				firstMatch = profile;
			}
		}
		if (firstMatch != null) {
			return resolve(firstMatch);
		}
		throw new IllegalArgumentException("未配置 Java " + javaVersion + " 的兼容性矩阵");
	}

	public CompatibilityProfile resolveById(String id) {
		for (CompatibilityProfile profile : profiles) {
			if (id != null && id.equals(profile.getId())) {
				return resolve(profile);
			}
		}
		throw new IllegalArgumentException("未配置兼容性档位: " + id);
	}

	private CompatibilityProfile resolve(CompatibilityProfile profile) {
		String parentId = profile.getExtends();
		if (parentId == null || parentId.trim().isEmpty()) {
			return profile.copy();
		}
		CompatibilityProfile parent = resolveById(parentId);
		parent.mergeFrom(profile);
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
