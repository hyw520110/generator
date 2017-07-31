package ${implPackage};

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Param;

import ${mapperPackage}.BaseMapper;

public class BaseServiceImpl<T, PK extends Serializable> {

    private BaseMapper<T, PK> baseMapper;

    /**
     * <p>
     * 插入一条记录（选择字段，策略插入）
     * </p>
     *
     * @param entity
     *            实体对象
     * @return boolean
     */
    public boolean save(T entity) {
        Integer rows = baseMapper.save(entity);
        return null != rows && rows > 0;
    }

    /**
    * 根据id获取对象
    * @param id 对象id
    * @return Object  返回对象  
    */
    public T findById(PK id) {
        return baseMapper.findById(id);
    }

    /**
     * 根据指定条件查询一条记录 
     * @author:  heyiwu 
     * @param map
     * @return
     */
    public T findOne(Map<String, Object> map) {
        return baseMapper.findOne(map);
    }

    /**
    * 分页查询--总条数
    * @return Integer 总条数   
    */
    public Integer count(Map<String, Object> map) {
        return baseMapper.count(map);
    }

    /**
    * 分页查询列表，使用此方法的dto需要继承pageList
    * @return List<E> 返回数据集合   
    */
    public List<T> findPage(Map<String, Object> map) {
        return baseMapper.findPage(map);
    }

    /**
    * 查询全部
    * @return List<E> 返回数据集合   
    */
    public List<T> findAll(Map<String, Object> map) {
        return baseMapper.findAll(map);
    }

    /**
    * 根据id删除对象
    * @param id 对象id
    *     
    */
    public Integer deleteById(PK id) {
        return baseMapper.deleteById(id);
    }

    /**
     * 根据指定条件删除
     * @author:  heyiwu 
     * @param pars
     * @return
     */
    public Integer deleteByMap(@Param("pars") Map<String, Object> pars) {
        return baseMapper.deleteByMap(pars);
    }

    /**
     * 根据id更新实体
     * @author:  heyiwu 
     * @param entity 实体对象
     * @return
     */
    public Integer updateById(T entity) {
        return baseMapper.updateById(entity);
    }

    /**
    * 更新对象信息 
    * @param entity 实体类
    *     
    */
    public Integer update(T entity) {
        return baseMapper.update(entity);
    }
}
