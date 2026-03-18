package org.hyw.tools.generator.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 文档导出格式枚举
 * <p>
 * 定义数据库表结构导出时支持的文档格式
 * </p>
 *
 * @author heyiwu
 * @version 2.0
 */
@Getter
@AllArgsConstructor
public enum ExportFormat {

	/**
	 * Word 文档格式 (.docx)
	 */
	DOCX(".docx", "Word 文档"),

	/**
	 * PDF 文档格式 (.pdf)
	 */
	PDF(".pdf", "PDF 文档");

	/**
	 * 文件扩展名
	 */
	private final String extension;

	/**
	 * 格式描述
	 */
	private final String description;
}