package org.hyw.tools.generator.web.filter;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hyw.tools.generator.web.service.UserGeneratorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class GeneratorClientIdFilter extends OncePerRequestFilter {

	@Autowired
	private UserGeneratorService userGeneratorService;

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		userGeneratorService.ensureClientId(request, response);
		filterChain.doFilter(request, response);
	}
}
