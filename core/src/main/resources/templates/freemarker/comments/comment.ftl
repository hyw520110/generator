<#--  ============================================ -->
<#--  文件头注释模板 -->
<#--  使用方式：<#include "comments/comment.ftl"> -->
<#--  ============================================ -->

/**
<#if comment?? && comment?has_content>
 * ${comment!}
<#elseif table??>
 * ${(table.comment!'')!}
<#else>
 * ${comment!}
</#if>
 *
 * @author ${author!}
 * @copyright ${copyright!}
 */