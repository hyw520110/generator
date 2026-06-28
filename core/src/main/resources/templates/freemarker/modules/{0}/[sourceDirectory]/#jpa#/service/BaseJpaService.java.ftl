package ${servicePackage!};

import java.util.List;
import java.io.Serializable;

<#assign comment ="JPA 公共接口">
<#include 'comments/comment.ftl'>
public interface BaseJpaService<T, PK extends Serializable> {
    boolean save(T entity);
    T findById(PK id);
    Long count();
    Iterable<T> findAll();
    Boolean deleteById(PK id);
}
