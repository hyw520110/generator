package org.hyw.tools.generator.cmd.enums;

public enum ValueType {
	NOT_REQUIRE_SINGLE, NOT_REQUIRE_MULTIPLE, REQUIRE_SINGLE, REQUIRE_MULTIPLE;


	public boolean isRequire() {
		return this.name().startsWith("REQUIRE");
	}

	public boolean isSingle() {
		return this.name().endsWith("SINGLE");
	}
}
