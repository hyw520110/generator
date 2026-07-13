package org.hyw.tools.generator.compat;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ResolvedPlatform {

	private final String id;
	private final String name;
	private final int javaVersion;
	private final int release;
	private final String templateFamily;
	private final String namespace;
	private final Map<String, Object> variables;

	public ResolvedPlatform(String id, String name, int javaVersion, int release, String templateFamily,
			String namespace, Map<String, Object> variables) {
		this.id = id;
		this.name = name;
		this.javaVersion = javaVersion;
		this.release = release;
		this.templateFamily = templateFamily;
		this.namespace = namespace;
		this.variables = variables == null ? new HashMap<>() : new HashMap<>(variables);
	}

	public String getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public int getJavaVersion() {
		return javaVersion;
	}

	public int getRelease() {
		return release;
	}

	public String getTemplateFamily() {
		return templateFamily;
	}

	public String getNamespace() {
		return namespace;
	}

	public Map<String, Object> getVariables() {
		return Collections.unmodifiableMap(variables);
	}
}
