#if(${StringUtils.indexOf("$superControllerClass", '.')}==-1)
package ${controllerPackage}.commons;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import org.springframework.ui.Model;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
#if($!{springboot_version})
import org.springframework.web.bind.annotation.RestController;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

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
import ${servicePackage}.BaseService;
import ${voPackage}.Result;
import io.swagger.annotations.ApiOperation;

#parse('/templates/commons/comment.vm')
public class BaseController<BizService extends BaseService,Entity extends BaseEntity> {
	
    @Autowired
    protected BizService bizService;
	
    @RequestMapping(value = "/add", method = RequestMethod.POST)
    @ApiOperation(value = "添加", notes = "添加")
	@ResponseBody
	public Result add(Entity entity) {
	    bizService.save(entity);
	    return new Result();
	}
	
	@RequestMapping(value = "/{id}", method = RequestMethod.GET)
	@ApiOperation(value = "根据id获取数据", notes = "根据id获取数据")
	@ResponseBody
	public Result<Entity> getInfo(@PathVariable Serializable id) {
	    return new Result<>((Entity) bizService.getById(id));
	}
	
	@RequestMapping(value = "/update", method = RequestMethod.PUT)
	@ApiOperation(value = "根据id更新数据", notes = "根据id更新数据")
	@ResponseBody
	public Result update(Entity entity) {
	    bizService.updateById(entity);
	    return new Result<>();
	}
	
	@RequestMapping(value = "/{id}", method = RequestMethod.DELETE)
	@ApiOperation(value = "根据id删除数据", notes = "根据id删除数据")
	@ResponseBody
	public Result remove(@PathVariable Serializable id) {
	    bizService.removeById(id);
	    return new Result<>();
	}
	
	@RequestMapping(value = "/list", method = RequestMethod.GET)
	@ApiOperation(value = "获取列表", notes = "获取列表")
	@ResponseBody
	public Result<List<Entity>> listAll(Entity entity) {
	    return new Result<>(bizService.list(new QueryWrapper(entity)));
	}
	
	@RequestMapping(value = "/page", method = RequestMethod.GET)
	@ApiOperation(value = "分页获取列表", notes = "分页获取列表")
	@ResponseBody
	public Result<IPage<Entity>> page(@RequestParam(name = "pageNo",defaultValue = "50",required = false)Integer pageNo,@RequestParam(name = "pageSize",defaultValue = "1",required = false) Integer pageSize, Entity entity) {
	    IPage<Entity> page = bizService.page(new Page(pageNo, pageSize),new QueryWrapper(entity));
	    return new Result<>(page);
	}    
}
#end