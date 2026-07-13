<#if projectBuilder == "MAVEN">
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
	xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
	<modelVersion>4.0.0</modelVersion>
	<parent>
		<groupId>${rootPackage!}</groupId>
		<artifactId>${projectName!}-parent</artifactId>
		<version>${version!}</version>
		<relativePath>../parent</relativePath>
	</parent>
	<groupId>${rootPackage!}</groupId>
	<artifactId>${projectName!}-${moduleName!}</artifactId>
	<version>${version!}</version>
	<packaging>jar</packaging>
		<properties>
			<maven.compiler.source>${javaVersion!}</maven.compiler.source>
			<maven.compiler.target>${javaVersion!}</maven.compiler.target>
			<maven.compiler.release>${bytecodeRelease!javaVersion!}</maven.compiler.release>
			<project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
	</properties>
	<dependencies>
	<#if (sqlType!'xml') == "plus">
		<dependency>
			<groupId>com.baomidou</groupId>
			<artifactId>mybatis-plus-spring</artifactId>
			<version>${mybatis_plus_version!'3.5.16'}</version>
		</dependency>
	</#if>
		<dependency>
		 	<groupId>org.apache.commons</groupId>
		  	<artifactId>commons-lang3</artifactId>
		  	<version>${commons_lang3_version!'3.14.0'}</version>
		</dependency>
		<dependency>
			    <groupId>org.hibernate.validator</groupId>
			    <artifactId>hibernate-validator</artifactId>
			    <#if !(SPRINGBOOT?? && SPRINGBOOT)><version>6.2.5.Final</version></#if>
			</dependency>
			<dependency>
			    <groupId>${validationApiGroupId}</groupId>
			    <artifactId>${validationApiArtifactId}</artifactId>
			</dependency><#if JPA?? && JPA>		
		<dependency>
		  <groupId>org.springframework.boot</groupId>
		  <artifactId>spring-boot-starter-data-jpa</artifactId>
		</dependency>
</#if>
		<!-- OpenAPI 3 注解 -->
		<dependency>
			<groupId>io.swagger.core.v3</groupId>
			<artifactId>swagger-annotations</artifactId>
			<version>2.2.15</version>
		</dependency>
		<!-- Jackson -->
		<dependency>
			<groupId>com.fasterxml.jackson.core</groupId>
			<artifactId>jackson-annotations</artifactId>
		</dependency>
<#if MYBATIS?? && MYBATIS>
<#if sqlType?? && sqlType == "plus">
		<dependency>
			<groupId>com.baomidou</groupId>
			<artifactId>mybatis-plus-core</artifactId>
		</dependency>
		<dependency>
			<groupId>com.baomidou</groupId>
			<artifactId>mybatis-plus-extension</artifactId>
		</dependency>
</#if>
</#if>				
		<dependency>
			<groupId>org.junit.jupiter</groupId>
			<artifactId>junit-jupiter</artifactId>
			<version>5.10.2</version>
			<scope>test</scope>
		</dependency>
	</dependencies>
	<build>
		<plugins>
			<!-- 编译插件：设置编译版本、编码 -->
				<plugin>
					<groupId>org.apache.maven.plugins</groupId>
					<artifactId>maven-compiler-plugin</artifactId>
					<version>3.13.0</version>
					<configuration>
						<release><#noparse>${maven.compiler.release}</#noparse></release>
						<encoding><#noparse>${project.build.sourceEncoding}</#noparse></encoding>
					</configuration>
				</plugin>
			<!-- 源码jar插件 -->
			<plugin>
				<groupId>org.apache.maven.plugins</groupId>
				<artifactId>maven-source-plugin</artifactId>
				<version>3.0.0</version>
				<executions>
					<execution>
						<id>attach-sources</id>
						<goals>
							<goal>jar</goal>
						</goals>
					</execution>
				</executions>
			</plugin>
		</plugins>
	</build>
	<profiles>
		<!-- 生产环境,调用开发环境配置,默认激活 -->
		<profile>
			<id>prod</id>
			<properties>
				<env.profile>main</env.profile>
			</properties>
		</profile>
		<!-- 开发环境,默认激活(在settings.xml里配置的) mvn deploy或mvn deploy -Pdev -->
		<profile>
			<id>dev</id>
			<properties>
				<env.profile>main</env.profile>
			</properties>
		</profile>
		<!-- 测试环境,mvn install -Ptest或mvn deploy -Ptest -->
		<profile>
			<id>test</id>
			<properties>
				<env.profile>test</env.profile>
				<project.release.version>1.1.0-SNAPSHOT</project.release.version>
			</properties>
		</profile>
	</profiles>
</project>
</#if>
