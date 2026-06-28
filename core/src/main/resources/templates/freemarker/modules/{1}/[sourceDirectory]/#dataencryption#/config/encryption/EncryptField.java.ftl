package ${packagePath};

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记该字段需要在数据库层进行加解密。
 * 使用方式：与 MyBatis-Plus @TableField(typeHandler = EncryptTypeHandler.class) 配合使用。
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface EncryptField {
}
