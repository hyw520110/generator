<html xmlns="http://www.w3.org/1999/xhtml" xmlns:th="http://www.thymeleaf.org" xmlns:layout="http://www.ultraq.net.nz/web/thymeleaf/layout">
<head>
<title>添加${table.comment!}</title>
<link rel="stylesheet" href="/static/css/bootstrap.min.css" />
<link rel="stylesheet" href="/static/css/ace.min.css" class="ace-main-stylesheet" id="main-ace-style" />
<link rel="stylesheet" href="/static/css/style.css" />
</head>
<body>
	<form th:action="@{/${table.beanName!}/add}" method="POST" th:object="${bean!}">
	<table class="table  table-bordered table-hover">
		<#list table.fields as field>
		<#if !field.isPrimarykey()>
		<tr>
			<td class="jqgrow ui-row-ltr ui-widget-content" >
			<span ><#if field.comment?has_content>${field.comment!}<#else>${field.propertyName!}</#if></span>
			</td>
			<td>
				<input type="text" name="${field.propertyName!}"  />
				<span <#if field.isNotNullAble()>th:title="必输项" style="color: red"</#if>>*</span>
				<span th:if="<#noparse>${#fields.hasErrors('</#noparse>${field.propertyName!}<#noparse>')}</#noparse>" th:errors="*{${field.propertyName!}}">错误消息</span>
			</td>
		</tr>
		</#if>
		</#list>
		<tr><td colspan="2" align="center"><input class="btn btn-primary btn-sm" type="submit" value="提交"/></td></tr>
	</table>
	</form>
</body>
</html>
