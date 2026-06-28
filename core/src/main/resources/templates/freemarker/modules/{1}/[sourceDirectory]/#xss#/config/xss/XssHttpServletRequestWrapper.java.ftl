package ${packagePath};

import ${servletPackage}.http.HttpServletRequest;
import ${servletPackage}.http.HttpServletRequestWrapper;
import org.springframework.web.util.HtmlUtils;

public class XssHttpServletRequestWrapper extends HttpServletRequestWrapper {
    public XssHttpServletRequestWrapper(HttpServletRequest request) {
        super(request);
    }
    @Override
    public String[] getParameterValues(String parameter) {
        String[] values = super.getParameterValues(parameter);
        if (values == null) return null;
        int count = values.length;
        String[] encodedValues = new String[count];
        for (int i = 0; i < count; i++) {
            encodedValues[i] = HtmlUtils.htmlEscape(values[i]);
        }
        return encodedValues;
    }
    @Override
    public String getParameter(String parameter) {
        String value = super.getParameter(parameter);
        return value == null ? null : HtmlUtils.htmlEscape(value);
    }
    @Override
    public String getHeader(String name) {
        String value = super.getHeader(name);
        return value == null ? null : HtmlUtils.htmlEscape(value);
    }
}
