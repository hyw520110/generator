package org.hyw.tools.generator.constants;

import org.hyw.tools.generator.enums.EngineType;

/**
 * 代码生成器常量类
 */
public final class Consts {


	// 外部配置文件
	public static final String PATH_SEPARATOR = "/";
	public static final String CONFIG_DIR_NAME = ".generator";
	public static final String CONFIG_FILE_NAME = "generator.yaml";
	public static final String DEFAULT_CONFIG_FILE = PATH_SEPARATOR+CONFIG_FILE_NAME;
	public static final String CONF_DIR_PATH = "/conf/";

	public static final String PATH_SEPARATOR_REGEX = "/";
	public static final char WINDOWS_SEPARATOR = '\\';

	// 编码常量
	public static final String DEFAULT_ENCODING = "UTF-8";

	public static final int DB_LOGIN_TIMEOUT = 3;
	// 索引常量
	public static final int INDEX_ZERO = 0;
	public static final int INDEX_FIRST = 1;
	public static final int INDEX_SECOND = 2;
	public static final int INDEX_LAST_OFFSET = -1;

	public static final String EXT_JAVA = ".java";
	public static final String EXT_YML = ".yml";
	public static final String EXT_YAML = ".yaml";
	public static final String EXT_XML = ".xml";
	public static final String EXT_PROPERTIES = ".properties";
	public static final String TEMPLATE_SKIP_PREFIX = "_";

	// 配置文件名
	public static final String VELOCITY_CONF_FILE = "velocity.properties";

	// 目录常量
	public static final String DIR_MODULES = "modules";
	public static final String DIR_COMPONENTS = "components";
	public static final String DIR_COMPONENTS_BASIC = "components-basic";
	public static final String ASSETS_DIR = "assets";

	// 上下文变量名
	public static final String CTX_TABLES = "tables";
	public static final String CTX_MODULE_NAME = "moduleName";
	public static final String CTX_CLASS_NAME = "className";

	/**
	 * 模板根目录
	 */
	public static final String TEMPLATE_ROOT = "templates";

	/**
	 * Velocity 模板目录
	 */
	public static final String VELOCITY_DIR = EngineType.VELOCITY.getName();

	/**
	 * FreeMarker 模板目录
	 */
	public static final String FREEMARKER_DIR = EngineType.FREEMARKER.getName();

	/**
	 * 模板目录路径
	 */
	public static final String TEMPLATE_DIR_VELOCITY = TEMPLATE_ROOT + "/" + VELOCITY_DIR;
	public static final String TEMPLATE_DIR_FREEMARKER = TEMPLATE_ROOT + "/" + FREEMARKER_DIR;

	/**
	 * 公共组件目录
	 */
	public static final String COMMONS_DIR = "commons";

	/**
	 * 特定组件目录
	 */
	public static final String COMPONENTS_DIR = "components";

	/**
	 * 模块目录
	 */
	public static final String MODULES_DIR = "modules";

	/**
	 * 注释模板目录
	 */
	public static final String COMMENTS_DIR = "comments";

	/**
	 * 宏模板文件
	 */
	public static final String MACROS_FILE = "macros.vm";

	/**
	 * 注释模板文件
	 */
	public static final String COMMENT_FILE = "comment.vm";

	// 错误信息
	public static final String ERR_NO_TABLES_FOUND = "没有找到要生成文档的表";
	public static final String ERR_DOC_GENERATION_FAILED = "生成文档失败: ";
	public static final String ERR_GLOBAL_CONFIG_NULL = "全局配置不能为空";
	public static final String ERR_OUTPUT_DIR_EMPTY = "输出目录不能为空";
	public static final String ERR_CREATE_OUTPUT_DIR = "无法创建输出目录: ";
	public static final String ERR_OUTPUT_DIR_NO_WRITE = "输出目录无写权限: ";
	public static final String ERR_TEMPLATE_DIR_NOT_EXIST = "模板目录不存在: ";
	public static final String ERR_DATASOURCE_NULL = "数据源配置不能为空";
	public static final String ERR_DB_CONNECTION_FAILED = "数据库连接失败";
	public static final String ERR_DB_CONNECTION_ERROR = "数据库连接异常: ";

	public static final String LOG_WRITE_FAILED = "写入文件失败: {}";

	// 文档生成常量
	public static final int DOC_TITLE_FONT_SIZE = 24;
	public static final int DOC_SUMMARY_FONT_SIZE = 14;
	public static final int DOC_TABLE_WIDTH = 8500;
	public static final int DOC_HEADER_FONT_SIZE = 14;
	public static final String DOC_DEFAULT_FONT_FAMILY = "宋体";
	public static final String DOC_TABLE_HEADER_COLOR = "B0C4DE";

	// PDF 生成常量
	public static final int PDF_PAGE_MARGIN = 50;
	public static final int PDF_LINE_HEIGHT = 15;
	public static final int PDF_TABLE_LINE_HEIGHT = 12;
	public static final int PDF_TITLE_FONT_SIZE = 18;
	public static final int PDF_SUBTITLE_FONT_SIZE = 14;
	public static final int PDF_BODY_FONT_SIZE = 10;
	public static final int PDF_HEADER_FONT_SIZE = 12;
	public static final float PDF_LINE_WIDTH = 0.5f;
	public static final float PDF_HEADER_ROW_HEIGHT = 12;
	public static final float PDF_CELL_PADDING = 5;
	public static final float PDF_COLUMN_WIDTH_SUMMARY[] = {60, 150, 200};
	public static final float PDF_COLUMN_WIDTH_FIELDS[] = {100, 120, 80, 80, 200};
	public static final int PDF_MIN_Y_POSITION = 50;
	public static final int PDF_TABLE_MARGIN = 20;
	public static final int PDF_PAGE_MARGIN_FOR_NEW_PAGE = 100;
	public static final String PDF_TABLE_HEADER_COLOR_RGB = "176, 196, 222";
	public static final int PDF_MAX_TEXT_LENGTH = 15;
	public static final int PDF_HEADER_FONT_SIZE_VALUE = 10;
	public static final int PDF_BODY_FONT_SIZE_VALUE = 9;

	// 字符串截断常量
	public static final String TEXT_ELLIPSIS = "...";

	// 缓存配置常量
	public static final int CACHE_SIZE_5000 = 5000;
	public static final int CACHE_SIZE_1000 = 1000;
	public static final int CACHE_EXPIRE_MINUTES_10 = 10;
	public static final int CACHE_EXPIRE_MINUTES_30 = 30;
	public static final long CACHE_SIZE_MAX = 1000L;
	public static final long CACHE_EXPIRE_TIME_30_MINUTES = 30L;
	public static final long CACHE_EXPIRE_TIME_10_MINUTES = 10L;

	// IO 缓冲区大小常量
	public static final int BUFFER_SIZE_8192 = 8192;
	public static final int BUFFER_SIZE_1024 = 1024;
	public static final int BOM_HEADER_LENGTH = 3;
	public static final int UTF8_BOM_BYTE1 = 0xEF;
	public static final int UTF8_BOM_BYTE2 = 0xBB;
	public static final int UTF8_BOM_BYTE3 = 0xBF;
	public static final int CONTROL_CHAR_THRESHOLD = 0x09;
	public static final int CONTROL_CHAR_CHECK_BYTES = 1024;

	// 路径相关常量
	public static final String PATH_WINDOWS_SEPARATOR = "\\";
	public static final String PATH_PLACEHOLDER_START = "{";
	public static final String PATH_PLACEHOLDER_END = "}";
	public static final String FILE_EXTENSION_SEPARATOR = ".";

	// 占位符常量
	public static final String DIR_PLACEHOLDER_START = "[";
	public static final String DIR_PLACEHOLDER_END = "]";
	public static final String MODULE_PLACEHOLDER_PREFIX = "${module[";
	public static final String MODULE_PLACEHOLDER_SUFFIX = "]}";
	public static final String BUSINESS_PLACEHOLDER_PREFIX = "${";

	// 占位符正则表达式
	public static final String NUMBER_PLACEHOLDER_REGEX = "\\{[0-9]+\\}";
	public static final String MODULE_PLACEHOLDER_REGEX = "\\$\\{module\\[[0-9]+\\]\\}";

	// 操作系统相关常量
	public static final String OS_NAME_WINDOWS = "Windows";
	public static final String OS_NAME_MAC = "mac";
	public static final String OS_NAME_LINUX = "linux";
	public static final String OS_NAME_UNIX = "nix";
	public static final String OS_NAME_PREFIX_WIN = "win";
	public static final String OS_COMMAND_WINDOWS = "cmd /c start ";
	public static final String OS_COMMAND_UNIX = "open ";
	public static final long DEFAULT_SERIAL_VERSION_UID = 1L;

	// 标准目录名
	public static final String DIR_PARENT = "parent";
	public static final String DIR_SRC = "src";
	public static final String DIR_MAIN = "main";
	public static final String DIR_TEST = "test";
	public static final String DIR_JAVA = "java";
	public static final String DIR_RESOURCES = "resources";

	// 模板扩展名正则
	public static final String TEMPLATE_EXT_REGEX = "\\.(ftl|vm)$";

	// 字段相关常量
	public static final String DEFAULT_PRIMARY_KEY_TYPE = "Long";
	public static final int MIN_PROPERTY_NAME_LENGTH = 1;

	// 列宽配置常量
	public static final int FIELD_COUNT_ADJUST_MAX = 15;
	public static final int FIELD_COUNT_THRESHOLD_FIXED_LEFT = 10;
	public static final int FIELD_COUNT_THRESHOLD_FIXED_LEFT_MANY = 15;
	public static final int INDEX_THRESHOLD_IMPORTANT_FIELDS = 3;
	public static final double IMPORTANCE_WEIGHT_PRIMARY_KEY = 1.5;
	public static final double IMPORTANCE_WEIGHT_NOT_NULL = 1.2;
	public static final double IMPORTANCE_WEIGHT_NULLABLE = 0.8;
	public static final double IMPORTANCE_WEIGHT_MULTIPLIER_TEXT = 1.3;
	public static final double IMPORTANCE_WEIGHT_MULTIPLIER_DATE = 1.2;
	public static final double IMPORTANCE_WEIGHT_MULTIPLIER_INT = 0.7;
	public static final double IMPORTANCE_WEIGHT_MULTIPLIER_NAME = 1.3;
	public static final double IMPORTANCE_WEIGHT_MULTIPLIER_CODE = 0.7;
	public static final double COLUMN_WIDTH_PERCENT_MIN = 5.0;
	public static final double COLUMN_WIDTH_PERCENT_MAX = 30.0;

	// 字段类型关键字
	public static final String[] FIELD_TYPE_TEXT_KEYWORDS = {"text", "blob"};
	public static final String[] FIELD_TYPE_DATE_KEYWORDS = {"date", "time"};
	public static final String[] FIELD_TYPE_INT_KEYWORDS = {"int", "tinyint", "smallint"};

	// 字段名称关键字
	public static final String[] FIELD_NAME_WIDE_KEYWORDS = {"name", "title", "description"};
	public static final String[] FIELD_NAME_NARROW_KEYWORDS = {"id", "code", "no", "type", "status"};

	// 敏感字段关键词
	public static final String[] SENSITIVE_FIELD_KEYWORDS = {"password", "passwd", "secret", "credential"};
	public static final String[] SENSITIVE_FIELD_SUFFIXES = {"pwd", "pass"};

	// 正则表达式常量
	public static final String REGEX_LINE_BREAK = "\\R";

	// 配置分隔符
	public static final String CONFIG_SEPARATOR_COMMA = ",";
	public static final String CONFIG_SEPARATOR_COLON = ":";
	public static final int CONFIG_SPLIT_LIMIT_TWO = 2;

	// 通配符转换
	public static final String WILDCARD_DOT_REGEX = "\\.";
	public static final String WILDCARD_DOT_REPLACE = "\\.";
	public static final String WILDCARD_STAR_REGEX = ".*";
	public static final String WILDCARD_STAR_REPLACE = "*";

	// 数据库URL关键字
	public static final String DB_URL_KEYWORD_MYSQL = ":mysql:";
	public static final String DB_URL_KEYWORD_ORACLE = ":oracle:";
	public static final String DB_URL_KEYWORD_SQLSERVER = ":sqlserver:";
	public static final String DB_URL_KEYWORD_POSTGRESQL = ":postgresql:";

	private Consts() {
	}
}
