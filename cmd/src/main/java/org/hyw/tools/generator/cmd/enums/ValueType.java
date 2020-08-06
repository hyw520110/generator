package org.hyw.tools.generator.cmd.enums;

public enum ValueType {
	NOT_REQUIRE_SINGLE, NOT_REQUIRE_MULTIPLE, NOT_REQUIRE_SINGLE_OR_MULTIPLE, REQUIRE_SINGLE, REQUIRE_MULTIPLE,
	REQUIRE_SINGLE_OR_MULTIPLE;



	public boolean isRequire() {
		return this.name().startsWith("REQUIRE");
	}

	public boolean isSingle() {
		return this.name().endsWith("SINGLE") || this.name().contains("SINGLE");
	}

	public boolean isVariable() {
		return this.name().contains("_OR_");
	}

}
