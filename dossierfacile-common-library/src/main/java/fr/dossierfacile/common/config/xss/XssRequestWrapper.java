package fr.dossierfacile.common.config.xss;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

public class XssRequestWrapper extends HttpServletRequestWrapper {

    public XssRequestWrapper(HttpServletRequest servletRequest) {
        super(servletRequest);
    }

    @Override
    @SuppressWarnings("java:S1168") // Jakarta Servlet specification requires null when parameter is not present
    public String[] getParameterValues(String parameter) {
        String[] values = super.getParameterValues(parameter);
        if (values == null) {
            return null;
        }

        int count = values.length;
        String[] encodedValues = new String[count];
        for (int i = 0; i < count; i++) {
            encodedValues[i] = XssSanitizer.sanitize(values[i]);
        }
        return encodedValues;
    }

    @Override
    public String getParameter(String parameter) {
        String value = super.getParameter(parameter);
        return XssSanitizer.sanitize(value);
    }

    @Override
    public String getHeader(String name) {
        String value = super.getHeader(name);
        return XssSanitizer.sanitize(value);
    }
}
