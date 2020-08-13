#if(${StringUtils.indexOf("$superServiceImplClass", '.')}==-1)
package ${implPackage};

import ${BaseServicePackage}.BaseService;

#if("plus"=="$mapperType")
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
#else
import ${mapperPackage}.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.binding.MapperMethod;
import org.apache.ibatis.session.SqlSession;
import org.mybatis.spring.SqlSessionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
#end
#set($comment="公共接口默认实现")
#parse('/templates/comments/comment.vm')
#if("plus"=="$mapperType")
public class BaseServiceImpl<M extends BaseMapper<T>, T> extends ServiceImpl<BaseMapper<T>, T> implements BaseService<T> {
#else	
public class BaseServiceImpl<T> implements BaseService{

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
        Integer rows = baseMapper.insert(entity);
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
#end 
}
#end