package ${mapperPackage};

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Param;

public interface BaseMapper<E, PK extends Serializable> {

    /**
    * 根据id获取对象
    * @param id 对象id
    * @return Object  返回对象  
    */
    E findById(PK id);
    
    /**
     * 根据指定条件查询一条记录 
     * @author:  heyiwu
     * @param map
     * @return
     */
    E findOne(Map<String, Object> map);
    /**
    * 分页查询--总条数
    * @return Integer 总条数   
    */
    Integer count(Map<String, Object> map);

    /**
    * 分页查询列表，使用此方法的dto需要继承pageList
    * @return List<E> 返回数据集合   
    */
    List<E> findPage(Map<String, Object> map);

    /**
    * 查询全部
    * @return List<E> 返回数据集合   
    */
    List<E> findAll(Map<String, Object> map);
    /**
    * 根据id删除对象
    * @param id 对象id
    *     
    */
    Integer deleteById(PK id);

    /**
     * 根据指定条件删除
     * @author:  heyiwu 
     * @param pars
     * @return
     */
    Integer deleteByMap(@Param("pars") Map<String, Object> pars);

    /**
    * 保存对象信息
    * @param entity 对象实体类
     * @return 
    *     
    */
    Integer save(E entity);

    /**
     * 根据id更新实体
     * @author:  heyiwu 
     * @param entity 实体对象
     * @return
     */
    Integer updateById(E entity);

    /**
    * 更新对象信息 
    * @param entity 实体类
    *     
    */
    Integer update(E entity);

}
