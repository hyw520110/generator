package org.hyw.tools.generator.web.service;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.hyw.tools.generator.Generator;
import org.hyw.tools.generator.conf.dao.DataSourceConf;
import org.hyw.tools.generator.conf.db.Table;
import org.hyw.tools.generator.conf.db.TableRelation;
import org.hyw.tools.generator.metadata.DatabaseMetadataReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class DatabaseMetadataService {

	private static final Logger logger = LoggerFactory.getLogger(DatabaseMetadataService.class);

	public List<String> getDatabases(Generator generator, String ipAndPort, String username, String pwd) {
		DataSourceConf ds = generator.getDataSource();
		ds.setIpAndPort(ipAndPort);
		ds.setUsername(username);
		ds.setPwd(pwd);
		ds.setDbName("");
		return ds.getDataBaseNames();
	}

	public void applyConnection(Generator generator, String ipAndPort, String dbName, String username, String pwd) {
		DataSourceConf ds = generator.getDataSource();
		ds.setIpAndPort(ipAndPort);
		ds.setDbName(dbName);
		ds.setUsername(username);
		ds.setPwd(pwd);
	}

	public void validateConnection(Generator generator, String ipAndPort, String dbName, String username, String pwd) {
		applyConnection(generator, ipAndPort, dbName, username, pwd);
		generator.getDataSource().getDataBaseNames();
	}

	public Map<String, Object> buildTableRelations(Generator generator, String ipAndPort, String dbName,
			String username, String pwd, String tabNames) throws Exception {
		applyConnection(generator, ipAndPort, dbName, username, pwd);
		DataSourceConf ds = generator.getDataSource();
		DatabaseMetadataReader metadataReader = new DatabaseMetadataReader(ds, generator.getGlobal());

		List<String> tableNames = StringUtils.isNotBlank(tabNames) ? Arrays.asList(tabNames.split(","))
				: generator.getAllTableNames();
		List<TableRelation> relations = metadataReader.getTableRelations(tableNames);

		List<Map<String, Object>> nodes = new ArrayList<>();
		Map<String, Boolean> addedTables = new HashMap<>();
		Map<String, Table> tableMap = new HashMap<>();
		for (Table table : generator.getTables(true)) {
			tableMap.put(table.getName(), table);
		}

		for (String tableName : tableNames) {
			addNodeIfAbsent(nodes, addedTables, tableMap, tableName);
		}
		for (TableRelation rel : relations) {
			addNodeIfAbsent(nodes, addedTables, tableMap, rel.getTargetTable());
		}

		List<Map<String, Object>> edges = new ArrayList<>();
		for (TableRelation rel : relations) {
			Map<String, Object> edge = new HashMap<>();
			edge.put("source", rel.getSourceTable());
			edge.put("target", rel.getTargetTable());
			edge.put("label", rel.getFkColumn());
			edge.put("fkColumn", rel.getFkColumn());
			edge.put("pkColumn", rel.getPkColumn());
			edge.put("fkName", rel.getFkName());
			edge.put("nullable", rel.isNullable());
			edges.add(edge);
		}

		Map<String, List<Map<String, Object>>> tableColumnsMap = new HashMap<>();
		Map<String, List<Map<String, Object>>> tableForeignKeysMap = new HashMap<>();
		readTableDetails(ds, dbName, username, pwd, addedTables, tableColumnsMap, tableForeignKeysMap);

		Map<String, Object> result = new HashMap<>();
		result.put("nodes", nodes);
		result.put("edges", edges);
		result.put("tableCount", tableNames.size());
		result.put("relationCount", relations.size());
		result.put("tableDetails", tableColumnsMap);
		result.put("tableForeignKeys", tableForeignKeysMap);
		return result;
	}

	private void addNodeIfAbsent(List<Map<String, Object>> nodes, Map<String, Boolean> addedTables,
			Map<String, Table> tableMap, String tableName) {
		if (addedTables.containsKey(tableName)) {
			return;
		}
		Map<String, Object> node = new HashMap<>();
		node.put("id", tableName);
		Table table = tableMap.get(tableName);
		String comment = table != null ? table.getComment() : "";
		node.put("label", StringUtils.isNotBlank(comment) ? comment : tableName);
		node.put("tableName", tableName);
		node.put("comment", comment);
		nodes.add(node);
		addedTables.put(tableName, true);
	}

	private void readTableDetails(DataSourceConf ds, String dbName, String username, String pwd,
			Map<String, Boolean> addedTables, Map<String, List<Map<String, Object>>> tableColumnsMap,
			Map<String, List<Map<String, Object>>> tableForeignKeysMap) throws Exception {
		java.sql.Connection directConn = null;
		try {
			String jdbcUrl = ds.getDBType().buildUrl(ds.getIp(), ds.getPort(), dbName);
			directConn = java.sql.DriverManager.getConnection(jdbcUrl, username, pwd);
			logger.info("[relations] 直连数据库获取元数据 - URL: {}", jdbcUrl);

			DatabaseMetaData metaData = directConn.getMetaData();
			String catalog = dbName;
			String schema = null;

			for (String tableName : addedTables.keySet()) {
				tableColumnsMap.put(tableName, readColumns(metaData, catalog, schema, tableName));
				tableForeignKeysMap.put(tableName, readForeignKeys(metaData, catalog, schema, tableName));
			}
		} finally {
			if (directConn != null && !directConn.isClosed()) {
				try {
					directConn.close();
				} catch (SQLException e) {
					logger.warn("关闭直连连接失败", e);
				}
			}
		}
	}

	private List<Map<String, Object>> readColumns(DatabaseMetaData metaData, String catalog, String schema,
			String tableName) throws SQLException {
		List<Map<String, Object>> columns = new ArrayList<>();
		try (ResultSet rs = metaData.getColumns(catalog, schema, tableName, null)) {
			while (rs.next()) {
				Map<String, Object> column = new HashMap<>();
				String columnName = rs.getString("COLUMN_NAME");
				String dataType = rs.getString("TYPE_NAME");
				int columnSize = rs.getInt("COLUMN_SIZE");
				int nullable = rs.getInt("NULLABLE");
				String defaultValue = rs.getString("COLUMN_DEF");
				String remark = rs.getString("REMARKS");

				column.put("columnName", columnName);
				column.put("dataType", dataType + (columnSize > 0 ? "(" + columnSize + ")" : ""));
				column.put("isPrimary", isPrimaryKey(metaData, catalog, schema, tableName, columnName));
				column.put("isForeignKey", false);
				column.put("isNullable", nullable == DatabaseMetaData.columnNullable);
				column.put("comment", StringUtils.defaultString(remark, ""));
				column.put("defaultValue", defaultValue);
				columns.add(column);
			}
		}
		return columns;
	}

	private boolean isPrimaryKey(DatabaseMetaData metaData, String catalog, String schema, String tableName,
			String columnName) throws SQLException {
		try (ResultSet pkRs = metaData.getPrimaryKeys(catalog, schema, tableName)) {
			while (pkRs.next()) {
				if (columnName.equals(pkRs.getString("COLUMN_NAME"))) {
					return true;
				}
			}
		}
		return false;
	}

	private List<Map<String, Object>> readForeignKeys(DatabaseMetaData metaData, String catalog, String schema,
			String tableName) throws SQLException {
		List<Map<String, Object>> foreignKeys = new ArrayList<>();
		try (ResultSet rs = metaData.getImportedKeys(catalog, schema, tableName)) {
			while (rs.next()) {
				Map<String, Object> fk = new HashMap<>();
				fk.put("column", rs.getString("FKCOLUMN_NAME"));
				fk.put("referenceTable", rs.getString("PKTABLE_NAME"));
				fk.put("referenceColumn", rs.getString("PKCOLUMN_NAME"));
				fk.put("fkName", rs.getString("FK_NAME"));
				foreignKeys.add(fk);
			}
		}
		return foreignKeys;
	}
}
