#if(${StringUtils.indexOf("$superControllerClass", '.')}==-1)
package ${controllerPackage}.commons;

import java.io.Serializable;
import java.util.List;
#if($!{springboot_version})
import org.springframework.web.bind.annotation.RestController;
#if($mapperType == "plus")
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
#end
import org.springframework.stereotype.Controller;
#end
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import ${entityPackage}.BaseEntity;
#if($global.modules && $global.modules.size() > 1)
import ${api_servicePackage}.BaseService;
#else
import ${servicePackage}.BaseService;
#end
#if($VUE)
#if($global.modules && $global.modules.size() > 1)
import ${api_dtoPackage}.Result;
import ${api_dtoPackage}.PageResult;
#else
import ${dtoPackage}.Result;
import ${dtoPackage}.PageResult;
#end
#end
import io.swagger.v3.oas.annotations.Operation;
#set($comment="公共接口实现")
#parse('/templates/comments/comment.vm')
public class BaseController<BizService extends BaseService,Entity extends BaseEntity> {
#if($VUE)
    @Autowired
    protected BizService bizService;
    protected static final String AUTH_TYPE_WEB = "web";
	protected static final String AUTH_TYPE_APP = "app";

    @RequestMapping(value = "/add", method = RequestMethod.POST)
    @Operation(summary = "添加", description = "添加")
	@ResponseBody
	public Result<Entity> add(@RequestBody Entity entity) {
	    bizService.save(entity);
	    return new Result<>(entity);
	}

	@RequestMapping(value = "", method = RequestMethod.POST)
	@Operation(summary = "添加", description = "添加")
	@ResponseBody
	public Result<Entity> create(@RequestBody Entity entity) {
	    bizService.save(entity);
	    return new Result<>(entity);
	}

	@RequestMapping(value = "/{id}", method = RequestMethod.GET)
	@Operation(summary = "根据id获取数据", description = "根据id获取数据")
	@ResponseBody
	public Result<Entity> getInfo(@PathVariable("id") Serializable id) {
	    return new Result<>((Entity) bizService.getById(id));
	}

	@RequestMapping(value = "", method = RequestMethod.PUT)
	@Operation(summary = "根据id更新数据", description = "根据id更新数据")
	@ResponseBody
	public Result<Entity> update(@RequestBody Entity entity) {
	    bizService.updateById(entity);
	    return new Result<>(entity);
	}

	@RequestMapping(value = "/{id}", method = RequestMethod.DELETE)
	@Operation(summary = "根据id删除数据", description = "根据id删除数据")
	@ResponseBody
	public Result remove(@PathVariable("id") Serializable id) {
	    bizService.removeById(id);
	    return new Result<>();
	}

	@RequestMapping(value = "/batch", method = RequestMethod.DELETE)
	@Operation(summary = "批量删除", description = "批量删除")
	@ResponseBody
	public Result<?> removeBatch(@RequestBody List<Serializable> ids) {
	    bizService.removeByIds(ids);
	    return new Result<>();
	}

#if($mapperType == "plus")
	@RequestMapping(value = "/list", method = RequestMethod.GET)
	@Operation(summary = "获取列表", description = "获取列表")
	@ResponseBody
	public Result<List<Entity>> listAll(Entity entity) {
	    return new Result<>(bizService.list(new QueryWrapper(entity)));
	}

	@RequestMapping(value = "/page", method = RequestMethod.GET)
	@Operation(summary = "分页获取列表", description = "分页获取列表")
	@ResponseBody
	public Result<PageResult<Entity>> page(@RequestParam(name = "pageNum",defaultValue = "1",required = false)Integer pageNo,@RequestParam(name = "pageSize",defaultValue = "10",required = false) Integer pageSize, Entity entity) {
	    IPage<Entity> page = bizService.page(new Page(pageNo, pageSize),new QueryWrapper(entity));
	    return Result.ok(PageResult.of(page.getRecords(), page.getCurrent(), page.getSize(), page.getTotal()));
	}
#end
#end
}
#end
