package ${controllerPackage!};

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import java.util.List;
import java.util.Map;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.servlet.ModelAndView;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RequestParam;

<#list table.importPackages as pkg>
<#if pkg?has_content>
import ${pkg!};
</#if>
</#list>

<#if VUE??>
<#if global.modules?? && global.modules?size gt 1>
import ${api_dtoPackage!}.Result;
<#else>
import ${dtoPackage!}.Result;
</#if>
</#if>

import org.springframework.web.bind.annotation.RequestMapping;
import ${dtoPackage!}.${dtoName!};
import ${servicePackage!}.${serviceName!};
import ${entityPackage!}.${entityName!};

<#if superControllerClass?? && !superControllerClass?contains('.')>
import ${controllerPackage!}.commons.${superControllerClass!};
</#if>

<#include 'comments/comment.ftl'>

@Tag(name = "${table.comment!}")
<#if springboot_version??>
@org.springframework.web.bind.annotation.RestController
<#else>
@Controller
</#if>
@RequestMapping("/${table.beanName!}")
<#-- 复合主键的表不继承BaseController，因为BaseController不支持复合主键 -->
public class ${controllerName!} <#if superControllerClass?? && table.primarykeyFields?size lte 1>extends ${superControllerClass!}<${serviceName!},${entityName!}></#if> {

<#-- 判断是否需要重写方法：
   1. 主键不是 id
   2. 复合主键（多个主键字段）不重写，因为签名不兼容
-->
<#assign needOverride = false>
<#if table.primarykeyFields?size == 1>
<#list table.primarykeyFields as field>
<#if field.propertyName != "id">
<#assign needOverride = true>
</#if>
</#list>
</#if>

<#-- 如果需要重写方法 -->
<#if needOverride && "plus" == mapperType>
<#list table.primarykeyFields as field>
<#assign pkParamTypes = "">
<#assign pkParamNames = "">
<#list table.primarykeyFields as f>
<#assign pkParamTypes = pkParamTypes + f.fieldType.type>
<#assign pkParamNames = pkParamNames + f.propertyName>
<#if f?has_next>
<#assign pkParamTypes = pkParamTypes + ", ">
<#assign pkParamNames = pkParamNames + ", ">
</#if>
</#list>

	@GetMapping(value = "/<#list table.primarykeyFields as f>${f.propertyName}<#if f?has_next>/</#if></#list>")
	@Operation(summary = "根据<#list table.primarykeyFields as f>${f.comment!f.propertyName}<#if f?has_next>、</#if></#list>获取数据", description = "根据<#list table.primarykeyFields as f>${f.comment!f.propertyName}<#if f?has_next>、</#if></#list>获取数据")
	@ResponseBody
	public Result<${entityName!}> getInfo(<#list table.primarykeyFields as f>@PathVariable("${f.propertyName}") final ${f.fieldType.type} ${f.propertyName}<#if f?has_next>, </#if></#list>) {
	    return new Result<>((${entityName!}) bizService.getById(<#list table.primarykeyFields as f>${f.propertyName}<#if f?has_next>, </#if></#list>));
	}

	@DeleteMapping(value = "/<#list table.primarykeyFields as f>${f.propertyName}<#if f?has_next>/</#if></#list>")
	@Operation(summary = "根据<#list table.primarykeyFields as f>${f.comment!f.propertyName}<#if f?has_next>、</#if></#list>删除数据", description = "根据<#list table.primarykeyFields as f>${f.comment!f.propertyName}<#if f?has_next>、</#if></#list>删除数据")
	@ResponseBody
	public Result remove(<#list table.primarykeyFields as f>@PathVariable("${f.propertyName}") final ${f.fieldType.type} ${f.propertyName}<#if f?has_next>, </#if></#list>) {
	    bizService.removeById(<#list table.primarykeyFields as f>${f.propertyName}<#if f?has_next>, </#if></#list>);
	    return new Result<>();
	}
	<#break>
</#list>
</#if>
<#if "plus"!=mapperType>
<#assign sName = StringUtils.lowercaseFirst(serviceName)!>
	
    @Autowired
    private ${serviceName!} ${sName!};

<#if table.primarykeyFields?size gt 0>
	@GetMapping(value="/view/<#list table.primarykeyFields as field>${field.propertyName}<#if field?has_next>,</#if></#list>/{method}")
    public <#if THYMELEAF??>${dtoName!}<#else> ModelAndView </#if> getInfo(<#list table.primarykeyFields as field>@PathVariable(value = "${field.propertyName}") final ${field.fieldType.type} ${field.propertyName} <#if field?has_next>,</#if> </#list>,@PathVariable(value = "method")String method){
        ${dtoName!} bean= ${sName!}.getById(<#list table.primarykeyFields as field>${field.propertyName}<#if field?has_next>,</#if></#list>);
        return <#if THYMELEAF??>bean<#else> new ModelAndView("${table.beanName!}/"+method,"bean",bean)</#if> ;
    }
</#if>

</#if>
<#if "plus"!=mapperType>
	@Operation(summary = "${table.comment!}-分页列表查询", description = "${table.comment!}-分页列表查询")
	@GetMapping(value="/list")
    public <#if THYMELEAF??>Result<?><#else> ModelAndView </#if> list(HttpServletRequest req,@RequestParam Map<String, Object> map ,@RequestParam(required = false, defaultValue = "1") int pageNo,@RequestParam(required = false, defaultValue = "10") int pageRows, Model model){
        PageHelper.startPage(pageNo, pageRows);
        List<${dtoName!}> list=${sName!}.findAll(map);
<#if THYMELEAF??>
		return Result.ok(new PageInfo<>(list));
<#else>
        model.addAttribute("map",map);
        return new ModelAndView("${table.beanName!}/list","page",new PageInfo<${dtoName!}>(list));
</#if>
    }

<#if !THYMELEAF??>
	@Operation(summary = "${table.comment!}-添加", description = "${table.comment!}-添加")
    @GetMapping(value="/add")
    public ModelAndView toAdd(HttpServletRequest req,@ModelAttribute("bean") ${dtoName!} bean){
        return new ModelAndView("/${table.beanName!}/create");
    }
</#if>
    
	@ApiOperation(value = "${table.comment!}-添加", notes = "${table.comment!}-添加")
    @PostMapping(value="/add")
    public <#if THYMELEAF??>Result<?><#else> ModelAndView </#if> save(HttpServletRequest req,@Validated @ModelAttribute("bean") ${dtoName!} bean,BindingResult result){
<#if THYMELEAF??>
    	return Result.ok("添加成功!");
<#else>
		if(result.hasErrors()){
		    return new ModelAndView("/${table.beanName!}/create");
		}
        return new ModelAndView("redirect:/${table.beanName!}/list","flag",${sName!}.save(bean));
</#if>
    }
	
	@Operation(summary = "${table.comment!}-更新", description = "${table.comment!}-更新")
	@PostMapping(value="/update")
    public <#if THYMELEAF??>Result<?><#else> ModelAndView </#if> update(@Valid ${dtoName!} bean){
<#if THYMELEAF??>		
		return Result.ok("添加"+(${sName!}.update(bean)>0?"成功":"失败")+"!");
<#else>
    	return new ModelAndView("redirect:/${table.beanName!}/list","flag",${sName!}.update(bean)>0);
</#if>
    }
	
	/*
	 * 注意:数据更新操作一般必须是post请求
	 */
	@Operation(summary = "${table.comment!}-删除", description = "${table.comment!}-删除")
    @GetMapping(value="/del/<#list table.primarykeyFields as field>${field.propertyName}<#if field?has_next>,</#if></#list>")
    public <#if THYMELEAF??>Result<?><#else> ModelAndView </#if> delete(<#list table.primarykeyFields as field>@PathVariable(value = "${field.propertyName}") final ${field.fieldType.type} ${field.propertyName} <#if field?has_next>,</#if></#list>){
		${sName!}.deleteById(<#list table.primarykeyFields as field>${field.propertyName}<#if field?has_next>,</#if></#list>);
<#if THYMELEAF??>
		return Result.ok("添加"+(rows>0?"成功":"失败")+"!");
<#else>
		return new ModelAndView("redirect:/${table.beanName!}/list","flag",true);
</#if>
	}
</#if>
}
