package ${controllerPackage};

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import java.util.List;
import java.util.Map;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import org.springframework.web.servlet.ModelAndView;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

#foreach($pkg in ${table.importPackages})
#if(${StringUtils.show("$pkg")})
import ${pkg};
#end
#end

#if($VUE)
import ${voPackage}.Result;
#end

import org.springframework.web.bind.annotation.RequestMapping;
import ${entityPackage}.${entityName};
import ${servicePackage}.${entityName}Service;

#if(${superControllerClass})
import #if(${StringUtils.indexOf("$superControllerClass", '.')}==-1)${controllerPackage}.commons.#end${superControllerClass};
#end

#parse('/templates/comments/comment.vm')

@Api(value = "$!{table.comment}")
#if($!{springboot_version})
@org.springframework.web.bind.annotation.RestController
#else
@Controller
#end
@RequestMapping("/${table.beanName}")
public class ${className} #if(${superControllerClass})extends ${superControllerClass}<${entityName}Service,${entityName}>#end {
#if("plus"!="$mapperType")
#set($sName=${StringUtils.lowercaseFirst($serviceName)})
#set($eName=${StringUtils.capitalFirst("$entityName")})
	
    @Autowired
    private ${serviceName} ${sName};

#if(${table.getPrimarykeyFields().size()}>0)
	@GetMapping(value="/view/#foreach($field in ${table.primarykeyFields}){${field.propertyName}}#if($foreach.count!=${table.primarykeyFields.size()}),#end#end/{method}")
    public #if(!$THYMELEAF)${StringUtils.capitalFirst("$entityName")}#else ModelAndView #end getInfo(#foreach($field in ${table.primarykeyFields})@PathVariable(value = "${field.propertyName}") final ${field.fieldType.type} ${field.propertyName} #if($foreach.count!=${table.primarykeyFields.size()}),#end #end,@PathVariable(value = "method")String method){
        ${StringUtils.capitalFirst("$entityName")} bean= ${sName}.getById(#foreach($field in ${table.primarykeyFields})${field.propertyName}#if($foreach.count!=${table.primarykeyFields.size()}),#end#end);
        return #if(!$THYMELEAF)bean#else new ModelAndView("${table.beanName}/"+method,"bean",bean)#end ;
    }
#end

#end
#if("plus"!="$mapperType")
	@ApiOperation(value = "$!{table.comment}-分页列表查询", notes = "$!{table.comment}-分页列表查询")
	@GetMapping(value="/list")
    public #if(!$THYMELEAF)Result<?>#else ModelAndView #end list(HttpServletRequest req,@RequestParam Map<String, Object> map ,@RequestParam(required = false, defaultValue = "1") int pageNo,@RequestParam(required = false, defaultValue = "10") int pageRows, Model model){
        PageHelper.startPage(pageNo, pageRows);
        List<$eName> list=${sName}.findAll(map);
#if(!$THYMELEAF)
		return Result.ok(new PageInfo<>(list));
#else
        model.addAttribute("map",map);
        return new ModelAndView("${table.beanName}/list","page",new PageInfo<$eName>(list));
#end
    }

#if(!!$THYMELEAF)
	@ApiOperation(value = "$!{table.comment}-添加", notes = "$!{table.comment}-添加")
    @GetMapping(value="/add")
    public ModelAndView toAdd(HttpServletRequest req,@ModelAttribute("bean") $eName bean){
        return new ModelAndView("/${table.beanName}/create");
    }
#end
    
	@ApiOperation(value = "$!{table.comment}-添加", notes = "$!{table.comment}-添加")
    @PostMapping(value="/add")
    public #if(!$THYMELEAF)Result<?>#else ModelAndView #end save(HttpServletRequest req,@Validated @ModelAttribute("bean") $eName bean,BindingResult result){
#if(!$THYMELEAF)
    	return Result.ok("添加成功!");
#else
		if(result.hasErrors()){
		    return new ModelAndView("/${table.beanName}/create");
		}
        return new ModelAndView("redirect:/${table.beanName}/list","flag",${sName}.save(bean));
#end
    }
	
	@ApiOperation(value = "$!{table.comment}-更新", notes = "$!{table.comment}-更新")
	@PostMapping(value="/update")
    public #if(!$THYMELEAF)Result<?>#else ModelAndView #end update(@Valid $eName bean){
#if(!$THYMELEAF)		
		return Result.ok("添加"+(${sName}.update(bean)>0?"成功":"失败")+"!");
#else
    	return new ModelAndView("redirect:/${table.beanName}/list","flag",${sName}.update(bean)>0);
#end
    }
	
	/*
	 * 注意:数据更新操作一般必须是post请求
	 */
	@ApiOperation(value = "$!{table.comment}-删除", notes = "$!{table.comment}-删除")
    @GetMapping(value="/del/#foreach($field in ${table.primarykeyFields}){${field.propertyName}}#if($foreach.count!=${table.primarykeyFields.size()}),#end#end")
    public #if(!$THYMELEAF)Result<?>#else ModelAndView #end delete(#foreach($field in ${table.primarykeyFields})@PathVariable(value = "${field.propertyName}") final ${field.fieldType.type} ${field.propertyName} #if($foreach.count!=${table.primarykeyFields.size()}),#end#end){
		${sName}.deleteById(#foreach($field in ${table.primarykeyFields})${field.propertyName}#if($foreach.count!=${table.primarykeyFields.size()}),#end#end);
#if(!$THYMELEAF)
		return Result.ok("添加"+(rows>0?"成功":"失败")+"!");
#else
		return new ModelAndView("redirect:/${table.beanName}/list","flag",true);
#end
	}
#end
}
