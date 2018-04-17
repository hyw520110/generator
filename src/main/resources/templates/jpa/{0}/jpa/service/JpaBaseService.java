package ${servicePackage};

import java.util.List;
import java.util.Map;

/**
 * @author ${author}
 * @since ${date}
 * @copyright: ${copyright}
 */
public interface JpaBaseService<T,PK> {


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
    * 根据id获取对象
    * @param id 对象id
    * @return Object  返回对象  
    */
    T findById(PK id);

    
    /**
    * 分页查询--总条数
    * @return Integer 总条数   
    */
    Long count();

   
    /**
    * 查询全部
    * @return List<E> 返回数据集合   
    */
    Iterable<T> findAll();

 
}
