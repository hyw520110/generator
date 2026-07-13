package org.hyw.tools.generator.metadata;

import org.hyw.tools.generator.conf.GlobalConf;
import org.hyw.tools.generator.conf.dao.DataSourceConf;

public class MetadataReaderFactory {
    
    public static MetadataReader create(DataSourceConf dataSource, GlobalConf global) {
        if ("SQL_FILE".equalsIgnoreCase(dataSource.getSourceType())) {
            return new SqlFileMetadataReader(dataSource, global);
        }
        // Default to JDBC
        return new DatabaseMetadataReader(dataSource, global);
    }
}
