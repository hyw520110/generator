#if("plus"!="$mapperType")
#if(${StringUtils.indexOf("$superMapperClass", '.')}==-1)
package ${mapperPackage};

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Param;

#parse('/templates/comments/comment.vm')
public interface BaseMapper<E> {

	E selectById(Long id);
	
	E query(E rdObject);
	
	Long queryExist(E rdObject);
	
    /**
     * 根据指定条件查询一条记录 
     * @author:  heyiwu
     * @param map
     * @return
     */
    E findOne(Map<String, Object> map);
    /**
     * 查询全部
     * @return List<E> 返回数据集合   
     */
    List<E> findAll(Map<String, Object> map);


    /**
    * 分页查询列表，使用此方法的dto需要继承pageList
    * @return List<E> 返回数据集合   
    */
    List<E> findPage(Map<String, Object> map);
    
    /**
     * 分页查询--总条数
     * @return Integer 总条数   
     */
     Integer count(Map<String, Object> map);
     
    
    /**
     * 保存对象信息
     * @param entity 对象实体类
      * @return 
     *     
     */
    Integer insert(E entity);

    /**
    * 更新对象信息 
    * @param entity 实体类
    *     
    */
    Integer update(E entity);
    
    void remove(Long id);
    
}
#end
#end