<#--  ============================================ -->
<#--  文件头注释模板 -->
<#--  使用方式：<#include "comments/comment.ftl"> -->
<#--  ============================================ -->

/**
<#if table??>
 * ${(table.comment!'')!}
<#else>
 * ${comment!}
</#if>
 *
 * @author ${author!}
 * @copyright ${copyright!}
 */