package org.hyw.tools.generator;

import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.hyw.tools.generator.conf.db.TabField;
import org.hyw.tools.generator.conf.db.Table;
import org.hyw.tools.generator.enums.ExportFormat;
import org.apache.poi.xwpf.usermodel.*;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.*;

import java.awt.Color;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.util.List;

/**
 * 数据库导出文档工具
 * 支持 Word (.docx) 和 PDF 格式
 */
public class DbToDoc {

	public static void main(String[] args) {
		Generator generator = Generator.getInstance();
		List<Table> tables = generator.getTables();
		
		// 导出为 Word
		toDoc(tables, ExportFormat.DOCX, "/output/" + generator.getDataSource().getDbName() + ".docx");
		
		// 导出为 PDF
		toDoc(tables, ExportFormat.PDF, "/output/" + generator.getDataSource().getDbName() + ".pdf");
	}

	/**
	 * 导出数据库表结构为文档
	 * @param tables 表列表
	 * @param format 导出格式 (DOCX 或 PDF)
	 * @param fileName 输出文件名
	 */
	public static void toDoc(List<Table> tables, ExportFormat format, String fileName) {
		try {
			if (format == ExportFormat.DOCX) {
				exportToWord(tables, fileName);
				System.out.println("Word 文档生成成功: " + fileName);
			} else if (format == ExportFormat.PDF) {
				exportToPDF(tables, fileName);
				System.out.println("PDF 文档生成成功: " + fileName);
			}
		} catch (IOException e) {
			e.printStackTrace();
			System.err.println("文档生成失败: " + e.getMessage());
		}
	}

	/**
	 * 导出为 Word 文档
	 */
	private static void exportToWord(List<Table> tables, String fileName) throws IOException {
		XWPFDocument document = new XWPFDocument();
		try (FileOutputStream out = new FileOutputStream(fileName)) {
			// 添加标题
			XWPFParagraph titleParagraph = document.createParagraph();
			titleParagraph.setAlignment(ParagraphAlignment.CENTER);
			XWPFRun titleRun = titleParagraph.createRun();
			titleRun.setText("数据库结构定义");
			titleRun.setBold(true);
			titleRun.setFontSize(24);
			titleRun.setFontFamily("宋体");

			// 添加表汇总说明
			XWPFParagraph summaryParagraph = document.createParagraph();
			XWPFRun summaryRun = summaryParagraph.createRun();
			summaryRun.setText("表汇总");
			summaryRun.setBold(true);
			summaryRun.setFontSize(14);
			summaryRun.setFontFamily("宋体");

			// 创建表汇总表格
			XWPFTable summaryTable = document.createTable();
			CTTbl summaryTableCT = summaryTable.getCTTbl();
			CTTblPr summaryTablePr = summaryTableCT.getTblPr();
			summaryTablePr.setTblW(CTTblWidth.Factory.newInstance());
			summaryTablePr.getTblW().setType(STTblWidth.DXA);
			summaryTablePr.getTblW().setW(new BigInteger("8500"));

			// 创建表头行
			XWPFTableRow summaryHeaderRow = summaryTable.getRow(0);
			setTableCell(summaryHeaderRow.getCell(0), "序号", true);
			summaryHeaderRow.addNewTableCell();
			setTableCell(summaryHeaderRow.getCell(1), "表中文名", true);
			summaryHeaderRow.addNewTableCell();
			setTableCell(summaryHeaderRow.getCell(2), "表英文名", true);

			// 填充表汇总数据
			int index = 1;
			for (Table tab : tables) {
				if (skip(tab.getComment())) {
					continue;
				}
				String tabComment = process(tab.getComment());
				XWPFTableRow row = summaryTable.createRow();
				setTableCell(row.getCell(0), String.valueOf(index++), false);
				setTableCell(row.getCell(1), tabComment, false);
				setTableCell(row.getCell(2), tab.getName(), false);
			}

			// 为每个表创建详细字段信息
			for (Table tab : tables) {
				String tabComment = tab.getComment();
				if (skip(tabComment)) {
					continue;
				}
				tabComment = process(tabComment);
				List<TabField> fields = tab.getFields();

				// 添加空行
				document.createParagraph();

				// 添加表标题
				XWPFParagraph tableTitleParagraph = document.createParagraph();
				XWPFRun tableTitleRun = tableTitleParagraph.createRun();
				tableTitleRun.setText(tabComment + "(" + tab.getName() + ")");
				tableTitleRun.setBold(true);
				tableTitleRun.setFontSize(14);
				tableTitleRun.setFontFamily("宋体");

				// 创建字段表格
				XWPFTable fieldTable = document.createTable();
				CTTbl fieldTableCT = fieldTable.getCTTbl();
				CTTblPr fieldTablePr = fieldTableCT.getTblPr();
				fieldTablePr.setTblW(CTTblWidth.Factory.newInstance());
				fieldTablePr.getTblW().setType(STTblWidth.DXA);
				fieldTablePr.getTblW().setW(new BigInteger("8500"));

				// 创建表头行
				XWPFTableRow fieldHeaderRow = fieldTable.getRow(0);
				setTableCell(fieldHeaderRow.getCell(0), "名称", true);
				fieldHeaderRow.addNewTableCell();
				setTableCell(fieldHeaderRow.getCell(1), "类型", true);
				fieldHeaderRow.addNewTableCell();
				setTableCell(fieldHeaderRow.getCell(2), "是否为空", true);
				fieldHeaderRow.addNewTableCell();
				setTableCell(fieldHeaderRow.getCell(3), "是否主键", true);
				fieldHeaderRow.addNewTableCell();
				setTableCell(fieldHeaderRow.getCell(4), "描述", true);

				// 填充字段数据
				for (TabField field : fields) {
					XWPFTableRow row = fieldTable.createRow();
					setTableCell(row.getCell(0), field.getName(), false);
					setTableCell(row.getCell(1), field.getType(), false);
					setTableCell(row.getCell(2), BooleanUtils.toString(field.isNullAble(), "是", "否"), false);
					setTableCell(row.getCell(3), BooleanUtils.toString(field.isPrimarykey(), "是", "否"), false);
					setTableCell(row.getCell(4), field.getComment(), false);
				}
			}

			// 保存文档
			document.write(out);
		} finally {
			document.close();
		}
	}

	/**
	 * 导出为 PDF 文档
	 */
	private static void exportToPDF(List<Table> tables, String fileName) throws IOException {
		try (PDDocument document = new PDDocument()) {
			PDPage page = new PDPage(PDRectangle.A4);
			document.addPage(page);

			PDPageContentStream contentStream = new PDPageContentStream(document, page);
			try {
				float margin = 50;
				float yPosition = page.getMediaBox().getHeight() - margin;
				float lineHeight = 15;
				float tableLineHeight = 12;

				// 标题
				contentStream.setFont(PDType1Font.HELVETICA_BOLD, 18);
				contentStream.beginText();
				contentStream.newLineAtOffset(margin, yPosition);
				contentStream.showText("数据库结构定义");
				contentStream.endText();
				yPosition -= 30;

				// 表汇总标题
				contentStream.setFont(PDType1Font.HELVETICA_BOLD, 14);
				contentStream.beginText();
				contentStream.newLineAtOffset(margin, yPosition);
				contentStream.showText("表汇总");
				contentStream.endText();
				yPosition -= 20;

				// 绘制表汇总表格
				contentStream.setFont(PDType1Font.HELVETICA, 10);
				float[] columnWidths = {60, 150, 200};
				float tableWidth = columnWidths[0] + columnWidths[1] + columnWidths[2];
				float xPosition = margin;

				// 表头
				contentStream.setLineWidth(0.5f);
				contentStream.setStrokingColor(Color.GRAY);
				drawTableRow(contentStream, xPosition, yPosition, columnWidths, new String[]{"序号", "表中文名", "表英文名"}, true);
				yPosition -= tableLineHeight;

				// 表数据
				int index = 1;
				for (Table tab : tables) {
					if (skip(tab.getComment())) {
						continue;
					}
					if (yPosition < 50) {
						// 新页面
						contentStream.close();
						page = new PDPage(PDRectangle.A4);
						document.addPage(page);
						contentStream = new PDPageContentStream(document, page);
						yPosition = page.getMediaBox().getHeight() - margin;
					}
					drawTableRow(contentStream, xPosition, yPosition, columnWidths, 
						new String[]{String.valueOf(index++), process(tab.getComment()), tab.getName()}, false);
					yPosition -= tableLineHeight;
				}

				yPosition -= 20;

				// 每个表的详细字段
				for (Table tab : tables) {
					String tabComment = tab.getComment();
					if (skip(tabComment)) {
						continue;
					}
					tabComment = process(tabComment);
					List<TabField> fields = tab.getFields();

					if (yPosition < 100) {
						contentStream.close();
						page = new PDPage(PDRectangle.A4);
						document.addPage(page);
						contentStream = new PDPageContentStream(document, page);
						yPosition = page.getMediaBox().getHeight() - margin;
					}

					// 表标题
					contentStream.setFont(PDType1Font.HELVETICA_BOLD, 12);
					contentStream.beginText();
					contentStream.newLineAtOffset(margin, yPosition);
					contentStream.showText(tabComment + "(" + tab.getName() + ")");
					contentStream.endText();
					yPosition -= 20;

					// 字段表格
					float[] fieldColumnWidths = {100, 120, 80, 80, 200};
					float fieldTableWidth = fieldColumnWidths[0] + fieldColumnWidths[1] + fieldColumnWidths[2] + fieldColumnWidths[3] + fieldColumnWidths[4];

					// 表头
					contentStream.setFont(PDType1Font.HELVETICA_BOLD, 10);
					drawTableRow(contentStream, xPosition, yPosition, fieldColumnWidths, 
						new String[]{"名称", "类型", "是否为空", "是否主键", "描述"}, true);
					yPosition -= tableLineHeight;

					// 字段数据
					contentStream.setFont(PDType1Font.HELVETICA, 9);
					for (TabField field : fields) {
						if (yPosition < 50) {
							contentStream.close();
							page = new PDPage(PDRectangle.A4);
							document.addPage(page);
							contentStream = new PDPageContentStream(document, page);
							yPosition = page.getMediaBox().getHeight() - margin;
						}
						drawTableRow(contentStream, xPosition, yPosition, fieldColumnWidths, 
							new String[]{
								field.getName(), 
								field.getType(), 
								BooleanUtils.toString(field.isNullAble(), "是", "否"), 
								BooleanUtils.toString(field.isPrimarykey(), "是", "否"), 
								field.getComment()
							}, false);
						yPosition -= tableLineHeight;
					}

					yPosition -= 20;
				}
			} finally {
				contentStream.close();
			}

			document.save(new File(fileName));
		}
	}

	/**
	 * 绘制 PDF 表格行
	 */
	private static void drawTableRow(PDPageContentStream contentStream, float x, float y, 
			float[] columnWidths, String[] texts, boolean isHeader) throws IOException {
		contentStream.setLineWidth(0.5f);
		
		// 绘制单元格边框
		float currentX = x;
		for (int i = 0; i < columnWidths.length; i++) {
			contentStream.moveTo(currentX, y);
			contentStream.lineTo(currentX + columnWidths[i], y);
			currentX += columnWidths[i];
		}
		contentStream.moveTo(x, y - 12);
		contentStream.lineTo(currentX, y - 12);

		// 绘制垂直线
		currentX = x;
		for (int i = 0; i < columnWidths.length; i++) {
			contentStream.moveTo(currentX, y);
			contentStream.lineTo(currentX, y - 12);
			currentX += columnWidths[i];
		}

		// 填充表头背景色
		if (isHeader) {
			contentStream.setNonStrokingColor(new Color(176, 196, 222));
			contentStream.addRect(x, y - 12, columnWidths[0] + columnWidths[1] + columnWidths[2], 12);
			contentStream.fill();
		}

		contentStream.stroke();

		// 绘制文本
		currentX = x;
		for (int i = 0; i < texts.length; i++) {
			String text = texts[i];
			if (text != null && text.length() > 15) {
				text = text.substring(0, 15) + "...";
			}
			contentStream.beginText();
			contentStream.newLineAtOffset(currentX + 5, y - 9);
			contentStream.showText(text);
			contentStream.endText();
			currentX += columnWidths[i];
		}
	}

	/**
	 * 处理表注释
	 */
	private static String process(String tabComment) {
		if (StringUtils.contains(tabComment, ":")) {
			tabComment = StringUtils.substringAfter(tabComment, ":");
		}
		return tabComment;
	}

	/**
	 * 过滤无注释的表和注释太长的表
	 */
	private static boolean skip(String comment) {
		return StringUtils.isBlank(comment) || comment.contains("\r\n");
	}

	/**
	 * 设置 Word 表格单元格内容和样式
	 */
	private static void setTableCell(XWPFTableCell cell, String text, boolean isHeader) {
		XWPFParagraph paragraph = cell.getParagraphs().get(0);
		paragraph.setAlignment(ParagraphAlignment.CENTER);
		XWPFRun run = paragraph.createRun();
		run.setText(text);
		run.setFontFamily("宋体");
		run.setFontSize(10);

		if (isHeader) {
			run.setBold(true);
			// 设置表头背景色
			cell.setColor("B0C4DE"); // 浅蓝色 RGB(176, 196, 222)
		}
	}
}
