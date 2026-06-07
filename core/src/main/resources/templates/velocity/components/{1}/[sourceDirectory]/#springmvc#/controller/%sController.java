package ${controllerPackage};

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import ${servletPackage}.http.HttpServletRequest;
import ${validationPackage}.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;

#foreach($pkg in $table.importPackages)
#if($pkg)
import ${pkg};
#end
#end

#if($VUE || $THYMELEAF)
#if($global.modules && $global.modules.size() > 1)
import ${api_dtoPackage}.PageResult;
import ${api_dtoPackage}.Result;
#else
import ${dtoPackage}.PageResult;
import ${dtoPackage}.Result;
#end
#end

import ${dtoPackage}.${dtoName};
import ${entityPackage}.${entityName};
import ${servicePackage}.${serviceName};
#if("plus" != "$mapperType" && $table.isCompositePrimaryKey())
import ${rootPackage}.key.${table.beanName}Key;
#end

#if($superControllerClass)
import #if($StringUtils.indexOf("$superControllerClass", ".") == -1)${controllerPackage}.commons.#end${superControllerClass};
#end

#parse('/templates/comments/comment.vm')

@Tag(name = "$!{table.comment}")
#if($!{springboot_version})
@org.springframework.web.bind.annotation.RestController
#else
@Controller
#end
@RequestMapping("/${table.beanName}")
public class ${controllerName} #if($superControllerClass && $table.primarykeyFields.size() <= 1)extends ${superControllerClass}<${serviceName},${entityName}>#end {

#set($needOverride = false)
#set($pkFields = $table.primarykeyFields)
#if($pkFields.size() == 1)
#foreach($field in $pkFields)
#if($field.propertyName != "id")
#set($needOverride = true)
#end
#end
#end

#if($needOverride && "plus" == "$mapperType")
	@GetMapping(value = "${table.primaryKeyPathPattern}")
	@Operation(summary = "$!{table.comment}-详情", description = "$!{table.comment}-详情")
	@ResponseBody
	public Result<${entityName}> getInfo(${table.primaryKeyMethodParameters}) {
	    return new Result<>((${entityName}) bizService.getById(${table.primaryKeyArgumentList}));
	}

	@DeleteMapping(value = "${table.primaryKeyPathPattern}")
	@Operation(summary = "$!{table.comment}-删除", description = "$!{table.comment}-删除")
	@ResponseBody
	public Result<?> remove(${table.primaryKeyMethodParameters}) {
	    bizService.removeById(${table.primaryKeyArgumentList});
	    return Result.ok();
	}
#end

#if("plus" != "$mapperType")
#set($sName = $StringUtils.lowercaseFirst($serviceName))

    @Autowired
    private ${serviceName} ${sName};

#if($VUE)
	@Operation(summary = "$!{table.comment}-分页列表查询", description = "$!{table.comment}-分页列表查询")
	@GetMapping(value="/page")
	public Result<PageResult<${dtoName}>> page(@RequestParam Map<String, Object> map,
			@RequestParam(required = false, defaultValue = "1") int pageNum,
			@RequestParam(required = false, defaultValue = "10") int pageSize) {
		PageHelper.startPage(pageNum, pageSize);
		List<${dtoName}> list = ${sName}.findAll(map);
		PageInfo<${dtoName}> page = new PageInfo<>(list);
		return Result.ok(PageResult.of(page.getList(), page.getPageNum(), page.getPageSize(), page.getTotal()));
	}

#if($table.primarykeyFields.size() > 0)
	@GetMapping(value="${table.primaryKeyPathPattern}")
	@Operation(summary = "$!{table.comment}-详情", description = "$!{table.comment}-详情")
	public Result<${dtoName}> detail(${table.primaryKeyMethodParameters}) {
#if($table.primarykeyFields.size() == 1)
		return Result.ok(${sName}.getById(${table.primaryKeyArgumentList}));
#else
		${table.beanName}Key key = new ${table.beanName}Key();
#foreach($field in $table.primarykeyFields)
		key.set${field.capitalName}(${field.propertyName});
#end
		return Result.ok(${sName}.getById(key));
#end
	}
#end

	@PostMapping(value="")
	@Operation(summary = "$!{table.comment}-添加", description = "$!{table.comment}-添加")
	public Result<?> create(@Valid @RequestBody ${dtoName} bean) {
		return Result.ok("添加成功", ${sName}.save(bean));
	}

	@PutMapping(value="")
	@Operation(summary = "$!{table.comment}-更新", description = "$!{table.comment}-更新")
	public Result<?> modify(@Valid @RequestBody ${dtoName} bean) {
		return Result.ok("更新" + (${sName}.update(bean) > 0 ? "成功" : "失败"));
	}

#if($table.primarykeyFields.size() > 0)
	@DeleteMapping(value="${table.primaryKeyPathPattern}")
	@Operation(summary = "$!{table.comment}-删除", description = "$!{table.comment}-删除")
	public Result<?> remove(${table.primaryKeyMethodParameters}) {
#if($table.primarykeyFields.size() == 1)
		${sName}.deleteById(${table.primaryKeyArgumentList});
#else
		${table.beanName}Key key = new ${table.beanName}Key();
#foreach($field in $table.primarykeyFields)
		key.set${field.capitalName}(${field.propertyName});
#end
		${sName}.deleteById(key);
#end
		return Result.ok();
	}
#end
#if($table.primarykeyFields.size() == 1)

	@DeleteMapping(value="/batch")
	@Operation(summary = "$!{table.comment}-批量删除", description = "$!{table.comment}-批量删除")
	public Result<?> removeBatch(@RequestBody List<${table.primaryKeyField.fieldType.type}> ids) {
		for (${table.primaryKeyField.fieldType.type} id : ids) {
			${sName}.deleteById(id);
		}
		return Result.ok();
	}
#end
#end

#if($table.primarykeyFields.size() > 0)
	@GetMapping(value="/view/#foreach($field in $table.primarykeyFields){${field.propertyName}}#if($foreach.hasNext),#end#end/{method}")
    public #if($THYMELEAF)${dtoName}#else ModelAndView #end getInfo(#foreach($field in $table.primarykeyFields)@PathVariable(value = "${field.propertyName}") final ${field.fieldType.type} ${field.propertyName} #if($foreach.hasNext),#end #end,@PathVariable(value = "method")String method){
        ${dtoName} bean = ${sName}.getById(#foreach($field in $table.primarykeyFields)${field.propertyName}#if($foreach.hasNext),#end#end);
        return #if($THYMELEAF)bean#else new ModelAndView("${table.beanName}/"+method,"bean",bean)#end;
    }
#end

	@Operation(summary = "$!{table.comment}-分页列表查询", description = "$!{table.comment}-分页列表查询")
	@GetMapping(value="/list")
    public #if($THYMELEAF)Result<?>#else ModelAndView #end list(HttpServletRequest req,@RequestParam Map<String, Object> map ,@RequestParam(required = false, defaultValue = "1") int pageNo,@RequestParam(required = false, defaultValue = "10") int pageRows, Model model){
        PageHelper.startPage(pageNo, pageRows);
        List<${dtoName}> list = ${sName}.findAll(map);
#if($THYMELEAF)
		PageInfo<${dtoName}> page = new PageInfo<>(list);
		return Result.ok(PageResult.of(page.getList(), page.getPageNum(), page.getPageSize(), page.getTotal()));
#else
        model.addAttribute("map", map);
        return new ModelAndView("${table.beanName}/list", "page", new PageInfo<${dtoName}>(list));
#end
    }

#if(!$THYMELEAF)
	@Operation(summary = "$!{table.comment}-添加", description = "$!{table.comment}-添加")
    @GetMapping(value="/add")
    public ModelAndView toAdd(HttpServletRequest req,@ModelAttribute("bean") ${dtoName} bean){
        return new ModelAndView("/${table.beanName}/create");
    }
#end

	@Operation(summary = "$!{table.comment}-添加", description = "$!{table.comment}-添加")
    @PostMapping(value="/add")
    public #if($THYMELEAF)Result<?>#else ModelAndView #end save(HttpServletRequest req,@Validated @ModelAttribute("bean") ${dtoName} bean,BindingResult result){
#if($THYMELEAF)
    	return Result.ok("添加成功!");
#else
		if (result.hasErrors()) {
		    return new ModelAndView("/${table.beanName}/create");
		}
        return new ModelAndView("redirect:/${table.beanName}/list", "flag", ${sName}.save(bean));
#end
    }

	@Operation(summary = "$!{table.comment}-更新", description = "$!{table.comment}-更新")
	@PostMapping(value="/update")
    public #if($THYMELEAF)Result<?>#else ModelAndView #end update(@Valid ${dtoName} bean){
#if($THYMELEAF)
		return Result.ok("更新" + (${sName}.update(bean) > 0 ? "成功" : "失败"));
#else
		return new ModelAndView("redirect:/${table.beanName}/list", "flag", ${sName}.update(bean) > 0);
#end
    }

	@Operation(summary = "$!{table.comment}-删除", description = "$!{table.comment}-删除")
    @GetMapping(value="/del/#foreach($field in $table.primarykeyFields){${field.propertyName}}#if($foreach.hasNext),#end#end")
    public #if($THYMELEAF)Result<?>#else ModelAndView #end delete(#foreach($field in $table.primarykeyFields)@PathVariable(value = "${field.propertyName}") final ${field.fieldType.type} ${field.propertyName} #if($foreach.hasNext),#end#end){
		${sName}.deleteById(#foreach($field in $table.primarykeyFields)${field.propertyName}#if($foreach.hasNext),#end#end);
#if($THYMELEAF)
		return Result.ok();
#else
		return new ModelAndView("redirect:/${table.beanName}/list", "flag", true);
#end
	}
#end
}
