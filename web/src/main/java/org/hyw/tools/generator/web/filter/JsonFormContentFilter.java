package org.hyw.tools.generator.web.filter;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.lang.Nullable;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.FormContentFilter;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

public class JsonFormContentFilter extends FormContentFilter {

	private static final Logger logger = LoggerFactory.getLogger(JsonFormContentFilter.class);
	private static final List<String> HTTP_METHODS = Arrays.asList("PUT", "PATCH", "DELETE", "POST");
	public static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		MultiValueMap<String, String> params = parseIfNecessary(request);
		filterChain.doFilter(
				(null == params || params.isEmpty()) ? request : new FormContentRequestWrapper(request, params),
				response);
	}

	private MultiValueMap<String, String> parseIfNecessary(HttpServletRequest request) throws IOException {
		MultiValueMap<String, String> result = null;
		if (!shouldParse(request)) {
			return result;
		}
		StringWriter writer = new StringWriter();
		IOUtils.copy(new InputStreamReader(request.getInputStream()), writer);
		try {
			JSONObject json = JSON.parseObject(writer.toString());
			result = new LinkedMultiValueMap<>(json.keySet().size());
			for (String name : json.keySet()) {
				result.add(name, json.getString(name));
			}
		} catch (Exception e) {
			logger.error("{}:{}", e.getClass(), e.getLocalizedMessage());
		}
		return result;
	}

	private boolean shouldParse(HttpServletRequest request) {
		try {
			if (!HTTP_METHODS.contains(request.getMethod())) {
				return false;
			}
			MediaType mediaType = MediaType.parseMediaType(request.getContentType());
			return (MediaType.APPLICATION_JSON.includes(mediaType));
		} catch (IllegalArgumentException ex) {
		}
		return false;
	}

	private static class FormContentRequestWrapper extends HttpServletRequestWrapper {

		private MultiValueMap<String, String> formParams;

		public FormContentRequestWrapper(HttpServletRequest request, MultiValueMap<String, String> params) {
			super(request);
			this.formParams = params;
		}

		@Override
		@Nullable
		public String getParameter(String name) {
			String queryStringValue = super.getParameter(name);
			String formValue = this.formParams.getFirst(name);
			return (queryStringValue != null ? queryStringValue : formValue);
		}

		@Override
		public Map<String, String[]> getParameterMap() {
			Map<String, String[]> result = new LinkedHashMap<>();
			Enumeration<String> names = getParameterNames();
			while (names.hasMoreElements()) {
				String name = names.nextElement();
				result.put(name, getParameterValues(name));
			}
			return result;
		}

		@Override
		public Enumeration<String> getParameterNames() {
			Set<String> names = new LinkedHashSet<>();
			names.addAll(Collections.list(super.getParameterNames()));
			names.addAll(this.formParams.keySet());
			return Collections.enumeration(names);
		}

		@Override
		@Nullable
		public String[] getParameterValues(String name) {
			String[] parameterValues = super.getParameterValues(name);
			List<String> formParam = this.formParams.get(name);
			if (formParam == null) {
				return parameterValues;
			}
			if (parameterValues == null || getQueryString() == null) {
				return StringUtils.toStringArray(formParam);
			} else {
				List<String> result = new ArrayList<>(parameterValues.length + formParam.size());
				result.addAll(Arrays.asList(parameterValues));
				result.addAll(formParam);
				return StringUtils.toStringArray(result);
			}
		}
	}

}