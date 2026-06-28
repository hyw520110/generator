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
            <artifactId>${mybatis_plus_starter_artifact!'mybatis-plus-spring-boot3-starter'}</artifactId>
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
<#elseif "postgresql"=="${dbType}">
		<!-- postgresql -->
		<dependency>
			<groupId>org.postgresql</groupId>
			<artifactId>postgresql</artifactId>
		</dependency>
<#elseif "oracle"=="${dbType}">
		<!-- oracle -->
		<dependency>
			<groupId>com.oracle.database.jdbc</groupId>
			<artifactId>ojdbc8</artifactId>
		</dependency>
<#elseif "sqlserver"=="${dbType}">
		<!-- sqlserver -->
		<dependency>
			<groupId>com.microsoft.sqlserver</groupId>
			<artifactId>mssql-jdbc</artifactId>
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
		<!-- Knife4j for Spring Boot 3 -->
		<dependency>
		    <groupId>com.github.xiaoymin</groupId>
		    <artifactId>${knife4j_starter_artifact!'knife4j-openapi3-jakarta-spring-boot-starter'}</artifactId>
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
<#if KAFKA?? && KAFKA>        
		<!-- kafka -->
		<dependency>
		    <groupId>org.springframework.kafka</groupId>
		    <artifactId>spring-kafka</artifactId>
		</dependency>
</#if>
<#if ELASTICSEARCH?? && ELASTICSEARCH>        
		<!-- elasticsearch -->
		<dependency>
		    <groupId>org.springframework.boot</groupId>
		    <artifactId>spring-boot-starter-data-elasticsearch</artifactId>
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
			<groupId>${validationApiGroupId}</groupId>
			<artifactId>${validationApiArtifactId}</artifactId>
		</dependency>
<#else>
		<dependency>
			<groupId>org.springframework</groupId>
			<artifactId>spring-context</artifactId>
			<version>${spring_version!}</version>
		</dependency>
		<dependency>
		  <groupId>org.springframework</groupId>
		  <artifactId>spring-jdbc</artifactId>
		  <version>${spring_version!}</version>
		</dependency>
		<dependency>
			<groupId>org.springframework</groupId>
			<artifactId>spring-web</artifactId>
			<version>${spring_version!}</version>
		</dependency>
		<dependency>
		  <groupId>${servletApiGroupId}</groupId>
		  <artifactId>${servletApiArtifactId}</artifactId>
		  <version>3.0-alpha-1</version>
		  <scope>provided</scope>
		</dependency>
</#if>
<#if global.modules?? && global.modules?size gt 1>
		<dependency>
			<groupId>${rootPackage!}</groupId>
			<artifactId>${projectName!}-api</artifactId>
			<version>${version!}</version>
		</dependency>
</#if>
			
<#if JWT?? && JWT>
			<dependency>
				<groupId>io.jsonwebtoken</groupId>
				<artifactId>jjwt-api</artifactId>
		</dependency>
		<dependency>
			<groupId>io.jsonwebtoken</groupId>
			<artifactId>jjwt-impl</artifactId>
		</dependency>
		<dependency>
				<groupId>io.jsonwebtoken</groupId>
				<artifactId>jjwt-jackson</artifactId>
			</dependency>
</#if>
	<#if SHIRO?? && SHIRO>
				<dependency>
					<groupId>org.apache.shiro</groupId>
				<artifactId>shiro-spring</artifactId>
			<#if shiroClassifier?has_content>
			<classifier>${shiroClassifier}</classifier>
			</#if>
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
			<#if shiroClassifier?has_content>
			<classifier>${shiroClassifier}</classifier>
				</#if>
				</dependency>
				<dependency>
				<groupId>org.apache.shiro</groupId>
				<artifactId>shiro-web</artifactId>
				<#if shiroClassifier?has_content>
				<classifier>${shiroClassifier}</classifier>
				</#if>
			</dependency>
	</#if>
				<dependency>
					<groupId>org.junit.jupiter</groupId>
					<artifactId>junit-jupiter</artifactId>
<#if !SPRINGBOOT?? || !SPRINGBOOT>
				<version>5.10.2</version>
</#if>
				<scope>test</scope>
			</dependency>
<#if SPRINGBOOT?? && SPRINGBOOT>
<#if javaVersion?? && (javaVersion?starts_with("17") || javaVersion?starts_with("21"))>
		<!-- JAXB API for Java 17+ compatibility -->
		<dependency>
			<groupId>${jaxbApiGroupId}</groupId>
			<artifactId>${jaxbApiArtifactId}</artifactId>
			<version>${jaxbApiVersion!'4.0.0'}</version>
		</dependency>
		<dependency>
			<groupId>org.glassfish.jaxb</groupId>
			<artifactId>jaxb-runtime</artifactId>
			<version>${jaxbRuntimeVersion!'4.0.2'}</version>
		</dependency>
</#if>
</#if>
	
		<#if GATEWAY?? && GATEWAY>
		<dependency>
			<groupId>org.springframework.cloud</groupId>
			<artifactId>spring-cloud-starter-gateway</artifactId>
		</dependency>
		</#if>

		<#if OPENAPI?? && OPENAPI>
		<dependency>
			<groupId>org.springdoc</groupId>
			<artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
			<version>2.3.0</version>
		</dependency>
		</#if>

		<#if OPENFEIGN?? && OPENFEIGN>
		<dependency>
			<groupId>org.springframework.cloud</groupId>
			<artifactId>spring-cloud-starter-openfeign</artifactId>
		</dependency>
		</#if>

		<#if SPRINGSECURITY?? && SPRINGSECURITY>
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-security</artifactId>
		</dependency>
		</#if>

        <#if VALIDATION?? && VALIDATION>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>
        </#if>

        <#if MULTIDATASOURCE?? && MULTIDATASOURCE>
        <dependency>
            <groupId>com.baomidou</groupId>
            <artifactId>dynamic-datasource-spring-boot-starter</artifactId>
            <version>3.5.2</version>
        </dependency>
        </#if>

        <#if WEBSOCKET?? && WEBSOCKET>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-websocket</artifactId>
        </dependency>
        </#if>

        <#if EXCEL?? && EXCEL>
        <dependency>
            <groupId>com.alibaba</groupId>
            <artifactId>easyexcel</artifactId>
            <version>3.3.3</version>
            
        </dependency>
        </#if>

        <#if SEATA?? && SEATA>
        <dependency>
            <groupId>com.alibaba.cloud</groupId>
            <artifactId>spring-cloud-starter-alibaba-seata</artifactId>
            
        </dependency>
        </#if>

        <#if OAUTH2?? && OAUTH2>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-oauth2-resource-server</artifactId>
            
        </dependency>
        </#if>

        <#if MULTICACHE?? && MULTICACHE>
        <dependency>
            <groupId>com.alicp.jetcache</groupId>
            <artifactId>jetcache-starter-redis</artifactId>
            <version>2.7.3</version>
            
        </dependency>
        </#if>

        <#if WORKFLOW?? && WORKFLOW>
        <dependency>
            <groupId>org.flowable</groupId>
            <artifactId>flowable-spring-boot-starter</artifactId>
            <version>6.8.0</version>
            
        </dependency>
        </#if>

        <#if JOB?? && JOB>
        <dependency>
            <groupId>com.xuxueli</groupId>
            <artifactId>xxl-job-core</artifactId>
            <version>2.4.0</version>
            
        </dependency>
        </#if>
</dependencies>
	<build>
		<finalName>${r'${project.artifactId}'}</finalName>
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
<#if SPRINGBOOT?? && SPRINGBOOT>			 <version>3.3.1</version></#if>
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
<#if SPRINGBOOT?? && SPRINGBOOT><version>3.13.0</version></#if>
					<configuration>
						<release>${r'${maven.compiler.release}'}</release>
						<encoding>${r'${project.build.sourceEncoding}'}</encoding>
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
				
		<#if GATEWAY?? && GATEWAY>
		<dependency>
			<groupId>org.springframework.cloud</groupId>
			<artifactId>spring-cloud-starter-gateway</artifactId>
		</dependency>
		</#if>

		<#if OPENAPI?? && OPENAPI>
		<dependency>
			<groupId>org.springdoc</groupId>
			<artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
			<version>2.3.0</version>
		</dependency>
		</#if>

		<#if OPENFEIGN?? && OPENFEIGN>
		<dependency>
			<groupId>org.springframework.cloud</groupId>
			<artifactId>spring-cloud-starter-openfeign</artifactId>
		</dependency>
		</#if>

		<#if SPRINGSECURITY?? && SPRINGSECURITY>
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-security</artifactId>
		</dependency>
		</#if>

        <#if VALIDATION?? && VALIDATION>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>
        </#if>

        <#if MULTIDATASOURCE?? && MULTIDATASOURCE>
        <dependency>
            <groupId>com.baomidou</groupId>
            <artifactId>dynamic-datasource-spring-boot-starter</artifactId>
            <version>3.5.2</version>
        </dependency>
        </#if>

        <#if WEBSOCKET?? && WEBSOCKET>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-websocket</artifactId>
        </dependency>
        </#if>

        <#if EXCEL?? && EXCEL>
        <dependency>
            <groupId>com.alibaba</groupId>
            <artifactId>easyexcel</artifactId>
            <version>3.3.3</version>
            
        </dependency>
        </#if>

        <#if SEATA?? && SEATA>
        <dependency>
            <groupId>com.alibaba.cloud</groupId>
            <artifactId>spring-cloud-starter-alibaba-seata</artifactId>
            
        </dependency>
        </#if>

        <#if OAUTH2?? && OAUTH2>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-oauth2-resource-server</artifactId>
            
        </dependency>
        </#if>

        <#if MULTICACHE?? && MULTICACHE>
        <dependency>
            <groupId>com.alicp.jetcache</groupId>
            <artifactId>jetcache-starter-redis</artifactId>
            <version>2.7.3</version>
            
        </dependency>
        </#if>

        <#if WORKFLOW?? && WORKFLOW>
        <dependency>
            <groupId>org.flowable</groupId>
            <artifactId>flowable-spring-boot-starter</artifactId>
            <version>6.8.0</version>
            
        </dependency>
        </#if>

        <#if JOB?? && JOB>
        <dependency>
            <groupId>com.xuxueli</groupId>
            <artifactId>xxl-job-core</artifactId>
            <version>2.4.0</version>
            
        </dependency>
        </#if>
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
<#if SPRINGBOOT?? && SPRINGBOOT><version>3.4.2</version></#if>
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
<#if SPRINGBOOT?? && SPRINGBOOT><version>3.7.1</version></#if>
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
