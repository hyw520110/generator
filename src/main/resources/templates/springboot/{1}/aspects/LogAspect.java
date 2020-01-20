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
import com.big.box.demo.app.utils.SpringUtils;

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

	@Before("reqLog()")
	public void doBefore(JoinPoint joinPoint) throws Throwable {
		ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
		HttpServletRequest request = attributes.getRequest();

		Enumeration<String> headerNames = request.getHeaderNames();
		String headers = JSON.toJSONString(headerNames);

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
		logger.info("-------------请求信息开始--------------");
		logger.info("发起请求的IP:{}", HttpUtils.getIpAddr(request));
		logger.info("请求url:{} {}", request.getRequestURL().toString(),
				request.getQueryString() == null ? "" : request.getQueryString());
		logger.info("请求方式:{}", request.getMethod());
		logger.info("请求头:{}", headers);
		if (StringUtils.isNotBlank(params.toString())) {
			logger.info("请求参数:{}", params.toString());
		}
		logger.info("-------------请求信息结束--------------");
	}

	@AfterReturning(returning = "ret", pointcut = "reqLog()")
	public void doAfterReturning(Object ret) throws Throwable {
		// 记录下响应内容
		logger.info("-------------响应信息开始--------------");
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
		logger.info("-------------响应信息结束--------------");
	}

}
