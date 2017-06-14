package com.tzg.tools.generator.conf;

import java.io.InputStream;
import java.io.Serializable;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

public class BaseBean implements Serializable {
    private static final long serialVersionUID = 1L;

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }

    public static InputStream getResourceAsStream(String path) {
        return BaseBean.class.getResourceAsStream(path);
    }
}
