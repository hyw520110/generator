package ${controllerPackage!};

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
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
import org.springframework.web.bind.annotation.RequestParam;

<#list table.importPackages as pkg>
<#if pkg?has_content>
import ${pkg!};
</#if>
</#list>

<#if VUE??>
import ${dtoPackage!}.Result;
</#if>

import org.springframework.web.bind.annotation.RequestMapping;
import ${dtoPackage!}.${dtoName!};
import ${servicePackage!}.${serviceName!};

<#if superControllerClass?? && !superControllerClass?contains('.')>
import ${controllerPackage!}.commons.${superControllerClass!};
</#if>

<#include 'comments/comment.ftl'>

@Api(value = "${table.comment!}")
<#if springboot_version??>
@org.springframework.web.bind.annotation.RestController
<#else>
@Controller
</#if>
@RequestMapping("/${table.beanName!}")
public class ${controllerName!} <#if superControllerClass??>extends ${superControllerClass!}<${serviceName!},${dtoName!}></#if> {
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
	@ApiOperation(value = "${table.comment!}-分页列表查询", notes = "${table.comment!}-分页列表查询")
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
	@ApiOperation(value = "${table.comment!}-添加", notes = "${table.comment!}-添加")
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
	
	@ApiOperation(value = "${table.comment!}-更新", notes = "${table.comment!}-更新")
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
	@ApiOperation(value = "${table.comment!}-删除", notes = "${table.comment!}-删除")
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
