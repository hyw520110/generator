package ${rootPackage}.core.entity;

import java.io.Serializable;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableLogic;

import lombok.Data;

/**
 * Base entity for all database entities in the multi-tenant platform.
 * <p>
 * Provides:
 * <ul>
 *   <li>{@code tenantId} — tenant isolation field, auto-filled on INSERT</li>
 *   <li>{@code appId} — application isolation field, auto-filled on INSERT</li>
 *   <li>{@code createTime} — record creation timestamp (UTC), auto-filled on INSERT</li>
 *   <li>{@code updateTime} — last update timestamp (UTC), auto-filled on INSERT and UPDATE</li>
 *   <li>{@code deleted} — logical delete flag (0=active, 1=deleted)</li>
 * </ul>
 * Requires MyBatis-Plus MetaObjectHandler (AutoFillMetaObjectHandler) to populate fields.
 * All timestamps should be stored in UTC.
 */
@Data
public class BaseEntity implements Serializable {

	private static final long serialVersionUID = 4389010669628305929L;

	/**
	 * Tenant identifier for data isolation between different customers/deployments.
	 */
	@TableField(value = "tenant_id", fill = FieldFill.INSERT)
	private String tenantId;

	/**
	 * Application identifier for data isolation between different apps (e.g. senseai, VPropTrader).
	 */
	@TableField(value = "app_id", fill = FieldFill.INSERT)
	private String appId;

	/**
	 * Record creation time (UTC). Auto-filled on INSERT.
	 */
	@TableField(value = "created_at", fill = FieldFill.INSERT)
	private LocalDateTime createTime;

	/**
	 * Last update time (UTC). Auto-filled on INSERT and UPDATE.
	 */
	@TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
	private LocalDateTime updateTime;

	/**
	 * Logical delete flag. 0 = active, 1 = deleted.
	 */
	@TableLogic
	@TableField(value = "deleted", fill = FieldFill.INSERT)
	private Integer deleted;

}
