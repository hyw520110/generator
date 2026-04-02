package ${controllerPackage};

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

#foreach($pkg in ${table.importPackages})
#if(${StringUtils.show("$pkg")})
import ${pkg};
#end
#end

#if($VUE)
import ${dtoPackage}.Result;
#end

import org.springframework.web.bind.annotation.RequestMapping;
import ${dtoPackage}.${dtoName};
import ${servicePackage}.${serviceName};
import ${entityPackage}.${entityName};

#if(${superControllerClass})
import #if(${StringUtils.indexOf("$superControllerClass", '.')}==-1)${controllerPackage}.commons.#end${superControllerClass};
#end

#parse('/templates/comments/comment.vm')

@Tag(name = "$!{table.comment}")
#if($!{springboot_version})
@org.springframework.web.bind.annotation.RestController
#else
@Controller
#end
@RequestMapping("/${table.beanName}")
#* 复合主键的表不继承BaseController，因为BaseController不支持复合主键 *#
public class ${controllerName} #if(${superControllerClass} && $pkFields.size() <= 1)extends ${superControllerClass}<${serviceName},${entityName}>#end {

#* 判断是否需要重写方法：
   1. 主键不是 id
   2. 复合主键（多个主键字段）不重写，因为签名不兼容
*#
#set($needOverride = false)
#set($pkFields = ${table.primarykeyFields})
#if($pkFields.size() == 1)
#foreach($field in $pkFields)
#if($field.propertyName != "id")
#set($needOverride = true)
#end
#end
#end

#* 如果需要重写方法 *#
#if($needOverride && "plus" == "$mapperType")
	#foreach($field in $pkFields)
#set($pkParamTypes = "")
#set($pkParamNames = "")
#foreach($f in $pkFields)
#set($pkParamTypes = "$pkParamTypes${f.fieldType.type}")
#set($pkParamNames = "$pkParamNames${f.propertyName}")
#if($foreach.hasNext)
#set($pkParamTypes = "$pkParamTypes, ")
#set($pkParamNames = "$pkParamNames, ")
#end
#end

	@GetMapping(value = "/#foreach($f in $pkFields){$f.propertyName}#if($foreach.hasNext)/#end#end")
	@Operation(summary = "根据#foreach($f in $pkFields)${f.comment?default($f.propertyName)}#if($foreach.hasNext)、#end#end获取数据", description = "根据#foreach($f in $pkFields)${f.comment?default($f.propertyName)}#if($foreach.hasNext)、#end#end获取数据")
	@ResponseBody
	public Result<${entityName}> getInfo(#foreach($f in $pkFields)@PathVariable("$f.propertyName") final ${f.fieldType.type} $f.propertyName#if($foreach.hasNext), #end#end) {
	    return new Result<>((${entityName}) bizService.getById(#foreach($f in $pkFields)$f.propertyName#if($foreach.hasNext), #end#end));
	}

	@DeleteMapping(value = "/#foreach($f in $pkFields){$f.propertyName}#if($foreach.hasNext)/#end#end")
	@Operation(summary = "根据#foreach($f in $pkFields)${f.comment?default($f.propertyName)}#if($foreach.hasNext)、#end#end删除数据", description = "根据#foreach($f in $pkFields)${f.comment?default($f.propertyName)}#if($foreach.hasNext)、#end#end删除数据")
	@ResponseBody
	public Result remove(#foreach($f in $pkFields)@PathVariable("$f.propertyName") final ${f.fieldType.type} $f.propertyName#if($foreach.hasNext), #end#end) {
	    bizService.removeById(#foreach($f in $pkFields)$f.propertyName#if($foreach.hasNext), #end#end);
	    return new Result<>();
	}
	#break
#end
#end
#if("plus"!="$mapperType")
#set($sName=${StringUtils.lowercaseFirst($serviceName)})
	
    @Autowired
    private ${serviceName} ${sName};

#if(${table.getPrimarykeyFields().size()}>0)
	@GetMapping(value="/view/#foreach($field in ${table.primarykeyFields}){${field.propertyName}}#if($foreach.count!=${table.primarykeyFields.size()}),#end#end/{method}")
    public #if(!$THYMELEAF)${dtoName}#else ModelAndView #end getInfo(#foreach($field in ${table.primarykeyFields})@PathVariable(value = "${field.propertyName}") final ${field.fieldType.type} ${field.propertyName} #if($foreach.count!=${table.primarykeyFields.size()}),#end #end,@PathVariable(value = "method")String method){
        ${dtoName} bean= ${sName}.getById(#foreach($field in ${table.primarykeyFields})${field.propertyName}#if($foreach.count!=${table.primarykeyFields.size()}),#end#end);
        return #if(!$THYMELEAF)bean#else new ModelAndView("${table.beanName}/"+method,"bean",bean)#end ;
    }
#end

#end
#if("plus"!="$mapperType")
	@Operation(summary = "$!{table.comment}-分页列表查询", description = "$!{table.comment}-分页列表查询")
	@GetMapping(value="/list")
    public #if(!$THYMELEAF)Result<?>#else ModelAndView #end list(HttpServletRequest req,@RequestParam Map<String, Object> map ,@RequestParam(required = false, defaultValue = "1") int pageNo,@RequestParam(required = false, defaultValue = "10") int pageRows, Model model){
        PageHelper.startPage(pageNo, pageRows);
        List<${dtoName}> list=${sName}.findAll(map);
#if(!$THYMELEAF)
		return Result.ok(new PageInfo<>(list));
#else
        model.addAttribute("map",map);
        return new ModelAndView("${table.beanName}/list","page",new PageInfo<${dtoName}>(list));
#end
    }

#if(!!$THYMELEAF)
	@Operation(summary = "$!{table.comment}-添加", description = "$!{table.comment}-添加")
    @GetMapping(value="/add")
    public ModelAndView toAdd(HttpServletRequest req,@ModelAttribute("bean") ${dtoName} bean){
        return new ModelAndView("/${table.beanName}/create");
    }
#end
    
	@Operation(summary = "$!{table.comment}-添加", description = "$!{table.comment}-添加")
    @PostMapping(value="/add")
    public #if(!$THYMELEAF)Result<?>#else ModelAndView #end save(HttpServletRequest req,@Validated @ModelAttribute("bean") ${dtoName} bean,BindingResult result){
#if(!$THYMELEAF)
    	return Result.ok("添加成功!");
#else
		if(result.hasErrors()){
		    return new ModelAndView("/${table.beanName}/create");
		}
        return new ModelAndView("redirect:/${table.beanName}/list","flag",${sName}.save(bean));
#end
    }
        
    	@Operation(summary = "$!{table.comment}-更新", description = "$!{table.comment}-更新")
    	@PostMapping(value="/update")    public #if(!$THYMELEAF)Result<?>#else ModelAndView #end update(@Valid ${dtoName} bean){
#if(!$THYMELEAF)		
		return Result.ok("添加"+(${sName}.update(bean)>0?"成功":"失败")+"!");
#else
    	return new ModelAndView("redirect:/${table.beanName}/list","flag",${sName}.update(bean)>0);
#end
    }
	
	/*
	 * 注意:数据更新操作一般必须是post请求
	 */
	@Operation(summary = "$!{table.comment}-删除", description = "$!{table.comment}-删除")
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