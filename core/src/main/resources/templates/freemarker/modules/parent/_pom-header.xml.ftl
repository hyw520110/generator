<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
	xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
	<modelVersion>4.0.0</modelVersion>
	<groupId>${rootPackage!}</groupId>
	<artifactId>${projectName!}-parent</artifactId>
	<version>${version!}</version>
	<packaging>pom</packaging>
	<properties>
		<maven.compiler.source>${javaVersion!}</maven.compiler.source>
		<maven.compiler.target>${javaVersion!}</maven.compiler.target>
		<project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
		<java.version>${javaVersion!}</java.version>
		<resource.delimiter>@</resource.delimiter>
		<jwt.version>0.9.1</jwt.version>
		<shiro.version>1.4.0</shiro.version>
		<mybatis.plus.version>3.3.2</mybatis.plus.version>
		<dubbo.version>${dubbo_version!}</dubbo.version>
		<swagger.version>2.9.2</swagger.version>
		<swagger.ui.version>1.9.6</swagger.ui.version>
<#if springboot_version?has_content>
		<spring-boot.version>${springboot_version!}</spring-boot.version>
</#if>
<#if springcloud_version?has_content>
		<spring-cloud-zookeeper.version>${spring_cloud_zookeeper_version!}</spring-cloud-zookeeper.version>
		<curator.version>${curator_version!}</curator.version>
		<spring-cloud-build.version>${springcloud_version!}</spring-cloud-build.version>
		<spring-cloud-alibaba.version>${springcloud_alibaba_version!}</spring-cloud-alibaba.version>
</#if>
	</properties>
 	<modules>
<#list modules as module>
 		<module>../${module!}</module>
</#list>
 	</modules>