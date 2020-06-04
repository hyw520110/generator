package ${aspectsPackage};

import java.util.Enumeration;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.support.config.FastJsonConfig;
import ${rootPackage}.${projectName}#if($!{moduleName}).${moduleName}#end.utils.SpringUtils;

import ${rootPackage}.${projectName}#if($!{moduleName}).${moduleName}#end.utils.HttpUtils;

@Profile({ "dev", "test" })
@Aspect
@Component
@Order(1)
public class LogAspect {

	private Logger logger = LoggerFactory.getLogger(getClass());

	@Pointcut("execution(public * com..*.controller..*.*(..))")
	public void reqLog() {
	}

//	@Before("reqLog()")
	public void doBefore(JoinPoint joinPoint) throws Throwable {
		ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
		HttpServletRequest request = attributes.getRequest();
		String headers = JSON.toJSONString(request.getHeaderNames());
		StringBuilder params = new StringBuilder();
		Object[] args = joinPoint.getArgs();
		if (args != null) {
			for (Object obj : args) {
				if (obj != null) {
					if (obj instanceof HttpServletRequest || obj instanceof HttpServletResponse) {
						continue;
					}
					if (obj instanceof MultipartFile) {
						params.append(String.format("上传文件,名称%s,文件大小%s", ((MultipartFile) obj).getOriginalFilename(),
								((MultipartFile) obj).getSize()));
					} else {
						params.append(JSON.toJSONString(obj));
					}
					params.append(",");
				}
			}
		}

		// 记录请求内容
		logger.info("{} {} {}", HttpUtils.getIpAddr(request),request.getMethod(),request.getRequestURL());
		logger.info("headers:{}", headers);
		if (StringUtils.isNotBlank(params.toString())) {
			logger.info("parameters:{}", params.toString());
		}
	}

	@AfterReturning(returning = "ret", pointcut = "reqLog()")
	public void doAfterReturning(Object ret) throws Throwable {
		// 记录下响应内容
		FastJsonConfig conf = null;
		try {
			conf=SpringUtils.getBean(FastJsonConfig.class);	
		} catch (Exception ignore) {
		}
		if (null == conf) {
			logger.info("response info: {}", JSON.toJSONString(ret));
		} else {
			logger.info("response info:{}",
					JSON.toJSONString(ret, conf.getSerializeFilters(), conf.getSerializerFeatures()));
		}
	}

}
