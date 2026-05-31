package org.hyw.tools.generator.enums;

/**
 * Supported Java language/runtime targets for generated projects.
 */
public enum JavaVersion {
	JAVA_8(8),
	JAVA_11(11),
	JAVA_17(17),
	JAVA_21(21);

	private final int version;

	JavaVersion(int version) {
		this.version = version;
	}

	public int getVersion() {
		return version;
	}

	public static JavaVersion from(String value) {
		if (value == null || value.trim().isEmpty()) {
			return null;
		}
		String normalized = value.trim().toUpperCase();
		if (normalized.startsWith("JAVA_")) {
			return JavaVersion.valueOf(normalized);
		}
		if (normalized.startsWith("JAVA")) {
			normalized = normalized.substring(4);
		}
		int number = Integer.parseInt(normalized);
		return from(number);
	}

	public static JavaVersion from(int version) {
		for (JavaVersion javaVersion : values()) {
			if (javaVersion.version == version) {
				return javaVersion;
			}
		}
		throw new IllegalArgumentException("不支持的 Java 版本: " + version);
	}
}
