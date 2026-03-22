<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
	<modelVersion>4.0.0</modelVersion>
	<parent>
		<groupId>${rootPackage!}</groupId>
		<artifactId>${projectName!}-parent</artifactId>
		<version>${version!}</version>
		<relativePath>../parent</relativePath>
	</parent>
	<artifactId>${projectName!}-${moduleName!}</artifactId>
	<packaging>jar</packaging>
	<properties>
		<mainClass>${rootPackage!}.${projectName!}.${moduleName!}.Booter</mainClass>
	</properties>
	<dependencies>
<#if SPRINGBOOT?? && SPRINGBOOT>
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-actuator</artifactId>
		</dependency>
<#if ZOOKEEPER>
	<!-- zk config service -->
		<dependency>
			<groupId>org.springframework.cloud</groupId>
			<artifactId>spring-cloud-starter-zookeeper-config</artifactId>
			<exclusions>
				<exclusion>
					<groupId>org.springframework.boot</groupId>
					<artifactId>spring-boot-starter-tomcat</artifactId>
				</exclusion>
			</exclusions>
		</dependency>
		<!-- zk service discovery -->
		<dependency>
			<groupId>org.springframework.cloud</groupId>
			<artifactId>spring-cloud-starter-zookeeper-discovery</artifactId>
			<exclusions>
				<exclusion>
					<groupId>org.apache.zookeeper</groupId>
					<artifactId>zookeeper</artifactId>
				</exclusion>
			</exclusions>
		</dependency>
		<dependency>
			<groupId>org.apache.zookeeper</groupId>
			<artifactId>zookeeper</artifactId>
			<optional>true</optional>
			<exclusions>
				<exclusion>
					<groupId>org.slf4j</groupId>
					<artifactId>slf4j-log4j12</artifactId>
				</exclusion>
				<exclusion>
					<groupId>log4j</groupId>
					<artifactId>log4j</artifactId>
				</exclusion>
			</exclusions>
		</dependency>
		<dependency>
			<groupId>org.apache.curator</groupId>
			<artifactId>curator-framework</artifactId>
		</dependency>
<#if SENTINEL>
        <!-- ZooKeeper配置接口限流规则 -->
		<dependency>
			<groupId>com.alibaba.csp</groupId>
			<artifactId>sentinel-datasource-zookeeper</artifactId>
		</dependency>
</#if>
<#else>
		<dependency>
			<groupId>com.alibaba.csp</groupId>
			<artifactId>sentinel-core</artifactId> 
		</dependency>
		<dependency>
			<groupId>com.alibaba.csp</groupId>
			<artifactId>sentinel-annotation-aspectj</artifactId>
		</dependency>
		<!-- 接入sentinel控制台实时监控 -->
		<dependency>
			<groupId>com.alibaba.csp</groupId> 
			<artifactId>sentinel-transport-simple-http</artifactId>
		</dependency>
</#if>
<#if SENTINEL>
		<dependency>
			<groupId>com.alibaba.cloud</groupId>
			<artifactId>spring-cloud-starter-alibaba-sentinel</artifactId>
		</dependency>
</#if>
<#if DUBBO?? && DUBBO>
		<!-- Dubbo Spring Boot Starter (Apache Dubbo 原生 starter，版本由 dubbo-bom 管理) -->
		<dependency>
			<groupId>org.apache.dubbo</groupId>
			<artifactId>dubbo-spring-boot-starter</artifactId>
		</dependency>
</#if>
<#if mapperType?? && mapperType == "plus">
		<!-- mybatis-plus -->
        <dependency>
            <groupId>com.baomidou</groupId>
            <artifactId>mybatis-plus-spring-boot3-starter</artifactId>
        </dependency>
    	<!-- mybatis plus generator -->
		<dependency>
		    <groupId>com.baomidou</groupId>
		    <artifactId>mybatis-plus-generator</artifactId>
		    <scope>test</scope>
		</dependency>
		<dependency>
		    <groupId>org.apache.velocity</groupId>
		    <artifactId>velocity-engine-core</artifactId>
		    <version>2.2</version>
		    <scope>test</scope>
		</dependency>
<#else>
		<!-- mybatis -->
		<dependency>
			<groupId>org.mybatis.spring.boot</groupId>
			<artifactId>mybatis-spring-boot-starter</artifactId>
		</dependency>
</#if>		
		<!--pagehelper -->
		<dependency>
			<groupId>com.github.pagehelper</groupId>
			<artifactId>pagehelper-spring-boot-starter</artifactId>
		</dependency>
<#if enableCache?has_content>
		 <dependency>
            <groupId>net.sf.ehcache</groupId>
            <artifactId>ehcache</artifactId>
<#if SPRINGBOOT?? && SPRINGBOOT>            
            <version>2.10.4</version>
</#if>
        </dependency>
		<dependency>
			<groupId>org.mybatis.caches</groupId>
			<artifactId>mybatis-ehcache</artifactId>
			<version>1.1.0</version>
		</dependency>
<#if SPRINGBOOT?? && SPRINGBOOT>
<#if REDIS?? && REDIS>
		<!-- https://www.jianshu.com/p/15d0a9ce37dd -->
		 <dependency>
            <groupId>com.github.ben-manes.caffeine</groupId>
            <artifactId>caffeine</artifactId>
        </dependency>
</#if>
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-cache</artifactId>
		</dependency>
</#if>
</#if>
<#if "mysql"=="${dbType}">
		<!-- mysql -->
		<dependency>
			<groupId>com.mysql</groupId>
			<artifactId>mysql-connector-j</artifactId>
		</dependency>
</#if>
		<dependency>
			<groupId>com.alibaba</groupId>
			<artifactId>druid-spring-boot-starter</artifactId>
		</dependency>
<#if SPRINGBOOT?? && SPRINGBOOT>
		<!-- 
		<dependency>
			<groupId>org.springframework</groupId>
			<artifactId>springloaded</artifactId>
		</dependency> 
		<dependency>
		  <groupId>org.springframework.boot</groupId>
		  <artifactId>spring-boot-devtools</artifactId>
		  <optional>true</optional>
		</dependency>
		-->
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-web</artifactId>
			<exclusions>
				<exclusion>
					<groupId>org.springframework.boot</groupId>
					<artifactId>spring-boot-starter-tomcat</artifactId>
				</exclusion>
			</exclusions>
		</dependency>
<#if THYMELEAF?? && THYMELEAF>
		<dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-thymeleaf</artifactId>
            <exclusions>
            	<exclusion>
            		<groupId>org.springframework.boot</groupId>
                	<artifactId>spring-boot-starter-tomcat</artifactId>
            	</exclusion>
            </exclusions>
		</dependency>
</#if>
<#if REDIS>
		<dependency>  
		    <groupId>org.springframework.boot</groupId>  
		    <artifactId>spring-boot-starter-data-redis</artifactId>  
		</dependency>  
</#if>
		<!-- Undertow是红帽公司的java开源高性能web服务器(Wildfly默认的Web服务器)-->  
		<dependency>
	        <groupId>org.springframework.boot</groupId>
	        <artifactId>spring-boot-starter-undertow</artifactId>
		</dependency>
<#if "fastjson"=="${json_type!}">
		<dependency>
		  <groupId>com.alibaba</groupId>
		  <artifactId>fastjson</artifactId>
		  <version>1.2.83</version>
		</dependency>
</#if>

<#if SWAGGER2?? && SWAGGER2>
		<!-- knife4j for Spring Boot 3.x (OpenAPI 3 / Swagger 3) -->
		<dependency>
		    <groupId>com.github.xiaoymin</groupId>
		    <artifactId>knife4j-openapi3-jakarta-spring-boot-starter</artifactId>
		    <version>4.3.0</version>
		</dependency>
</#if>
<#if "${javaVersion!}"=="1.8"  &&  !SPRINGBOOT  &&  !DUBBO  &&  !ZIPKIN>    	
		<!-- 实时数据追踪 -->
    	<dependency>
            <groupId>com.github.jessyZu</groupId>
   			<artifactId>dubbo-zipkin-spring-starter</artifactId>
   			<version>${r"${zipkin_version}"}</version>
        </dependency>
<#if ROCKETMQ>        
		<!-- rocketmq -->
		<dependency>
		    <groupId>com.maihaoche</groupId>
		    <artifactId>spring-boot-starter-rocketmq</artifactId>
		    <version>${r"${spring_boot_starter_rocketmq_version}"}</version>
		</dependency>
</#if>
</#if>
	    <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-jdbc</artifactId>
        </dependency>
</#if>
   		<!-- io工具集 -->
		<dependency>
			<groupId>commons-io</groupId>
			<artifactId>commons-io</artifactId>
			<version>2.4</version>
		</dependency>
		<dependency>
			<groupId>commons-codec</groupId>
			<artifactId>commons-codec</artifactId>
			<version>1.11</version>
		</dependency>
		<dependency>
		   <groupId>org.springframework.boot</groupId>
		   <artifactId>spring-boot-starter-test</artifactId>
		   <scope>test</scope>
		</dependency>
<#if lombok?has_content && lombok>
		<!-- lombok -->
		<dependency>
			<groupId>org.projectlombok</groupId>
			<artifactId>lombok</artifactId>
			<optional>true</optional>
		</dependency>
</#if>
		<!-- Jakarta Validation API (Spring Boot 3.x) -->
		<dependency>
			<groupId>jakarta.validation</groupId>
			<artifactId>jakarta.validation-api</artifactId>
		</dependency>
<#else>
		<dependency>
			<groupId>org.springframework</groupId>
			<artifactId>spring-context</artifactId>
			<version>${spring.version!}</version>
		</dependency>
		<dependency>
		  <groupId>org.springframework</groupId>
		  <artifactId>spring-jdbc</artifactId>
		  <version>${spring.version!}</version>
		</dependency>
		<dependency>
			<groupId>org.springframework</groupId>
			<artifactId>spring-web</artifactId>
			<version>${spring.version!}</version>
		</dependency>
		<dependency>
		  <groupId>jakarta.servlet</groupId>
		  <artifactId>servlet-api</artifactId>
		  <version>3.0-alpha-1</version>
		  <scope>provided</scope>
		</dependency>
</#if>
		<dependency>
			<groupId>${rootPackage!}</groupId>
			<artifactId>${projectName!}-api</artifactId>
			<version>1.0.1-SNAPSHOT</version>
		</dependency>
		
		<dependency>
			<groupId>io.jsonwebtoken</groupId>
			<artifactId>jjwt</artifactId>
		</dependency>
		<dependency>
			<groupId>org.apache.shiro</groupId>
			<artifactId>shiro-spring</artifactId>
			<classifier>jakarta</classifier>
			<exclusions>
				<exclusion>
					<groupId>org.apache.shiro</groupId>
					<artifactId>shiro-web</artifactId>
				</exclusion>
				<exclusion>
					<groupId>org.apache.shiro</groupId>
					<artifactId>shiro-core</artifactId>
				</exclusion>
			</exclusions>
		</dependency>
		<dependency>
			<groupId>org.apache.shiro</groupId>
			<artifactId>shiro-core</artifactId>
			<classifier>jakarta</classifier>
		</dependency>
		<dependency>
			<groupId>org.apache.shiro</groupId>
			<artifactId>shiro-web</artifactId>
			<classifier>jakarta</classifier>
		</dependency>
		<dependency>
			<groupId>junit</groupId>
			<artifactId>junit</artifactId>
<#if SPRINGBOOT?? && SPRINGBOOT>
			<version>4.12</version>
</#if>
			<scope>test</scope>
		</dependency>
	</dependencies>
	<build>
		<finalName>${project.artifactId!}</finalName>
		<resources>
			 <resource>
			    <directory>src/main/resources</directory>
			    <includes>
			    	<include>*.yml</include>
			    </includes>
			    <filtering>true</filtering>
			 </resource>
		 	 <resource>
			    <directory>src/main/resources</directory>
			    <excludes>
			    	<exclude>*.yml</exclude>
			    </excludes>
			    <filtering>false</filtering>
			 </resource>
		</resources>	
		<plugins>
			<plugin>
			 <groupId>org.apache.maven.plugins</groupId>
			 <artifactId>maven-resources-plugin</artifactId>
<#if SPRINGBOOT?? && SPRINGBOOT>			 <version>2.6</version></#if>
			 <configuration>
			    <delimiters>
			       <delimiter>@</delimiter>
			    </delimiters>
			    <useDefaultDelimiters>false</useDefaultDelimiters>
			 </configuration>
			</plugin>
			<!-- 编译插件：设置编译版本、编码 -->
			<plugin>
				<groupId>org.apache.maven.plugins</groupId>
				<artifactId>maven-compiler-plugin</artifactId>
<#if SPRINGBOOT?? && SPRINGBOOT><version>3.3</version></#if>
				<configuration>
					<source>${maven.compiler.source!}</source>
					<target>${maven.compiler.target!}</target>
					<encoding>${project.build.sourceEncoding!}</encoding>
				</configuration>
			</plugin>
<#if SPRINGBOOT?? && SPRINGBOOT>
			<plugin>
				<groupId>org.springframework.boot</groupId>
				<artifactId>spring-boot-maven-plugin</artifactId>
				<version>${r"${spring-boot.version}"}</version>
				<!-- springloaded -->
				<!-- <dependencies>
					<dependency>
						<groupId>org.springframework</groupId>
						<artifactId>springloaded</artifactId>
						<version>1.2.7.RELEASE</version>
					</dependency>
				</dependencies>
				<executions>
					<execution>
						<goals>
							<goal>repackage</goal>
						</goals>
						<configuration>
							<classifier>exec</classifier>
						</configuration>
					</execution>
				</executions> -->
				<!-- spring-boot-devtools -->
				<configuration>
					<fork>true</fork>
					<!-- <jvmArguments>
                        -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=8000
                    </jvmArguments> -->
				</configuration>
			</plugin>
</#if>

		<plugin>
				<groupId>org.apache.maven.plugins</groupId>
				<artifactId>maven-jar-plugin</artifactId>
<#if SPRINGBOOT?? && SPRINGBOOT><version>2.6</version></#if>
				<configuration>
					<archive>
						<addMavenDescriptor>false</addMavenDescriptor>
						<manifestEntries>
							<Class-Path>. ../ ../conf ../templates ../static </Class-Path>
						</manifestEntries>
						<manifest>
							<addClasspath>true</addClasspath>
							<classpathPrefix>../lib/</classpathPrefix>
							<mainClass>${mainClass!}</mainClass>
						</manifest>
					</archive>
					<excludes>
						<exclude>**/bin/**</exclude>
						<exclude>/conf/**</exclude>
						<exclude>/templates/**</exclude>
						<exclude>/static/**</exclude>
						<exclude>**/maven/**</exclude>
						<exclude>*.yml</exclude>
						<exclude>*.xml</exclude>
					</excludes>
				</configuration>
			</plugin>
			<plugin>
				<groupId>org.apache.maven.plugins</groupId>
				<artifactId>maven-assembly-plugin</artifactId>
<#if SPRINGBOOT?? && SPRINGBOOT><version>2.2.1</version></#if>
				<configuration>
					<descriptors>
						<descriptor>src/main/resources/maven/assembly.xml</descriptor>
					</descriptors>
				</configuration>
				<executions>
					<execution>
						<id>make-assembly</id>
						<phase>package</phase>
						<goals>
							<goal>single</goal>
						</goals>
					</execution>
				</executions>
			</plugin>			
		</plugins>
	</build>
</project>