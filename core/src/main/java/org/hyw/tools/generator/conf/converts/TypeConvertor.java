package org.hyw.tools.generator.conf.converts;

import org.hyw.tools.generator.conf.KeyPair;
import org.hyw.tools.generator.enums.FieldType;

/**
 * 
 * Filename:    TypeConvertor.java  
 * Description: 类型转换  
 * Copyright:   Copyright (c) 2015-2018 All Rights Reserved.
 * Company:     org.hyw.cn Inc.
 * @author:     heyiwu 
 * @version:    1.0  
 * Create at:   2017年6月13日 上午10:01:29  
 *
 */
public interface TypeConvertor {
    
    /**
     * 数据库表列类型转换java属性类型
     * @author:  heyiwu 
     * @param columnType 列类型
     * @return
     */
    KeyPair<String, FieldType> convert(String columnType);
}
