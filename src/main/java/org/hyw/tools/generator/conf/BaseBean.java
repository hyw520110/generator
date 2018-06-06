package org.hyw.tools.generator.conf;

import java.io.InputStream;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

public class BaseBean implements Serializable {
	private static final long serialVersionUID = 1L;
	protected String pattern = "yyyy-MM-dd HH:mm:ss";

	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
	}

	public static InputStream getResourceAsStream(String path) {
		return BaseBean.class.getResourceAsStream(path);
	}

	public String getDate() {
		return new SimpleDateFormat(pattern).format(new Date());
	}

	protected void close(AutoCloseable... args) {
		if (null == args || args.length == 0) {
			return;
		}
		for (AutoCloseable arg : args) {
			if (null == arg) {
				continue;
			}
			try {
				arg.close();
			} catch (Exception ignore) {
			}
		}
	}
}
