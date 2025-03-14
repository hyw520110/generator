#if(${StringUtils.indexOf("$superServiceClass",'.')}==-1)
package ${servicePackage};

import java.util.List;
import java.util.Map;
#if("plus" == "$mapperType")
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.toolkit.SqlHelper;
#end

#set($comment="公共接口")
#parse('/templates/comments/comment.vm')
#if("plus"=="$mapperType")
public interface BaseService<T> extends com.baomidou.mybatisplus.extension.service.IService<T>{
#else
public interface BaseService<T> {
	/**
	 * <p>
	 * 根据 ID 查询
	 * </p>
	 *
	 * @param id 主键ID
	 */
	T getById(PK id);

	/**
	 * 根据指定条件查询一条记录
	 * 
	 * @author: heyiwu
	 * @param map
	 * @return
	 */
	T findOne(Map<String, Object> map);

	/**
	 * 查询全部
	 * 
	 * @return List<E> 返回数据集合
	 */
	List<T> findAll(Map<String, Object> map);

	/**
	 * 分页查询--总条数
	 * 
	 * @return Integer 总条数
	 */
	Integer count(Map<String, Object> map);

	/**
	 * 分页查询列表，使用此方法的dto需要继承pageList
	 * 
	 * @return List<E> 返回数据集合
	 */
	List<T> findPage(Map<String, Object> map);
	
	/**
	 * 更新对象信息
	 * 
	 * @param entity 实体类
	 * 
	 */
	Integer update(T entity);
	
	/**
	 * <p>
	 * 插入一条记录（选择字段，策略插入）
	 * </p>
	 *
	 * @param entity 实体对象
	 * @return boolean
	 */
	boolean save(T entity);
	
	void deleteById(PK Id);
#end

}#end
