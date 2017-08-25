#if(${StringUtils.indexOf("$superServiceClass", '.')}==-1)
package ${servicePackage};

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * @author ${author}
 * @since ${date}
 * @copyright: ${copyright}
 */
public interface BaseService<T, PK extends Serializable> {

    /**
    * 根据id获取对象
    * @param id 对象id
    * @return Object  返回对象  
    */
    T findById(PK id);

    /**
     * 根据指定条件查询一条记录 
     * @author:  heyiwu 
     * @param map
     * @return
     */
    T findOne(Map<String, Object> map);
    /**
    * 查询全部
    * @return List<E> 返回数据集合   
    */
    List<T> findAll(Map<String, Object> map);

    /**
    * 分页查询--总条数
    * @return Integer 总条数   
    */
    Integer count(Map<String, Object> map);

    /**
    * 分页查询列表，使用此方法的dto需要继承pageList
    * @return List<E> 返回数据集合   
    */
    List<T> findPage(Map<String, Object> map);
    /**
     * <p>
     * 插入一条记录（选择字段，策略插入）
     * </p>
     *
     * @param entity
     *            实体对象
     * @return boolean
     */
    boolean save(T entity);
    /**
    * 更新对象信息 
    * @param entity 实体类
    *     
    */
    Integer update(T entity);

    /**
    * 根据id删除对象
    * @param id 对象id
    *     
    */
    Integer deleteById(PK id);
}
#end
