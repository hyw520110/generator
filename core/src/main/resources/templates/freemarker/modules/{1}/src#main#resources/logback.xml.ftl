<?xml version="1.0" encoding="UTF-8"?>
<!-- scan扫描配置文件的变化并自动重新配置 -->
<configuration scan="true" scanPeriod="30 seconds" debug="true" packagingData="${r"${log.packagingData:-true}"}">
	<timestamp key="timestamp" datePattern="yyyyMMdd'T'HHmmss"/>
	<property name="LOG_DIR" value="${r"${log.dir.name:-}"}${projectName!}${r"${log.dir.suffix:-}"}"/>
	<property name="LOG_BASE" value="./logs/${r"${LOG_DIR}"}/${r"${LOG_DIR}"}"  />
	<property name="CHARSET" value="UTF-8" />
	<jmxConfigurator />
	<property name="CONSOLE_PATTERN" value="%yellow(%date{yyyy-MM-dd HH:mm:ss}) |%highlight(%-5level) |%blue(%thread) |%blue(%file:%line) |%green(%logger) |%cyan(%msg%n)"/>
	<property name="pattern" value="%-15d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %replace(%caller{1}){'\t|Caller.{1}0| at|\R |\n', ''} %X{userId}-%X{_sessionId}-%X{_ip} %msg%n %ex{FULL}"/>
	<appender name="stdout" class="ch.qos.logback.core.ConsoleAppender">
		<encoder class="ch.qos.logback.classic.encoder.PatternLayoutEncoder"
			charset="${r"${CHARSET}"}">
			<pattern>${r"${pattern}"}</pattern>
		</encoder>
	</appender>
	<!-- 错误日志配置 RollingFileAppender继承自ch.qos.logback.core.FileAppender -->
	<appender name="common-error"
		class="ch.qos.logback.core.rolling.RollingFileAppender">
		<!-- 限定只输出ERROR级别的日志 -->
		<filter class="ch.qos.logback.classic.filter.LevelFilter">
			<level>ERROR</level>
			<onMatch>ACCEPT</onMatch>
			<onMismatch>DENY</onMismatch>
		</filter>
		<File>${r"${LOG_BASE}"}_error.log</File>
		<rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
			<fileNamePattern>${r"${LOG_BASE}"}_error_%d{yyyy-MM-dd_HH}.%i.log
			</fileNamePattern>
			<!-- 保留天数 -->
			<maxHistory>${r"${log.maxHistory:-60}"}</maxHistory>
			<timeBasedFileNamingAndTriggeringPolicy
				class="ch.qos.logback.core.rolling.SizeAndTimeBasedFNATP">
				<maxFileSize>${r"${log.maxFileSize:-10MB}"}</maxFileSize>
			</timeBasedFileNamingAndTriggeringPolicy>
		</rollingPolicy>
		<append>true</append>
		<encoder charset="${r"${CHARSET}"}"
			class="ch.qos.logback.classic.encoder.PatternLayoutEncoder">
			<pattern>${r"${pattern}"}</pattern>
			<charset>${r"${CHARSET}"}</charset>
			<immediateFlush>true</immediateFlush>
		</encoder>
	</appender>
	<!-- 应用日志 -->
	<appender name="common-default"
		class="ch.qos.logback.core.rolling.RollingFileAppender">
		<File>${r"${LOG_BASE}"}_default.log</File>
		<rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
			<fileNamePattern>${r"${LOG_BASE}"}_default.%d{yyyy-MM-dd}.%i.log
			</fileNamePattern>
			<!-- 保留天数 -->
			<maxHistory>${r"${log.maxHistory:-60}"}</maxHistory>
			<timeBasedFileNamingAndTriggeringPolicy
				class="ch.qos.logback.core.rolling.SizeAndTimeBasedFNATP">
				<maxFileSize>${r"${log.maxFileSize:-10MB}"}</maxFileSize>
			</timeBasedFileNamingAndTriggeringPolicy>
		</rollingPolicy>
		<append>true</append>
		<encoder charset="${r"${CHARSET}"}"
			class="ch.qos.logback.classic.encoder.PatternLayoutEncoder">
			<pattern>${r"${pattern}"}</pattern>
			<charset>${r"${CHARSET}"}</charset>
			<immediateFlush>true</immediateFlush>
		</encoder>
	</appender>

	<!-- logger的向上传递性属性 additivity默认为true -->
	<logger name="ch.qos.logback.classic" level="WARN" />
	<logger name="java.sql" level="WARN" />
	<logger name="jdbc" level="WARN" />
	<logger name="org.hibernate" level="INFO" />
	<logger name="org.hibernate.cache" level="ERROR" />
	<logger name="org.hibernate.sql" level="INFO" />
	<logger name="org.hibernate.engine" level="INFO" />
	<logger name="org.hibernate.type" level="ERROR" />
	<logger name="org.hibernate.util" level="ERROR" />
	<logger name="org.hibernate.cfg" level="WARN" />

	<logger name="org.springframework" level="INFO" />
	<logger name="org.springframework.web" level="INFO" />

	<logger name="org.apache" level="INFO" />
	<logger name="org.apache.shiro" level="INFO" />
	<logger name="org.apache.mina" level="WARN" />

	<logger
		name="org.springframework.web.servlet.handler.SimpleMappingExceptionResolver"
		level="WARN" />

	<logger name="com.danga.MemCached" level="INFO" />
	<logger name="net.sf.ehcache" level="INFO" />
	<logger name="freemarker" level="INFO" />

	<logger name="org.eclipse.jetty" level="INFO" />
	<logger name="com.opensymphony" level="WARN" />
	<logger name="net.sf" level="WARN" />
	<!-- 输出SQL语句 -->
	<logger name="druid.sql.Statement" level="${r"${log.druid.sql.Statement.level:-DEBUG}"}" />
	<logger name="com.alibaba.dubbo" level="INFO" />
	<root level="${r"${log.root.level:-INFO}"}">
		<!--将appender添加到root logger下 -->
		<appender-ref ref="stdout"  level="${r"${log.stdout.level:-INFO}"}"/>
		<appender-ref ref="common-error" level="ERROR" />
		<appender-ref ref="common-default"   />
	</root>
 
</configuration>
