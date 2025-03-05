
import java.awt.Color;
import java.io.FileOutputStream;
import java.util.List;

import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.hyw.tools.generator.Generator;
import org.hyw.tools.generator.conf.db.TabField;
import org.hyw.tools.generator.conf.db.Table;

import com.lowagie.text.BadElementException;
import com.lowagie.text.Cell;
import com.lowagie.text.Document;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.rtf.RtfWriter2;

/**
 * 数据库导出word
 **/
public class DbToWord {

	public static void main(String[] args) {
		Generator generator = Generator.getInstance();
		toWord(generator.getTables(), "/output/"+generator.getDataSource().getDbName()+".doc");
	}

	public static void toWord(List<Table> tables, String fileName) {
		// 创建word文档,并设置纸张的大小
		Document document = new Document(PageSize.A4);
		try {
			RtfWriter2.getInstance(document, new FileOutputStream(fileName));
			document.open();

			Paragraph ph = new Paragraph();
			Font f = new Font();
			Paragraph p = new Paragraph("数据库结构定义", new Font(Font.NORMAL, 24, Font.BOLDITALIC, new Color(0, 0, 0)));
			p.setAlignment(1);
			document.add(p);
			ph.setFont(f);

			document.add(new Paragraph("表汇总"));
			
			com.lowagie.text.Table tableMeta = newTable(3);
			Color headColor = headColor();
			tableMeta.addCell(newCell(headColor, "序号"));
			tableMeta.addCell(newCell(headColor, "表中文名"));
			tableMeta.addCell(newCell(headColor, "表英文名"));
			tableMeta.endHeaders();
			// 表头结束
			
			int i=1;
			for (Table tab : tables) {
				if(skip(tab.getComment())) {
					continue;
				}
				String tabComment = process(tab.getComment());
				tableMeta.addCell(String.valueOf(i++));
				tableMeta.addCell(tabComment);
				tableMeta.addCell(tab.getName());
			}
			document.add(tableMeta);

			for (Table tab : tables) {
//                String tabName = tab.getName();
				String tabComment = tab.getComment();
				if (skip(tab.getComment())) {
					continue;
				}
				tabComment = process(tabComment);
				List<TabField> fields = tab.getFields();
				document.add(new Paragraph(""));
				com.lowagie.text.Table table = newTable(5);
				Color color = headColor();
				table.addCell(newCell(color, "名称"));
				table.addCell(newCell(color, "类型"));
				table.addCell(newCell(color, "是否为空"));
				table.addCell(newCell(color, "是否主键"));
				table.addCell(newCell(color, "描述"));
				table.endHeaders();// 表头结束
				// 表格的主体
				for (TabField field : fields) {
					table.addCell(field.getName());
					table.addCell(field.getType());
					table.addCell(BooleanUtils.toString(field.isNullAble(), "是", "否"));
					table.addCell(BooleanUtils.toString(field.isPrimarykey(), "是", "否"));
					table.addCell(field.getComment());
				}
				// 表头
				Paragraph pheae = new Paragraph(tabComment+"("+tab.getName()+")");
				// 写入表说明
				document.add(pheae);
				// 生成表格
				document.add(table);
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			document.close();
		}
	}

	private static String process(String tabComment) {
		if(StringUtils.contains(tabComment, ":")) {
			tabComment=StringUtils.substringAfter(tabComment, ":");
		}
		return tabComment;
	}
	/**
	 * 过滤无注释的表和注释太长的表
	 * @param comment
	 * @return
	 */
	private static boolean skip(String comment) {
		return StringUtils.isBlank(comment)||comment.contains("\r\n");
	}

	private static Color headColor() {
		// 添加表头的元素，并设置表头背景的颜色
		Color chade = new Color(176, 196, 222);
		return chade;
	}

	private static Cell newCell(Color chade, String cellName) {
		Cell cell = new Cell(cellName);
		cell.setBackgroundColor(chade);
		return cell;
	}

	private static com.lowagie.text.Table newTable(int columns) throws BadElementException {
		com.lowagie.text.Table table = new com.lowagie.text.Table(columns);
		table.setBorderWidth(1);
		// table.setBorderColor(Color.BLACK);
		table.setPadding(0);
		table.setSpacing(0);
		return table;
	}
}
