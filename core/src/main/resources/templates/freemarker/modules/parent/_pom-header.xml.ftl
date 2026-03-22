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
<#if JWT??>
		<jwt.version>${jwt_version!'0.9.1'}</jwt.version>
</#if>
<#if SHIRO??>
		<shiro.version>${shiro_version!'2.0.2'}</shiro.version>
</#if>
<#if MYBATIS??>
		<mybatis-spring-boot.version>${mybatis_spring_boot_version!'3.0.3'}</mybatis-spring-boot.version>
		<pagehelper.version>${pagehelper_version!'2.1.0'}</pagehelper.version>
		<druid.version>${druid_version!'1.2.23'}</druid.version>
		<mysql-connector.version>${mysql_connector_version!'8.0.33'}</mysql-connector.version>
	<#if mapperType?? && mapperType == "plus">
		<mybatis.plus.version>${mybatis_plus_version!'3.5.8'}</mybatis.plus.version>
	</#if>
</#if>
<#if DUBBO?? && DUBBO>
		<dubbo.version>${dubbo_version!}</dubbo.version>
</#if>
<#if SWAGGER2??>
		<swagger.version>${swagger_version!'2.9.2'}</swagger.version>
		<swagger.ui.version>${swagger_ui_version!'1.9.6'}</swagger.ui.version>
</#if>
<#if SPRINGBOOT??>
		<lombok.version>${lombok_version!'1.18.20'}</lombok.version>
		<disruptor.version>${disruptor_version!'3.4.2'}</disruptor.version>
</#if>
<#if springboot_version?has_content>
		<spring-boot.version>${springboot_version!}</spring-boot.version>
</#if>
<#if springcloud_version?has_content>
		<spring-cloud.version>${springcloud_version!}</spring-cloud.version>
		<spring-cloud-alibaba.version>${springcloud_alibaba_version!}</spring-cloud-alibaba.version>
</#if>
<#if ZOOKEEPER?? && ZOOKEEPER>
		<spring-cloud-zookeeper.version>${spring_cloud_zookeeper_version!}</spring-cloud-zookeeper.version>
		<curator.version>${curator_version!}</curator.version>
</#if>
<#if sentinel_version?has_content>
		<sentinel.version>${sentinel_version!}</sentinel.version>
</#if>
	</properties>
 	<modules>
<#list modules as module>
 		<module>../${module!}</module>
</#list>
 	</modules>