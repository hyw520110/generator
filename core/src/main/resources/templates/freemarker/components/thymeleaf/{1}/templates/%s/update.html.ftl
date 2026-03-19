<!DOCTYPE html>
<html xmlns="http://www.w3.org/1999/xhtml" xmlns:th="http://www.thymeleaf.org" xmlns:layout="http://www.ultraq.net.nz/web/thymeleaf/layout">
<head>
<title>修改${table.comment!}</title>
<link rel="stylesheet" href="/static/css/bootstrap.min.css" />
<link rel="stylesheet" href="/static/css/ace.min.css" class="ace-main-stylesheet" id="main-ace-style" />
<link rel="stylesheet" href="/static/css/style.css" />
</head>
<body>
	<form th:action="@{/${table.beanName!}/update}" method="POST">
	<table class="table  table-bordered table-hover">
<#list table.fields as field>
<#if field.isPrimarykey()>
			<input type="hidden" name="${field.propertyName!}" th:value="*{bean.${field.propertyName!}}" />
<#else>
		<tr>
			<td class="jqgrow ui-row-ltr ui-widget-content"  ><#if field.comment?has_content?has_content>${field.propertyName!}<#else>${field.comment!}</#if></td>
			<td>
				<input type="text" name="${field.propertyName!}" th:value="*{bean.${field.propertyName!}}"/>
			</td>
		</tr>
</#if>
</#list>
		<tr><td colspan="2" align="center"><input class="btn btn-primary btn-sm" type="submit" value="提交"/></td></tr>
	</table>
	</form>
</body>
</html>
