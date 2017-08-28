#if(${StringUtils.indexOf("$superServiceImplClass", '.')}==-1)
package ${implPackage};

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import ${mapperPackage}.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.beans.factory.annotation.Autowired;
/**
 * @author ${author}
 * @since ${date}
 * @copyright: ${copyright}
 */
public class BaseServiceImpl<T> {

	@Autowired
    private BaseMapper<T> baseMapper;
     
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
    * 查询全部
    * @return List<E> 返回数据集合   
    */
    public List<T> findAll(Map<String, Object> map) {
        return baseMapper.findAll(map);
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
    * 更新对象信息 
    * @param entity 实体类
    *     
    */
    public Integer update(T entity) {
        return baseMapper.update(entity);
    }
 
}
#end