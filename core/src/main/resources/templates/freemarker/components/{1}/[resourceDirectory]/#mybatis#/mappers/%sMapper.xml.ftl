<#if mapperType=="xml"  ||  mapperType?? && mapperType == "plus">
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN" "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="${mapperPackage!}.${mapperName!}">
<#if enableCache?has_content>
	<!-- 开启二级缓存 cache二選一，區別在於是否記錄日誌-->
	<cache type="org.mybatis.caches.ehcache.LoggingEhcache"/>
	<!-- <cache type="org.mybatis.caches.ehcache.EhcacheCache"/> -->  
</#if>

<#if baseResultMap?has_content>
	<resultMap id="BaseResultMap" type="${entityPackage!}.${entityName!}">
<#list table.fields as field>
	<result column="${field.name!}" property="${field.propertyName!}" jdbcType="${field.jdbcType!}"/>
</#list>
	</resultMap>
	
</#if>

<#if columns?has_content>
    <sql id="columns">
      <![CDATA[
        ${table.fieldNames!}
      ]]>  
    </sql>
</#if>

<#if findById?has_content>
	<select id="findById" resultMap="BaseResultMap" >
	    SELECT <#if columns?has_content> <include refid="columns"/> <#else>  ${table.fieldNames!} </#if> from ${table.name!} WHERE <#list table.primarykeyFields as field> ${field.name!} = <#assign paramPlaceholder = "#{" + field.propertyName + ",jdbcType=" + field.jdbcType + "}">${paramPlaceholder} <#if field?has_next> and </#if> </#list> 
	</select>	
</#if>
<#if "plus"!=mapperType>

<#if findOne?has_content>
	<select id="findOne" resultMap="BaseResultMap" parameterType="java.util.Map">
	    SELECT <#if columns?has_content> <include refid="columns"/> <#else>  ${table.fieldNames!} </#if> FROM ${table.name!} where 1=1 <include refid="queryWhere"/> 
	</select>	
</#if>
<#if count?has_content>	
	<select id="count" resultType="java.lang.Integer" parameterType="java.util.Map">
    	select count(1) from ${table.name!} where 1=1 <include refid="queryWhere"/>
	</select>
</#if>	
<#if findPage?has_content>	
	<select id="findPage" resultMap="BaseResultMap" parameterType="java.util.Map">
		select <include refid="columns"/>  from ${table.name!} where 1=1 <include refid="queryWhere"/> 
	    <![CDATA[ ORDER BY <#list table.primarykeyFields as field> ${field.name!} <#if field?has_next> and </#if> </#list>  DESC ]]>
        <![CDATA[ LIMIT #{startRecord},#{endRecord} ]]>
	</select>
</#if>	
<#if findAll?has_content>
    <select id="findAll" resultMap="BaseResultMap" parameterType="java.util.Map">
		select	* from ${table.name!} where 1=1 <include refid="queryWhere"/>  order by <#list table.primarykeyFields as field> ${field.name!}<#if field?has_next> , </#if></#list> desc
	</select>
</#if>
</#if>	
<#if insert?has_content>
	<!-- TODO 	1. 返回复合主键 	2. 主键处理 -->
	<!-- 返回主键：
	1. insert节点中添加useGeneratedKey和keyProperty属性
	2. insert节点下添加selectKey子节点的方式
	 -->
	<insert id="insert" parameterType="${entityPackage!}.${entityName!}" useGeneratedKeys="true" keyProperty="${table.primaryKeyField.propertyName!}">
		<!-- 
		<selectKey resultType="<#if table.primaryKeyField.fieldType.claz??>${table.primaryKeyField.fieldType.claz!}<#else> java.lang.${table.primaryKeyField.fieldType.type!}</#if>" order="AFTER" keyProperty="${table.primaryKeyField.propertyName!}">
			SELECT LAST_INSERT_ID()  
		</selectKey>
		 <selectKey type="pre" resultClass="<#if table.primaryKeyField.fieldType.claz??>${table.primaryKeyField.fieldType.claz!}<#else> java.lang.${table.primaryKeyField.fieldType.type!}</#if>" order="AFTER" keyProperty="${table.primaryKeyField.propertyName!}">
		   SELECT @@IDENTITY AS id
		   </selectKey>
		 -->
		<![CDATA[
		INSERT INTO ${table.name!} (<#list table.fields as field><#if field.isIdentity()?has_content>${field.name!} <#if field?has_next>,</#if></#if></#list>
		) VALUES (<#list table.fields as field><#if field.isIdentity()?has_content><#assign paramPlaceholder = "#{" + field.propertyName + ",jdbcType=" + field.jdbcType + "}">${paramPlaceholder}<#if field?has_next>,</#if></#if></#list>)
		]]>
	</insert>
</#if>

<#if "plus"!=mapperType>
<#if update?has_content>

	<update id="update" parameterType="${entityPackage!}.${entityName!}">
		UPDATE ${table.name!} 
		<set>
<#list table.fields as field>
<#if field.isPrimarykey()?has_content>	
			<if test="${field.propertyName!} != null">
				<#assign paramPlaceholder = "#{" + field.propertyName + ",jdbcType=" + field.jdbcType + "}">${field.name!} = ${paramPlaceholder} <#if field?has_next>,</#if>
				
			</if>
</#if>	
</#list>
		</set>
		WHERE <#list table.primarykeyFields as field> <#assign paramPlaceholder = "#{" + field.propertyName + ",jdbcType=" + field.jdbcType + "}">${field.name!} = ${paramPlaceholder} <#if field?has_next> and </#if> </#list>
		     
	</update>
</#if>
</#if>

<#if deleteById?has_content>

	<delete id="deleteById"  >
		<![CDATA[DELETE FROM ${table.name!}  WHERE <#list table.primarykeyFields as field> <#assign paramPlaceholder = "#{" + field.propertyName + ",jdbcType=" + field.jdbcType + "}">${field.name!} = ${paramPlaceholder} <#if field?has_next> and </#if> </#list>]]>
	</delete>
</#if>
	<sql id="queryWhere">
<#list table.fields as field>
		<if test="${field.name!} != null and ${field.name!} != ''">
			and ${field.name!} = <#assign paramPlaceholder = "#{" + field.propertyName + "}">${paramPlaceholder} 
		</if>
</#list>
	</sql>	
</mapper>
</#if>