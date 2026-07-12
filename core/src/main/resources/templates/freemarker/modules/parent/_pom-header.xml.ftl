<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
	xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
	<modelVersion>4.0.0</modelVersion>
	<groupId>${rootPackage!}</groupId>
	<artifactId>${projectName!}-parent</artifactId>
	<version>${version!}</version>
	<packaging>pom</packaging>
	<properties>
		<#include "_pom-properties.xml.ftl">
	</properties>
 	<modules>
<#list modules as module>
 		<module>../${module!}</module>
</#list>
 	</modules>
