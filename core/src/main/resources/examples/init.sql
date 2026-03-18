-- ========================================
-- 后台管理系统示例数据库初始化脚本
-- 包含完整的权限管理、组织架构、日志审计等模块
-- 支持一对一、一对多、多对多关系
-- 包含单一主键和复合主键
-- ========================================

-- ========================================
-- 1. 用户模块
-- ========================================

-- 用户表（单一主键）
CREATE TABLE `sys_user` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `username` VARCHAR(50) NOT NULL COMMENT '用户名',
  `password` VARCHAR(100) NOT NULL COMMENT '密码（加密）',
  `real_name` VARCHAR(50) DEFAULT NULL COMMENT '真实姓名',
  `nickname` VARCHAR(50) DEFAULT NULL COMMENT '昵称',
  `avatar` VARCHAR(255) DEFAULT NULL COMMENT '头像URL',
  `email` VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
  `mobile` VARCHAR(20) DEFAULT NULL COMMENT '手机号',
  `gender` TINYINT(2) DEFAULT '0' COMMENT '性别：0-未知，1-男，2-女',
  `birthday` DATE DEFAULT NULL COMMENT '生日',
  `status` TINYINT(2) DEFAULT '1' COMMENT '状态：0-禁用，1-启用',
  `dept_id` BIGINT(20) DEFAULT NULL COMMENT '所属部门ID',
  `position_id` BIGINT(20) DEFAULT NULL COMMENT '职位ID',
  `employee_no` VARCHAR(50) DEFAULT NULL COMMENT '工号',
  `hire_date` DATE DEFAULT NULL COMMENT '入职日期',
  `last_login_time` DATETIME DEFAULT NULL COMMENT '最后登录时间',
  `last_login_ip` VARCHAR(50) DEFAULT NULL COMMENT '最后登录IP',
  `login_count` INT(11) DEFAULT '0' COMMENT '登录次数',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `create_by` BIGINT(20) DEFAULT NULL COMMENT '创建人ID',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` BIGINT(20) DEFAULT NULL COMMENT '更新人ID',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT(1) DEFAULT '0' COMMENT '删除标记：0-未删除，1-已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  UNIQUE KEY `uk_mobile` (`mobile`),
  UNIQUE KEY `uk_email` (`email`),
  KEY `idx_dept_id` (`dept_id`),
  KEY `idx_status` (`status`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统用户表';

-- 用户详细信息表（一对一关系，单一主键）
CREATE TABLE `sys_user_detail` (
  `user_id` BIGINT(20) NOT NULL COMMENT '用户ID（关联sys_user.id）',
  `id_card` VARCHAR(18) DEFAULT NULL COMMENT '身份证号',
  `native_place` VARCHAR(100) DEFAULT NULL COMMENT '籍贯',
  `address` VARCHAR(255) DEFAULT NULL COMMENT '详细地址',
  `postcode` VARCHAR(10) DEFAULT NULL COMMENT '邮政编码',
  `qq` VARCHAR(20) DEFAULT NULL COMMENT 'QQ号',
  `wechat` VARCHAR(50) DEFAULT NULL COMMENT '微信号',
  `emergency_contact` VARCHAR(50) DEFAULT NULL COMMENT '紧急联系人',
  `emergency_phone` VARCHAR(20) DEFAULT NULL COMMENT '紧急联系电话',
  `education` VARCHAR(50) DEFAULT NULL COMMENT '学历',
  `graduate_school` VARCHAR(100) DEFAULT NULL COMMENT '毕业院校',
  `major` VARCHAR(100) DEFAULT NULL COMMENT '专业',
  `work_experience` TEXT DEFAULT NULL COMMENT '工作经历（JSON格式）',
  `skill_tags` VARCHAR(500) DEFAULT NULL COMMENT '技能标签（逗号分隔）',
  `bio` TEXT DEFAULT NULL COMMENT '个人简介',
  `interests` VARCHAR(500) DEFAULT NULL COMMENT '兴趣爱好',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`user_id`),
  CONSTRAINT `fk_user_detail_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户详细信息表';

-- 用户登录令牌表（一对多关系，单一主键）
CREATE TABLE `sys_user_token` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '令牌ID',
  `user_id` BIGINT(20) NOT NULL COMMENT '用户ID',
  `token` VARCHAR(128) NOT NULL COMMENT '登录令牌',
  `token_type` VARCHAR(20) DEFAULT 'ACCESS' COMMENT '令牌类型：ACCESS-访问令牌，REFRESH-刷新令牌',
  `device_type` VARCHAR(20) DEFAULT NULL COMMENT '设备类型：PC,MOBILE,TABLET,WEB',
  `device_name` VARCHAR(100) DEFAULT NULL COMMENT '设备名称',
  `device_id` VARCHAR(100) DEFAULT NULL COMMENT '设备唯一标识',
  `browser_type` VARCHAR(50) DEFAULT NULL COMMENT '浏览器类型',
  `browser_version` VARCHAR(50) DEFAULT NULL COMMENT '浏览器版本',
  `os_type` VARCHAR(50) DEFAULT NULL COMMENT '操作系统类型',
  `os_version` VARCHAR(50) DEFAULT NULL COMMENT '操作系统版本',
  `ip_address` VARCHAR(50) DEFAULT NULL COMMENT 'IP地址',
  `location` VARCHAR(100) DEFAULT NULL COMMENT '登录地点',
  `expire_time` DATETIME NOT NULL COMMENT '过期时间',
  `is_expired` TINYINT(1) DEFAULT '0' COMMENT '是否过期：0-未过期，1-已过期',
  `last_access_time` DATETIME DEFAULT NULL COMMENT '最后访问时间',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_token` (`token`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_expire_time` (`expire_time`),
  CONSTRAINT `fk_user_token_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户登录令牌表';

-- ========================================
-- 2. 组织架构模块
-- ========================================

-- 部门表（单一主键，树形结构）
CREATE TABLE `sys_dept` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '部门ID',
  `parent_id` BIGINT(20) DEFAULT '0' COMMENT '父部门ID，0表示顶级部门',
  `dept_name` VARCHAR(50) NOT NULL COMMENT '部门名称',
  `dept_code` VARCHAR(50) DEFAULT NULL COMMENT '部门编码',
  `dept_type` TINYINT(2) DEFAULT '1' COMMENT '部门类型：1-公司，2-部门，3-小组',
  `leader_id` BIGINT(20) DEFAULT NULL COMMENT '负责人ID',
  `leader_name` VARCHAR(50) DEFAULT NULL COMMENT '负责人姓名',
  `phone` VARCHAR(20) DEFAULT NULL COMMENT '联系电话',
  `email` VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
  `address` VARCHAR(255) DEFAULT NULL COMMENT '部门地址',
  `sort_order` INT(11) DEFAULT '0' COMMENT '排序号',
  `status` TINYINT(2) DEFAULT '1' COMMENT '状态：0-禁用，1-启用',
  `ancestors` VARCHAR(500) DEFAULT NULL COMMENT '祖级列表（逗号分隔）',
  `level` TINYINT(2) DEFAULT '1' COMMENT '层级：1-顶级，2-二级...',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `create_by` BIGINT(20) DEFAULT NULL COMMENT '创建人ID',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` BIGINT(20) DEFAULT NULL COMMENT '更新人ID',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT(1) DEFAULT '0' COMMENT '删除标记：0-未删除，1-已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dept_code` (`dept_code`),
  KEY `idx_parent_id` (`parent_id`),
  KEY `idx_dept_name` (`dept_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='部门表';

-- 职位表（单一主键）
CREATE TABLE `sys_position` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '职位ID',
  `position_name` VARCHAR(50) NOT NULL COMMENT '职位名称',
  `position_code` VARCHAR(50) DEFAULT NULL COMMENT '职位编码',
  `position_level` TINYINT(2) DEFAULT NULL COMMENT '职位级别',
  `dept_id` BIGINT(20) DEFAULT NULL COMMENT '所属部门ID',
  `description` VARCHAR(500) DEFAULT NULL COMMENT '职位描述',
  `sort_order` INT(11) DEFAULT '0' COMMENT '排序号',
  `status` TINYINT(2) DEFAULT '1' COMMENT '状态：0-禁用，1-启用',
  `create_by` BIGINT(20) DEFAULT NULL COMMENT '创建人ID',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` BIGINT(20) DEFAULT NULL COMMENT '更新人ID',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT(1) DEFAULT '0' COMMENT '删除标记：0-未删除，1-已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_position_code` (`position_code`),
  KEY `idx_dept_id` (`dept_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='职位表';

-- ========================================
-- 3. 角色权限模块
-- ========================================

-- 角色表（单一主键）
CREATE TABLE `sys_role` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '角色ID',
  `role_name` VARCHAR(50) NOT NULL COMMENT '角色名称',
  `role_code` VARCHAR(50) NOT NULL COMMENT '角色编码',
  `role_type` TINYINT(2) DEFAULT '1' COMMENT '角色类型：1-系统角色，2-业务角色',
  `data_scope` TINYINT(2) DEFAULT '1' COMMENT '数据范围：1-全部数据，2-本部门及以下，3-本部门，4-仅本人，5-自定义',
  `sort_order` INT(11) DEFAULT '0' COMMENT '排序号',
  `status` TINYINT(2) DEFAULT '1' COMMENT '状态：0-禁用，1-启用',
  `is_default` TINYINT(1) DEFAULT '0' COMMENT '是否默认角色：0-否，1-是',
  `description` VARCHAR(500) DEFAULT NULL COMMENT '角色描述',
  `create_by` BIGINT(20) DEFAULT NULL COMMENT '创建人ID',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` BIGINT(20) DEFAULT NULL COMMENT '更新人ID',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT(1) DEFAULT '0' COMMENT '删除标记：0-未删除，1-已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_code` (`role_code`),
  KEY `idx_role_name` (`role_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色表';

-- 菜单表（单一主键，树形结构）
CREATE TABLE `sys_menu` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '菜单ID',
  `parent_id` BIGINT(20) DEFAULT '0' COMMENT '父菜单ID，0表示顶级菜单',
  `menu_name` VARCHAR(50) NOT NULL COMMENT '菜单名称',
  `menu_type` CHAR(1) NOT NULL COMMENT '菜单类型：M-目录，C-菜单，F-按钮',
  `menu_code` VARCHAR(100) DEFAULT NULL COMMENT '菜单编码',
  `route_path` VARCHAR(200) DEFAULT NULL COMMENT '路由路径',
  `component_path` VARCHAR(255) DEFAULT NULL COMMENT '组件路径',
  `redirect` VARCHAR(255) DEFAULT NULL COMMENT '重定向路径',
  `icon` VARCHAR(100) DEFAULT NULL COMMENT '菜单图标',
  `permission` VARCHAR(100) DEFAULT NULL COMMENT '权限标识',
  `visible` TINYINT(1) DEFAULT '1' COMMENT '是否显示：0-隐藏，1-显示',
  `is_frame` TINYINT(1) DEFAULT '0' COMMENT '是否外链：0-否，1-是',
  `is_cache` TINYINT(1) DEFAULT '0' COMMENT '是否缓存：0-不缓存，1-缓存',
  `sort_order` INT(11) DEFAULT '0' COMMENT '排序号',
  `status` TINYINT(2) DEFAULT '1' COMMENT '状态：0-禁用，1-启用',
  `ancestors` VARCHAR(500) DEFAULT NULL COMMENT '祖级列表（逗号分隔）',
  `level` TINYINT(2) DEFAULT '1' COMMENT '层级：1-顶级，2-二级...',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `create_by` BIGINT(20) DEFAULT NULL COMMENT '创建人ID',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` BIGINT(20) DEFAULT NULL COMMENT '更新人ID',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT(1) DEFAULT '0' COMMENT '删除标记：0-未删除，1-已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_menu_code` (`menu_code`),
  KEY `idx_parent_id` (`parent_id`),
  KEY `idx_menu_name` (`menu_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='菜单权限表';

-- 权限表（单一主键）
CREATE TABLE `sys_permission` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '权限ID',
  `permission_name` VARCHAR(50) NOT NULL COMMENT '权限名称',
  `permission_code` VARCHAR(100) NOT NULL COMMENT '权限编码',
  `permission_type` TINYINT(2) DEFAULT '1' COMMENT '权限类型：1-接口权限，2-数据权限，3-业务权限',
  `resource_type` VARCHAR(20) DEFAULT NULL COMMENT '资源类型：API,MENU,BUTTON,DATACOL',
  `resource_path` VARCHAR(255) DEFAULT NULL COMMENT '资源路径',
  `http_method` VARCHAR(20) DEFAULT NULL COMMENT 'HTTP方法：GET,POST,PUT,DELETE等',
  `description` VARCHAR(500) DEFAULT NULL COMMENT '权限描述',
  `status` TINYINT(2) DEFAULT '1' COMMENT '状态：0-禁用，1-启用',
  `create_by` BIGINT(20) DEFAULT NULL COMMENT '创建人ID',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` BIGINT(20) DEFAULT NULL COMMENT '更新人ID',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT(1) DEFAULT '0' COMMENT '删除标记：0-未删除，1-已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_permission_code` (`permission_code`),
  KEY `idx_permission_name` (`permission_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='权限表';

-- ========================================
-- 4. 关联表（多对多关系，复合主键）
-- ========================================

-- 用户角色关联表（复合主键）
CREATE TABLE `sys_user_role` (
  `user_id` BIGINT(20) NOT NULL COMMENT '用户ID',
  `role_id` BIGINT(20) NOT NULL COMMENT '角色ID',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`user_id`, `role_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_role_id` (`role_id`),
  CONSTRAINT `fk_user_role_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_user_role_role` FOREIGN KEY (`role_id`) REFERENCES `sys_role` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色关联表';

-- 角色菜单关联表（复合主键）
CREATE TABLE `sys_role_menu` (
  `role_id` BIGINT(20) NOT NULL COMMENT '角色ID',
  `menu_id` BIGINT(20) NOT NULL COMMENT '菜单ID',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`role_id`, `menu_id`),
  KEY `idx_role_id` (`role_id`),
  KEY `idx_menu_id` (`menu_id`),
  CONSTRAINT `fk_role_menu_role` FOREIGN KEY (`role_id`) REFERENCES `sys_role` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_role_menu_menu` FOREIGN KEY (`menu_id`) REFERENCES `sys_menu` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色菜单关联表';

-- 角色权限关联表（复合主键）
CREATE TABLE `sys_role_permission` (
  `role_id` BIGINT(20) NOT NULL COMMENT '角色ID',
  `permission_id` BIGINT(20) NOT NULL COMMENT '权限ID',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`role_id`, `permission_id`),
  KEY `idx_role_id` (`role_id`),
  KEY `idx_permission_id` (`permission_id`),
  CONSTRAINT `fk_role_permission_role` FOREIGN KEY (`role_id`) REFERENCES `sys_role` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_role_permission_permission` FOREIGN KEY (`permission_id`) REFERENCES `sys_permission` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色权限关联表';

-- 用户部门关联表（复合主键，支持多部门）
CREATE TABLE `sys_user_dept` (
  `user_id` BIGINT(20) NOT NULL COMMENT '用户ID',
  `dept_id` BIGINT(20) NOT NULL COMMENT '部门ID',
  `is_primary` TINYINT(1) DEFAULT '0' COMMENT '是否主部门：0-否，1-是',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`user_id`, `dept_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_dept_id` (`dept_id`),
  CONSTRAINT `fk_user_dept_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_user_dept_dept` FOREIGN KEY (`dept_id`) REFERENCES `sys_dept` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户部门关联表';

-- ========================================
-- 5. 数据字典模块
-- ========================================

-- 字典类型表（单一主键）
CREATE TABLE `sys_dict_type` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '字典ID',
  `dict_name` VARCHAR(100) NOT NULL COMMENT '字典名称',
  `dict_code` VARCHAR(100) NOT NULL COMMENT '字典编码',
  `dict_type` VARCHAR(100) DEFAULT NULL COMMENT '字典类型',
  `is_system` TINYINT(1) DEFAULT '0' COMMENT '是否系统字典：0-否，1-是',
  `status` TINYINT(2) DEFAULT '1' COMMENT '状态：0-禁用，1-启用',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `create_by` BIGINT(20) DEFAULT NULL COMMENT '创建人ID',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` BIGINT(20) DEFAULT NULL COMMENT '更新人ID',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT(1) DEFAULT '0' COMMENT '删除标记：0-未删除，1-已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dict_code` (`dict_code`),
  KEY `idx_dict_name` (`dict_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='字典类型表';

-- 字典数据表（单一主键，一对多关系）
CREATE TABLE `sys_dict_data` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '字典数据ID',
  `dict_type_id` BIGINT(20) NOT NULL COMMENT '字典类型ID',
  `dict_label` VARCHAR(100) NOT NULL COMMENT '字典标签',
  `dict_value` VARCHAR(100) NOT NULL COMMENT '字典值',
  `dict_sort` INT(11) DEFAULT '0' COMMENT '字典排序',
  `css_class` VARCHAR(100) DEFAULT NULL COMMENT '样式类名',
  `list_class` VARCHAR(100) DEFAULT NULL COMMENT '列表样式类名',
  `is_default` TINYINT(1) DEFAULT '0' COMMENT '是否默认：0-否，1-是',
  `status` TINYINT(2) DEFAULT '1' COMMENT '状态：0-禁用，1-启用',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `create_by` BIGINT(20) DEFAULT NULL COMMENT '创建人ID',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` BIGINT(20) DEFAULT NULL COMMENT '更新人ID',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT(1) DEFAULT '0' COMMENT '删除标记：0-未删除，1-已删除',
  PRIMARY KEY (`id`),
  KEY `idx_dict_type_id` (`dict_type_id`),
  KEY `idx_dict_value` (`dict_value`),
  CONSTRAINT `fk_dict_data_type` FOREIGN KEY (`dict_type_id`) REFERENCES `sys_dict_type` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='字典数据表';

-- ========================================
-- 6. 日志审计模块
-- ========================================

-- 登录日志表（单一主键）
CREATE TABLE `sys_login_log` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '日志ID',
  `user_id` BIGINT(20) DEFAULT NULL COMMENT '用户ID',
  `username` VARCHAR(50) DEFAULT NULL COMMENT '用户名',
  `login_type` VARCHAR(20) DEFAULT 'PASSWORD' COMMENT '登录类型：PASSWORD-密码，SMS-短信，WECHAT-微信，QQ-QQ等',
  `login_status` TINYINT(2) DEFAULT '1' COMMENT '登录状态：0-失败，1-成功',
  `ip_address` VARCHAR(50) DEFAULT NULL COMMENT 'IP地址',
  `location` VARCHAR(100) DEFAULT NULL COMMENT '登录地点',
  `device_type` VARCHAR(20) DEFAULT NULL COMMENT '设备类型：PC,MOBILE,TABLET,WEB',
  `device_name` VARCHAR(100) DEFAULT NULL COMMENT '设备名称',
  `device_id` VARCHAR(100) DEFAULT NULL COMMENT '设备唯一标识',
  `browser_type` VARCHAR(50) DEFAULT NULL COMMENT '浏览器类型',
  `browser_version` VARCHAR(50) DEFAULT NULL COMMENT '浏览器版本',
  `os_type` VARCHAR(50) DEFAULT NULL COMMENT '操作系统类型',
  `os_version` VARCHAR(50) DEFAULT NULL COMMENT '操作系统版本',
  `failure_reason` VARCHAR(255) DEFAULT NULL COMMENT '失败原因',
  `login_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '登录时间',
  `logout_time` DATETIME DEFAULT NULL COMMENT '退出时间',
  `online_duration` INT(11) DEFAULT NULL COMMENT '在线时长（秒）',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_username` (`username`),
  KEY `idx_login_time` (`login_time`),
  KEY `idx_login_status` (`login_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='登录日志表';

-- 操作日志表（单一主键）
CREATE TABLE `sys_oper_log` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '日志ID',
  `user_id` BIGINT(20) DEFAULT NULL COMMENT '用户ID',
  `username` VARCHAR(50) DEFAULT NULL COMMENT '用户名',
  `real_name` VARCHAR(50) DEFAULT NULL COMMENT '真实姓名',
  `module_name` VARCHAR(50) DEFAULT NULL COMMENT '模块名称',
  `business_type` VARCHAR(50) DEFAULT NULL COMMENT '业务类型',
  `method_name` VARCHAR(100) DEFAULT NULL COMMENT '方法名称',
  `request_method` VARCHAR(10) DEFAULT NULL COMMENT '请求方法：GET,POST,PUT,DELETE等',
  `request_url` VARCHAR(255) DEFAULT NULL COMMENT '请求URL',
  `request_params` TEXT DEFAULT NULL COMMENT '请求参数',
  `request_body` TEXT DEFAULT NULL COMMENT '请求体',
  `response_data` TEXT DEFAULT NULL COMMENT '响应数据',
  `oper_status` TINYINT(2) DEFAULT '1' COMMENT '操作状态：0-失败，1-成功',
  `error_msg` TEXT DEFAULT NULL COMMENT '错误信息',
  `cost_time` INT(11) DEFAULT NULL COMMENT '耗时（毫秒）',
  `ip_address` VARCHAR(50) DEFAULT NULL COMMENT 'IP地址',
  `location` VARCHAR(100) DEFAULT NULL COMMENT '操作地点',
  `browser_type` VARCHAR(50) DEFAULT NULL COMMENT '浏览器类型',
  `browser_version` VARCHAR(50) DEFAULT NULL COMMENT '浏览器版本',
  `os_type` VARCHAR(50) DEFAULT NULL COMMENT '操作系统类型',
  `os_version` VARCHAR(50) DEFAULT NULL COMMENT '操作系统版本',
  `oper_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_username` (`username`),
  KEY `idx_oper_time` (`oper_time`),
  KEY `idx_business_type` (`business_type`),
  KEY `idx_oper_status` (`oper_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作日志表';

-- ========================================
-- 7. 系统配置模块
-- ========================================

-- 系统参数表（单一主键）
CREATE TABLE `sys_config` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '参数ID',
  `config_name` VARCHAR(100) NOT NULL COMMENT '参数名称',
  `config_key` VARCHAR(100) NOT NULL COMMENT '参数键名',
  `config_value` VARCHAR(500) DEFAULT NULL COMMENT '参数键值',
  `config_type` TINYINT(2) DEFAULT '1' COMMENT '参数类型：1-系统参数，2-业务参数',
  `is_system` TINYINT(1) DEFAULT '0' COMMENT '是否系统参数：0-否，1-是',
  `is_encrypted` TINYINT(1) DEFAULT '0' COMMENT '是否加密：0-否，1-是',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `create_by` BIGINT(20) DEFAULT NULL COMMENT '创建人ID',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` BIGINT(20) DEFAULT NULL COMMENT '更新人ID',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT(1) DEFAULT '0' COMMENT '删除标记：0-未删除，1-已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_config_key` (`config_key`),
  KEY `idx_config_name` (`config_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统参数表';

-- 通知公告表（单一主键）
CREATE TABLE `sys_notice` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '公告ID',
  `notice_title` VARCHAR(100) NOT NULL COMMENT '公告标题',
  `notice_type` VARCHAR(20) DEFAULT 'INFO' COMMENT '公告类型：INFO-通知，ANNOUNCEMENT-公告，WARNING-警告，URGENT-紧急',
  `notice_content` TEXT DEFAULT NULL COMMENT '公告内容',
  `target_type` TINYINT(2) DEFAULT '1' COMMENT '目标类型：1-全部用户，2-指定用户，3-指定部门',
  `target_users` TEXT DEFAULT NULL COMMENT '目标用户ID列表（逗号分隔）',
  `target_depts` TEXT DEFAULT NULL COMMENT '目标部门ID列表（逗号分隔）',
  `publish_time` DATETIME DEFAULT NULL COMMENT '发布时间',
  `publisher_id` BIGINT(20) DEFAULT NULL COMMENT '发布人ID',
  `publisher_name` VARCHAR(50) DEFAULT NULL COMMENT '发布人姓名',
  `status` TINYINT(2) DEFAULT '0' COMMENT '状态：0-草稿，1-已发布，2-已撤回',
  `is_top` TINYINT(1) DEFAULT '0' COMMENT '是否置顶：0-否，1-是',
  `view_count` INT(11) DEFAULT '0' COMMENT '浏览次数',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `create_by` BIGINT(20) DEFAULT NULL COMMENT '创建人ID',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` BIGINT(20) DEFAULT NULL COMMENT '更新人ID',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT(1) DEFAULT '0' COMMENT '删除标记：0-未删除，1-已删除',
  PRIMARY KEY (`id`),
  KEY `idx_notice_type` (`notice_type`),
  KEY `idx_publish_time` (`publish_time`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通知公告表';

-- 公告阅读记录表（复合主键）
CREATE TABLE `sys_notice_read` (
  `notice_id` BIGINT(20) NOT NULL COMMENT '公告ID',
  `user_id` BIGINT(20) NOT NULL COMMENT '用户ID',
  `read_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '阅读时间',
  PRIMARY KEY (`notice_id`, `user_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_read_time` (`read_time`),
  CONSTRAINT `fk_notice_read_notice` FOREIGN KEY (`notice_id`) REFERENCES `sys_notice` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_notice_read_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='公告阅读记录表';

-- ========================================
-- 8. 消息通知模块
-- ========================================

-- 消息表（单一主键）
CREATE TABLE `sys_message` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '消息ID',
  `sender_id` BIGINT(20) DEFAULT NULL COMMENT '发送人ID',
  `sender_name` VARCHAR(50) DEFAULT NULL COMMENT '发送人姓名',
  `receiver_id` BIGINT(20) NOT NULL COMMENT '接收人ID',
  `receiver_name` VARCHAR(50) DEFAULT NULL COMMENT '接收人姓名',
  `message_type` VARCHAR(20) DEFAULT 'SYSTEM' COMMENT '消息类型：SYSTEM-系统，USER-用户，NOTICE-通知，REMINDER-提醒',
  `message_title` VARCHAR(200) NOT NULL COMMENT '消息标题',
  `message_content` TEXT DEFAULT NULL COMMENT '消息内容',
  `business_type` VARCHAR(50) DEFAULT NULL COMMENT '业务类型',
  `business_id` BIGINT(20) DEFAULT NULL COMMENT '业务ID',
  `link_url` VARCHAR(255) DEFAULT NULL COMMENT '跳转链接',
  `is_read` TINYINT(1) DEFAULT '0' COMMENT '是否已读：0-未读，1-已读',
  `read_time` DATETIME DEFAULT NULL COMMENT '阅读时间',
  `send_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '发送时间',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_sender_id` (`sender_id`),
  KEY `idx_receiver_id` (`receiver_id`),
  KEY `idx_is_read` (`is_read`),
  KEY `idx_send_time` (`send_time`),
  CONSTRAINT `fk_message_sender` FOREIGN KEY (`sender_id`) REFERENCES `sys_user` (`id`) ON DELETE SET NULL,
  CONSTRAINT `fk_message_receiver` FOREIGN KEY (`receiver_id`) REFERENCES `sys_user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='消息表';

-- ========================================
-- 9. 文件管理模块
-- ========================================

-- 文件信息表（单一主键）
CREATE TABLE `sys_file` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '文件ID',
  `file_name` VARCHAR(255) NOT NULL COMMENT '文件名称',
  `file_path` VARCHAR(500) NOT NULL COMMENT '文件路径',
  `file_size` BIGINT(20) DEFAULT NULL COMMENT '文件大小（字节）',
  `file_type` VARCHAR(100) DEFAULT NULL COMMENT '文件类型',
  `file_ext` VARCHAR(20) DEFAULT NULL COMMENT '文件扩展名',
  `mime_type` VARCHAR(100) DEFAULT NULL COMMENT 'MIME类型',
  `file_md5` VARCHAR(32) DEFAULT NULL COMMENT '文件MD5',
  `storage_type` VARCHAR(20) DEFAULT 'LOCAL' COMMENT '存储类型：LOCAL-本地，OSS-阿里云OSS，COS-腾讯云COS，MINIO-MinIO',
  `bucket_name` VARCHAR(100) DEFAULT NULL COMMENT '存储桶名称',
  `upload_user_id` BIGINT(20) DEFAULT NULL COMMENT '上传用户ID',
  `upload_user_name` VARCHAR(50) DEFAULT NULL COMMENT '上传用户姓名',
  `business_type` VARCHAR(50) DEFAULT NULL COMMENT '业务类型',
  `business_id` BIGINT(20) DEFAULT NULL COMMENT '业务ID',
  `is_image` TINYINT(1) DEFAULT '0' COMMENT '是否图片：0-否，1-是',
  `width` INT(11) DEFAULT NULL COMMENT '图片宽度',
  `height` INT(11) DEFAULT NULL COMMENT '图片高度',
  `thumbnail_path` VARCHAR(500) DEFAULT NULL COMMENT '缩略图路径',
  `download_count` INT(11) DEFAULT '0' COMMENT '下载次数',
  `access_count` INT(11) DEFAULT '0' COMMENT '访问次数',
  `status` TINYINT(2) DEFAULT '1' COMMENT '状态：0-删除，1-正常',
  `expire_time` DATETIME DEFAULT NULL COMMENT '过期时间',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_file_md5` (`file_md5`),
  KEY `idx_file_name` (`file_name`),
  KEY `idx_business_type` (`business_type`),
  KEY `idx_business_id` (`business_id`),
  KEY `idx_upload_user_id` (`upload_user_id`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文件信息表';

-- ========================================
-- 10. 定时任务模块
-- ========================================

-- 定时任务表（单一主键）
CREATE TABLE `sys_job` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '任务ID',
  `job_name` VARCHAR(100) NOT NULL COMMENT '任务名称',
  `job_group` VARCHAR(50) DEFAULT 'DEFAULT' COMMENT '任务组',
  `job_class` VARCHAR(255) NOT NULL COMMENT '任务类名',
  `cron_expression` VARCHAR(100) NOT NULL COMMENT 'Cron表达式',
  `job_description` VARCHAR(500) DEFAULT NULL COMMENT '任务描述',
  `job_params` TEXT DEFAULT NULL COMMENT '任务参数（JSON格式）',
  `misfire_policy` TINYINT(2) DEFAULT '1' COMMENT '错过执行策略：1-立即执行，2-执行一次，3-放弃执行',
  `concurrent` TINYINT(2) DEFAULT '0' COMMENT '是否并发执行：0-禁止，1-允许',
  `status` TINYINT(2) DEFAULT '0' COMMENT '状态：0-暂停，1-运行中',
  `execute_times` INT(11) DEFAULT '0' COMMENT '执行次数',
  `last_execute_time` DATETIME DEFAULT NULL COMMENT '上次执行时间',
  `next_execute_time` DATETIME DEFAULT NULL COMMENT '下次执行时间',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `create_by` BIGINT(20) DEFAULT NULL COMMENT '创建人ID',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` BIGINT(20) DEFAULT NULL COMMENT '更新人ID',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT(1) DEFAULT '0' COMMENT '删除标记：0-未删除，1-已删除',
  PRIMARY KEY (`id`),
  KEY `idx_job_name` (`job_name`),
  KEY `idx_job_group` (`job_group`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='定时任务表';

-- 定时任务执行日志表（单一主键）
CREATE TABLE `sys_job_log` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '日志ID',
  `job_id` BIGINT(20) NOT NULL COMMENT '任务ID',
  `job_name` VARCHAR(100) DEFAULT NULL COMMENT '任务名称',
  `job_group` VARCHAR(50) DEFAULT NULL COMMENT '任务组',
  `job_class` VARCHAR(255) DEFAULT NULL COMMENT '任务类名',
  `execute_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '执行时间',
  `execute_status` TINYINT(2) DEFAULT '1' COMMENT '执行状态：0-失败，1-成功',
  `execute_result` TEXT DEFAULT NULL COMMENT '执行结果',
  `error_message` TEXT DEFAULT NULL COMMENT '错误信息',
  `cost_time` INT(11) DEFAULT NULL COMMENT '耗时（毫秒）',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_job_id` (`job_id`),
  KEY `idx_execute_time` (`execute_time`),
  KEY `idx_execute_status` (`execute_status`),
  CONSTRAINT `fk_job_log_job` FOREIGN KEY (`job_id`) REFERENCES `sys_job` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='定时任务执行日志表';

-- ========================================
-- 示例数据
-- ========================================

-- 插入用户数据
INSERT INTO `sys_user` (`id`, `username`, `password`, `real_name`, `nickname`, `email`, `mobile`, `gender`, `status`, `dept_id`, `position_id`, `employee_no`, `hire_date`, `remark`, `create_by`, `create_time`) VALUES
(1, 'admin', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '系统管理员', '超级管理员', 'admin@example.com', '13800138000', 1, 1, 1, 1, 'E0001', '2020-01-01', '系统超级管理员', 1, '2020-01-01 00:00:00'),
(2, 'zhangsan', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '张三', '张三', 'zhangsan@example.com', '13800138001', 1, 1, 2, 2, 'E0002', '2020-02-01', '开发工程师', 1, '2020-02-01 00:00:00'),
(3, 'lisi', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '李四', '李四', 'lisi@example.com', '13800138002', 2, 1, 3, 3, 'E0003', '2020-03-01', '产品经理', 1, '2020-03-01 00:00:00'),
(4, 'wangwu', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '王五', '王五', 'wangwu@example.com', '13800138003', 1, 1, 4, 4, 'E0004', '2020-04-01', '测试工程师', 1, '2020-04-01 00:00:00'),
(5, 'zhaoliu', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '赵六', '赵六', 'zhaoliu@example.com', '13800138004', 1, 0, 2, 5, 'E0005', '2020-05-01', '运维工程师', 1, '2020-05-01 00:00:00');

-- 插入用户详细信息
INSERT INTO `sys_user_detail` (`user_id`, `id_card`, `native_place`, `address`, `postcode`, `qq`, `wechat`, `emergency_contact`, `emergency_phone`, `education`, `graduate_school`, `major`, `skill_tags`, `bio`, `interests`, `create_time`) VALUES
(1, '110101199001011234', '北京市', '北京市朝阳区', '100000', '123456789', 'wx_admin', '张父', '13900000000', '本科', '清华大学', '计算机科学与技术', 'Java,Spring,MySQL', '热爱技术，专注于后端开发', '阅读,运动', '2020-01-01 00:00:00'),
(2, '110101199002022345', '北京市', '北京市海淀区', '100084', '234567890', 'wx_zhangsan', '张母', '13900000001', '硕士', '北京大学', '软件工程', 'Java,Python,Redis', '全栈开发工程师', '编程,旅游', '2020-02-01 00:00:00'),
(3, '110101199003033456', '上海市', '上海市浦东新区', '200000', '345678901', 'wx_lisi', '李父', '13900000002', '本科', '复旦大学', '工商管理', '产品经理,数据分析', '专注于用户体验', '音乐,电影', '2020-03-01 00:00:00');

-- 插入部门数据
INSERT INTO `sys_dept` (`id`, `parent_id`, `dept_name`, `dept_code`, `dept_type`, `leader_id`, `leader_name`, `phone`, `email`, `address`, `sort_order`, `status`, `ancestors`, `level`, `remark`, `create_by`, `create_time`) VALUES
(1, 0, '示例科技有限公司', 'COMPANY_001', 1, 1, '系统管理员', '010-12345678', 'company@example.com', '北京市朝阳区', 1, 1, '0', 1, '总公司', 1, '2020-01-01 00:00:00'),
(2, 1, '研发中心', 'DEPT_001', 2, 2, '张三', '010-12345679', 'rd@example.com', '北京市朝阳区', 1, 1, '0,1', 2, '负责产品研发', 1, '2020-01-01 00:00:00'),
(3, 1, '市场部', 'DEPT_002', 2, 3, '李四', '010-12345680', 'market@example.com', '北京市朝阳区', 2, 1, '0,1', 2, '负责市场推广', 1, '2020-01-01 00:00:00'),
(4, 1, '人力资源部', 'DEPT_003', 2, 5, '赵六', '010-12345681', 'hr@example.com', '北京市朝阳区', 3, 1, '0,1', 2, '负责人力资源管理', 1, '2020-01-01 00:00:00'),
(5, 2, '开发组', 'DEPT_001_001', 3, 2, '张三', '010-12345682', 'dev@example.com', '北京市朝阳区', 1, 1, '0,1,2', 3, '负责开发工作', 1, '2020-01-01 00:00:00'),
(6, 2, '测试组', 'DEPT_001_002', 3, 4, '王五', '010-12345683', 'test@example.com', '北京市朝阳区', 2, 1, '0,1,2', 3, '负责测试工作', 1, '2020-01-01 00:00:00');

-- 插入职位数据
INSERT INTO `sys_position` (`id`, `position_name`, `position_code`, `position_level`, `dept_id`, `description`, `sort_order`, `status`, `create_by`, `create_time`) VALUES
(1, '总经理', 'POS_001', 1, 1, '公司总经理', 1, 1, 1, '2020-01-01 00:00:00'),
(2, '研发总监', 'POS_002', 2, 2, '研发中心总监', 1, 1, 1, '2020-01-01 00:00:00'),
(3, '产品经理', 'POS_003', 3, 3, '产品经理', 1, 1, 1, '2020-01-01 00:00:00'),
(4, '测试工程师', 'POS_004', 4, 6, '测试工程师', 1, 1, 1, '2020-01-01 00:00:00'),
(5, '运维工程师', 'POS_005', 4, 4, '运维工程师', 1, 1, 1, '2020-01-01 00:00:00');

-- 插入角色数据
INSERT INTO `sys_role` (`id`, `role_name`, `role_code`, `role_type`, `data_scope`, `sort_order`, `status`, `is_default`, `description`, `create_by`, `create_time`) VALUES
(1, '超级管理员', 'ROLE_SUPER_ADMIN', 1, 1, 1, 1, 1, '拥有所有权限', 1, '2020-01-01 00:00:00'),
(2, '管理员', 'ROLE_ADMIN', 1, 2, 2, 1, 1, '拥有大部分权限', 1, '2020-01-01 00:00:00'),
(3, '普通用户', 'ROLE_USER', 2, 4, 3, 1, 1, '普通用户权限', 1, '2020-01-01 00:00:00'),
(4, '访客', 'ROLE_GUEST', 2, 5, 4, 1, 0, '访客权限', 1, '2020-01-01 00:00:00');

-- 插入菜单数据
INSERT INTO `sys_menu` (`id`, `parent_id`, `menu_name`, `menu_type`, `menu_code`, `route_path`, `component_path`, `icon`, `permission`, `visible`, `is_frame`, `is_cache`, `sort_order`, `status`, `ancestors`, `level`, `remark`, `create_by`, `create_time`) VALUES
(1, 0, '系统管理', 'M', 'SYSTEM', '/system', NULL, 'setting', NULL, 1, 0, 0, 1, 1, '0', 1, '系统管理目录', 1, '2020-01-01 00:00:00'),
(2, 0, '用户管理', 'M', 'USER', '/user', NULL, 'user', NULL, 1, 0, 0, 2, 1, '0', 1, '用户管理目录', 1, '2020-01-01 00:00:00'),
(3, 0, '工作台', 'M', 'WORKSPACE', '/workspace', NULL, 'dashboard', NULL, 1, 0, 0, 3, 1, '0', 1, '工作台目录', 1, '2020-01-01 00:00:00'),
(100, 1, '用户管理', 'C', 'SYSTEM_USER', '/system/user', 'system/user/index', 'peoples', 'system:user:list', 1, 0, 0, 1, 1, '0,1', 2, '用户管理菜单', 1, '2020-01-01 00:00:00'),
(101, 1, '角色管理', 'C', 'SYSTEM_ROLE', '/system/role', 'system/role/index', 'team', 'system:role:list', 1, 0, 0, 2, 1, '0,1', 2, '角色管理菜单', 1, '2020-01-01 00:00:00'),
(102, 1, '菜单管理', 'C', 'SYSTEM_MENU', '/system/menu', 'system/menu/index', 'tree-table', 'system:menu:list', 1, 0, 0, 3, 1, '0,1', 2, '菜单管理菜单', 1, '2020-01-01 00:00:00'),
(103, 1, '部门管理', 'C', 'SYSTEM_DEPT', '/system/dept', 'system/dept/index', 'tree', 'system:dept:list', 1, 0, 0, 4, 1, '0,1', 2, '部门管理菜单', 1, '2020-01-01 00:00:00'),
(104, 1, '字典管理', 'C', 'SYSTEM_DICT', '/system/dict', 'system/dict/index', 'dict', 'system:dict:list', 1, 0, 0, 5, 1, '0,1', 2, '字典管理菜单', 1, '2020-01-01 00:00:00'),
(105, 1, '参数管理', 'C', 'SYSTEM_CONFIG', '/system/config', 'system/config/index', 'edit', 'system:config:list', 1, 0, 0, 6, 1, '0,1', 2, '参数管理菜单', 1, '2020-01-01 00:00:00'),
(106, 1, '通知公告', 'C', 'SYSTEM_NOTICE', '/system/notice', 'system/notice/index', 'message', 'system:notice:list', 1, 0, 0, 7, 1, '0,1', 2, '通知公告菜单', 1, '2020-01-01 00:00:00'),
(107, 1, '日志管理', 'C', 'SYSTEM_LOG', '/system/log', 'system/log/index', 'log', 'system:log:list', 1, 0, 0, 8, 1, '0,1', 2, '日志管理菜单', 1, '2020-01-01 00:00:00'),
(108, 1, '在线用户', 'C', 'SYSTEM_ONLINE', '/system/online', 'system/online/index', 'online', 'system:online:list', 1, 0, 0, 9, 1, '0,1', 2, '在线用户菜单', 1, '2020-01-01 00:00:00'),
(109, 1, '定时任务', 'C', 'SYSTEM_JOB', '/system/job', 'system/job/index', 'job', 'system:job:list', 1, 0, 0, 10, 1, '0,1', 2, '定时任务菜单', 1, '2020-01-01 00:00:00'),
(200, 2, '个人信息', 'C', 'USER_PROFILE', '/user/profile', 'user/profile/index', 'user', NULL, 1, 0, 0, 1, 1, '0,2', 2, '个人信息菜单', 1, '2020-01-01 00:00:00'),
(201, 2, '密码修改', 'C', 'USER_PASSWORD', '/user/password', 'user/password/index', 'password', NULL, 1, 0, 0, 2, 1, '0,2', 2, '密码修改菜单', 1, '2020-01-01 00:00:00'),
(300, 3, '首页', 'C', 'WORKSPACE_INDEX', '/workspace/index', 'workspace/index/index', 'home', NULL, 1, 0, 0, 1, 1, '0,3', 2, '首页菜单', 1, '2020-01-01 00:00:00'),
(1001, 100, '用户查询', 'F', 'SYSTEM_USER_QUERY', NULL, NULL, NULL, 'system:user:query', 1, 0, 0, 1, 1, '0,1,100', 3, NULL, 1, '2020-01-01 00:00:00'),
(1002, 100, '用户新增', 'F', 'SYSTEM_USER_ADD', NULL, NULL, NULL, 'system:user:add', 1, 0, 0, 2, 1, '0,1,100', 3, NULL, 1, '2020-01-01 00:00:00'),
(1003, 100, '用户修改', 'F', 'SYSTEM_USER_EDIT', NULL, NULL, NULL, 'system:user:edit', 1, 0, 0, 3, 1, '0,1,100', 3, NULL, 1, '2020-01-01 00:00:00'),
(1004, 100, '用户删除', 'F', 'SYSTEM_USER_DELETE', NULL, NULL, NULL, 'system:user:delete', 1, 0, 0, 4, 1, '0,1,100', 3, NULL, 1, '2020-01-01 00:00:00'),
(1005, 100, '用户导出', 'F', 'SYSTEM_USER_EXPORT', NULL, NULL, NULL, 'system:user:export', 1, 0, 0, 5, 1, '0,1,100', 3, NULL, 1, '2020-01-01 00:00:00'),
(1006, 100, '用户导入', 'F', 'SYSTEM_USER_IMPORT', NULL, NULL, NULL, 'system:user:import', 1, 0, 0, 6, 1, '0,1,100', 3, NULL, 1, '2020-01-01 00:00:00'),
(1007, 100, '重置密码', 'F', 'SYSTEM_USER_RESET_PASSWORD', NULL, NULL, NULL, 'system:user:resetPassword', 1, 0, 0, 7, 1, '0,1,100', 3, NULL, 1, '2020-01-01 00:00:00');

-- 插入权限数据
INSERT INTO `sys_permission` (`id`, `permission_name`, `permission_code`, `permission_type`, `resource_type`, `resource_path`, `http_method`, `description`, `status`, `create_by`, `create_time`) VALUES
(1, '用户查询权限', 'user:query', 1, 'API', '/api/user/list', 'GET', '查询用户列表', 1, 1, '2020-01-01 00:00:00'),
(2, '用户新增权限', 'user:add', 1, 'API', '/api/user/add', 'POST', '新增用户', 1, 1, '2020-01-01 00:00:00'),
(3, '用户修改权限', 'user:edit', 1, 'API', '/api/user/edit', 'PUT', '修改用户', 1, 1, '2020-01-01 00:00:00'),
(4, '用户删除权限', 'user:delete', 1, 'API', '/api/user/delete', 'DELETE', '删除用户', 1, 1, '2020-01-01 00:00:00'),
(5, '角色查询权限', 'role:query', 1, 'API', '/api/role/list', 'GET', '查询角色列表', 1, 1, '2020-01-01 00:00:00'),
(6, '角色新增权限', 'role:add', 1, 'API', '/api/role/add', 'POST', '新增角色', 1, 1, '2020-01-01 00:00:00'),
(7, '角色修改权限', 'role:edit', 1, 'API', '/api/role/edit', 'PUT', '修改角色', 1, 1, '2020-01-01 00:00:00'),
(8, '角色删除权限', 'role:delete', 1, 'API', '/api/role/delete', 'DELETE', '删除角色', 1, 1, '2020-01-01 00:00:00'),
(9, '菜单查询权限', 'menu:query', 1, 'API', '/api/menu/list', 'GET', '查询菜单列表', 1, 1, '2020-01-01 00:00:00'),
(10, '菜单新增权限', 'menu:add', 1, 'API', '/api/menu/add', 'POST', '新增菜单', 1, 1, '2020-01-01 00:00:00'),
(11, '菜单修改权限', 'menu:edit', 1, 'API', '/api/menu/edit', 'PUT', '修改菜单', 1, 1, '2020-01-01 00:00:00'),
(12, '菜单删除权限', 'menu:delete', 1, 'API', '/api/menu/delete', 'DELETE', '删除菜单', 1, 1, '2020-01-01 00:00:00');

-- 用户角色关联
INSERT INTO `sys_user_role` (`user_id`, `role_id`, `create_time`) VALUES
(1, 1, '2020-01-01 00:00:00'),
(2, 2, '2020-02-01 00:00:00'),
(3, 2, '2020-03-01 00:00:00'),
(4, 3, '2020-04-01 00:00:00'),
(5, 3, '2020-05-01 00:00:00');

-- 角色菜单关联
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `create_time`) VALUES
(1, 1, '2020-01-01 00:00:00'),
(1, 2, '2020-01-01 00:00:00'),
(1, 3, '2020-01-01 00:00:00'),
(1, 100, '2020-01-01 00:00:00'),
(1, 101, '2020-01-01 00:00:00'),
(1, 102, '2020-01-01 00:00:00'),
(1, 103, '2020-01-01 00:00:00'),
(1, 104, '2020-01-01 00:00:00'),
(1, 105, '2020-01-01 00:00:00'),
(1, 106, '2020-01-01 00:00:00'),
(1, 107, '2020-01-01 00:00:00'),
(1, 108, '2020-01-01 00:00:00'),
(1, 109, '2020-01-01 00:00:00'),
(1, 200, '2020-01-01 00:00:00'),
(1, 201, '2020-01-01 00:00:00'),
(1, 300, '2020-01-01 00:00:00'),
(1, 1001, '2020-01-01 00:00:00'),
(1, 1002, '2020-01-01 00:00:00'),
(1, 1003, '2020-01-01 00:00:00'),
(1, 1004, '2020-01-01 00:00:00'),
(1, 1005, '2020-01-01 00:00:00'),
(1, 1006, '2020-01-01 00:00:00'),
(1, 1007, '2020-01-01 00:00:00'),
(2, 1, '2020-01-01 00:00:00'),
(2, 2, '2020-01-01 00:00:00'),
(2, 3, '2020-01-01 00:00:00'),
(2, 100, '2020-01-01 00:00:00'),
(2, 101, '2020-01-01 00:00:00'),
(2, 103, '2020-01-01 00:00:00'),
(2, 200, '2020-01-01 00:00:00'),
(2, 300, '2020-01-01 00:00:00'),
(2, 1001, '2020-01-01 00:00:00'),
(2, 1002, '2020-01-01 00:00:00'),
(2, 1003, '2020-01-01 00:00:00'),
(3, 2, '2020-01-01 00:00:00'),
(3, 3, '2020-01-01 00:00:00'),
(3, 200, '2020-01-01 00:00:00'),
(3, 201, '2020-01-01 00:00:00'),
(3, 300, '2020-01-01 00:00:00');

-- 角色权限关联
INSERT INTO `sys_role_permission` (`role_id`, `permission_id`, `create_time`) VALUES
(1, 1, '2020-01-01 00:00:00'),
(1, 2, '2020-01-01 00:00:00'),
(1, 3, '2020-01-01 00:00:00'),
(1, 4, '2020-01-01 00:00:00'),
(1, 5, '2020-01-01 00:00:00'),
(1, 6, '2020-01-01 00:00:00'),
(1, 7, '2020-01-01 00:00:00'),
(1, 8, '2020-01-01 00:00:00'),
(1, 9, '2020-01-01 00:00:00'),
(1, 10, '2020-01-01 00:00:00'),
(1, 11, '2020-01-01 00:00:00'),
(1, 12, '2020-01-01 00:00:00'),
(2, 1, '2020-01-01 00:00:00'),
(2, 2, '2020-01-01 00:00:00'),
(2, 3, '2020-01-01 00:00:00'),
(2, 5, '2020-01-01 00:00:00'),
(2, 6, '2020-01-01 00:00:00'),
(2, 7, '2020-01-01 00:00:00');

-- 用户部门关联
INSERT INTO `sys_user_dept` (`user_id`, `dept_id`, `is_primary`, `create_time`) VALUES
(1, 1, 1, '2020-01-01 00:00:00'),
(2, 2, 1, '2020-02-01 00:00:00'),
(3, 3, 1, '2020-03-01 00:00:00'),
(4, 6, 1, '2020-04-01 00:00:00'),
(5, 4, 1, '2020-05-01 00:00:00'),
(2, 5, 0, '2020-02-01 00:00:00');

-- 插入字典类型
INSERT INTO `sys_dict_type` (`id`, `dict_name`, `dict_code`, `dict_type`, `is_system`, `status`, `remark`, `create_by`, `create_time`) VALUES
(1, '用户性别', 'sys_user_gender', 'SYSTEM', 1, 1, '用户性别字典', 1, '2020-01-01 00:00:00'),
(2, '用户状态', 'sys_user_status', 'SYSTEM', 1, 1, '用户状态字典', 1, '2020-01-01 00:00:00'),
(3, '部门类型', 'sys_dept_type', 'SYSTEM', 1, 1, '部门类型字典', 1, '2020-01-01 00:00:00'),
(4, '角色类型', 'sys_role_type', 'SYSTEM', 1, 1, '角色类型字典', 1, '2020-01-01 00:00:00'),
(5, '菜单类型', 'sys_menu_type', 'SYSTEM', 1, 1, '菜单类型字典', 1, '2020-01-01 00:00:00'),
(6, '通知类型', 'sys_notice_type', 'SYSTEM', 1, 1, '通知类型字典', 1, '2020-01-01 00:00:00'),
(7, '消息类型', 'sys_message_type', 'SYSTEM', 1, 1, '消息类型字典', 1, '2020-01-01 00:00:00'),
(8, '存储类型', 'sys_storage_type', 'SYSTEM', 1, 1, '存储类型字典', 1, '2020-01-01 00:00:00');

-- 插入字典数据
INSERT INTO `sys_dict_data` (`id`, `dict_type_id`, `dict_label`, `dict_value`, `dict_sort`, `css_class`, `list_class`, `is_default`, `status`, `remark`, `create_by`, `create_time`) VALUES
(1, 1, '未知', '0', 1, '', 'default', 1, 1, '未知性别', 1, '2020-01-01 00:00:00'),
(2, 1, '男', '1', 2, '', 'primary', 0, 1, '男性', 1, '2020-01-01 00:00:00'),
(3, 1, '女', '2', 3, '', 'danger', 0, 1, '女性', 1, '2020-01-01 00:00:00'),
(4, 2, '禁用', '0', 1, '', 'danger', 0, 1, '禁用状态', 1, '2020-01-01 00:00:00'),
(5, 2, '启用', '1', 2, '', 'primary', 1, 1, '启用状态', 1, '2020-01-01 00:00:00'),
(6, 3, '公司', '1', 1, '', 'primary', 0, 1, '公司类型', 1, '2020-01-01 00:00:00'),
(7, 3, '部门', '2', 2, '', 'success', 0, 1, '部门类型', 1, '2020-01-01 00:00:00'),
(8, 3, '小组', '3', 3, '', 'info', 0, 1, '小组类型', 1, '2020-01-01 00:00:00'),
(9, 4, '系统角色', '1', 1, '', 'primary', 0, 1, '系统角色', 1, '2020-01-01 00:00:00'),
(10, 4, '业务角色', '2', 2, '', 'success', 0, 1, '业务角色', 1, '2020-01-01 00:00:00'),
(11, 5, '目录', 'M', 1, '', 'primary', 0, 1, '目录菜单', 1, '2020-01-01 00:00:00'),
(12, 5, '菜单', 'C', 2, '', 'success', 0, 1, '菜单项', 1, '2020-01-01 00:00:00'),
(13, 5, '按钮', 'F', 3, '', 'info', 0, 1, '按钮权限', 1, '2020-01-01 00:00:00'),
(14, 6, '通知', 'INFO', 1, '', 'primary', 0, 1, '通知', 1, '2020-01-01 00:00:00'),
(15, 6, '公告', 'ANNOUNCEMENT', 2, '', 'success', 0, 1, '公告', 1, '2020-01-01 00:00:00'),
(16, 6, '警告', 'WARNING', 3, '', 'warning', 0, 1, '警告', 1, '2020-01-01 00:00:00'),
(17, 6, '紧急', 'URGENT', 4, '', 'danger', 0, 1, '紧急', 1, '2020-01-01 00:00:00'),
(18, 7, '系统消息', 'SYSTEM', 1, '', 'primary', 0, 1, '系统消息', 1, '2020-01-01 00:00:00'),
(19, 7, '用户消息', 'USER', 2, '', 'success', 0, 1, '用户消息', 1, '2020-01-01 00:00:00'),
(20, 7, '通知消息', 'NOTICE', 3, '', 'info', 0, 1, '通知消息', 1, '2020-01-01 00:00:00'),
(21, 7, '提醒消息', 'REMINDER', 4, '', 'warning', 0, 1, '提醒消息', 1, '2020-01-01 00:00:00'),
(22, 8, '本地存储', 'LOCAL', 1, '', 'primary', 1, 1, '本地存储', 1, '2020-01-01 00:00:00'),
(23, 8, '阿里云OSS', 'OSS', 2, '', 'success', 0, 1, '阿里云OSS', 1, '2020-01-01 00:00:00'),
(24, 8, '腾讯云COS', 'COS', 3, '', 'info', 0, 1, '腾讯云COS', 1, '2020-01-01 00:00:00'),
(25, 8, 'MinIO', 'MINIO', 4, '', 'warning', 0, 1, 'MinIO', 1, '2020-01-01 00:00:00');

-- 插入系统参数
INSERT INTO `sys_config` (`id`, `config_name`, `config_key`, `config_value`, `config_type`, `is_system`, `is_encrypted`, `remark`, `create_by`, `create_time`) VALUES
(1, '系统名称', 'sys.name', '示例管理系统', 1, 1, 0, '系统名称', 1, '2020-01-01 00:00:00'),
(2, '系统版本', 'sys.version', '1.0.0', 1, 1, 0, '系统版本', 1, '2020-01-01 00:00:00'),
(3, '用户默认密码', 'user.default.password', '123456', 1, 1, 1, '用户默认密码', 1, '2020-01-01 00:00:00'),
(4, '登录验证码开关', 'login.captcha.enabled', 'true', 1, 1, 0, '是否开启登录验证码', 1, '2020-01-01 00:00:00'),
(5, '登录失败锁定次数', 'login.max.retry.count', '5', 1, 1, 0, '登录失败最大重试次数', 1, '2020-01-01 00:00:00'),
(6, '文件上传路径', 'file.upload.path', '/upload', 1, 1, 0, '文件上传路径', 1, '2020-01-01 00:00:00'),
(7, '文件上传大小限制', 'file.upload.max.size', '104857600', 1, 1, 0, '文件上传大小限制（字节）', 1, '2020-01-01 00:00:00'),
(8, '会话超时时间', 'session.timeout', '7200', 1, 1, 0, '会话超时时间（秒）', 1, '2020-01-01 00:00:00');

-- 插入通知公告
INSERT INTO `sys_notice` (`id`, `notice_title`, `notice_type`, `notice_content`, `target_type`, `status`, `is_top`, `publish_time`, `publisher_id`, `publisher_name`, `remark`, `create_by`, `create_time`) VALUES
(1, '欢迎使用示例管理系统', 'ANNOUNCEMENT', '欢迎使用示例管理系统，本系统提供了完整的权限管理、组织架构、日志审计等功能。', 1, 1, 1, '2020-01-01 10:00:00', 1, '系统管理员', '系统公告', 1, '2020-01-01 00:00:00'),
(2, '系统维护通知', 'WARNING', '系统将于2020-01-02 00:00-02:00进行维护，请提前做好数据备份。', 1, 1, 0, '2020-01-01 15:00:00', 1, '系统管理员', '维护通知', 1, '2020-01-01 00:00:00'),
(3, '功能更新说明', 'INFO', '本次更新新增了定时任务功能，优化了系统性能，修复了若干bug。', 1, 1, 0, '2020-01-01 18:00:00', 1, '系统管理员', '更新说明', 1, '2020-01-01 00:00:00');

-- 公告阅读记录
INSERT INTO `sys_notice_read` (`notice_id`, `user_id`, `read_time`) VALUES
(1, 1, '2020-01-01 10:05:00'),
(1, 2, '2020-01-01 10:10:00'),
(1, 3, '2020-01-01 10:15:00'),
(2, 1, '2020-01-01 15:05:00'),
(2, 2, '2020-01-01 15:10:00');

-- 插入消息
INSERT INTO `sys_message` (`id`, `sender_id`, `sender_name`, `receiver_id`, `receiver_name`, `message_type`, `message_title`, `message_content`, `business_type`, `is_read`, `read_time`, `send_time`, `create_time`) VALUES
(1, 1, '系统管理员', 2, '张三', 'SYSTEM', '欢迎使用', '欢迎加入示例管理系统，请及时修改初始密码。', NULL, 1, '2020-02-01 09:00:00', '2020-02-01 08:00:00', '2020-02-01 08:00:00'),
(2, 1, '系统管理员', 3, '李四', 'SYSTEM', '欢迎使用', '欢迎加入示例管理系统，请及时修改初始密码。', NULL, 1, '2020-03-01 09:00:00', '2020-03-01 08:00:00', '2020-03-01 08:00:00'),
(3, 1, '系统管理员', 4, '王五', 'SYSTEM', '欢迎使用', '欢迎加入示例管理系统，请及时修改初始密码。', NULL, 0, NULL, '2020-04-01 08:00:00', '2020-04-01 08:00:00'),
(4, 2, '张三', 1, '系统管理员', 'USER', '请假申请', '申请请假一天，请审批。', 'LEAVE', 0, NULL, '2020-06-01 10:00:00', '2020-06-01 10:00:00');

-- 插入文件信息
INSERT INTO `sys_file` (`id`, `file_name`, `file_path`, `file_size`, `file_type`, `file_ext`, `mime_type`, `file_md5`, `storage_type`, `upload_user_id`, `upload_user_name`, `business_type`, `business_id`, `is_image`, `width`, `height`, `thumbnail_path`, `status`, `create_time`) VALUES
(1, 'example.jpg', '/upload/2020/01/01/example.jpg', 102400, 'image/jpeg', 'jpg', 'image/jpeg', 'abc123def456', 'LOCAL', 1, '系统管理员', 'AVATAR', 1, 1, 800, 600, '/upload/2020/01/01/example_thumb.jpg', 1, '2020-01-01 10:00:00'),
(2, 'document.pdf', '/upload/2020/01/01/document.pdf', 204800, 'application/pdf', 'pdf', 'application/pdf', 'def456ghi789', 'LOCAL', 2, '张三', 'DOCUMENT', 1, 0, NULL, NULL, NULL, 1, '2020-02-01 10:00:00'),
(3, 'data.xlsx', '/upload/2020/01/01/data.xlsx', 51200, 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet', 'xlsx', 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet', 'ghi789jkl012', 'LOCAL', 3, '李四', 'DATA', 1, 0, NULL, NULL, NULL, 1, '2020-03-01 10:00:00');

-- 插入定时任务
INSERT INTO `sys_job` (`id`, `job_name`, `job_group`, `job_class`, `cron_expression`, `job_description`, `job_params`, `misfire_policy`, `concurrent`, `status`, `execute_times`, `last_execute_time`, `next_execute_time`, `remark`, `create_by`, `create_time`) VALUES
(1, '清理过期日志', 'DEFAULT', 'com.example.job.CleanExpiredLogJob', '0 0 2 * * ?', '每天凌晨2点清理过期日志', '{"days":30}', 2, 0, 1, 150, '2020-06-15 02:00:00', '2020-06-16 02:00:00', '清理30天前的日志', 1, '2020-01-01 00:00:00'),
(2, '数据统计', 'DEFAULT', 'com.example.job.DataStatisticsJob', '0 0 1 * * ?', '每天凌晨1点统计数据', '{}', 2, 0, 1, 150, '2020-06-15 01:00:00', '2020-06-16 01:00:00', '统计前一天的数据', 1, '2020-01-01 00:00:00'),
(3, '发送提醒邮件', 'DEFAULT', 'com.example.job.SendReminderEmailJob', '0 0 9 * * ?', '每天早上9点发送提醒邮件', '{}', 2, 0, 0, 0, NULL, '2020-06-16 09:00:00', '发送提醒邮件', 1, '2020-01-01 00:00:00');

-- 插入登录日志
INSERT INTO `sys_login_log` (`id`, `user_id`, `username`, `login_type`, `login_status`, `ip_address`, `location`, `device_type`, `device_name`, `browser_type`, `browser_version`, `os_type`, `os_version`, `login_time`, `logout_time`, `online_duration`, `create_time`) VALUES
(1, 1, 'admin', 'PASSWORD', 1, '192.168.1.100', '北京市', 'PC', 'Windows PC', 'Chrome', '90.0', 'Windows', '10', '2020-06-15 09:00:00', '2020-06-15 18:00:00', 32400, '2020-06-15 09:00:00'),
(2, 2, 'zhangsan', 'PASSWORD', 1, '192.168.1.101', '北京市', 'PC', 'Windows PC', 'Firefox', '88.0', 'Windows', '10', '2020-06-15 09:30:00', '2020-06-15 17:30:00', 28800, '2020-06-15 09:30:00'),
(3, 3, 'lisi', 'PASSWORD', 1, '192.168.1.102', '北京市', 'MOBILE', 'iPhone', 'Safari', '14.0', 'iOS', '14.5', '2020-06-15 10:00:00', NULL, NULL, '2020-06-15 10:00:00'),
(4, 4, 'wangwu', 'PASSWORD', 0, '192.168.1.103', '北京市', 'PC', 'Windows PC', 'Chrome', '90.0', 'Windows', '10', '2020-06-15 10:30:00', NULL, NULL, '2020-06-15 10:30:00'),
(5, 1, 'admin', 'PASSWORD', 1, '192.168.1.100', '北京市', 'PC', 'Windows PC', 'Chrome', '90.0', 'Windows', '10', '2020-06-16 08:00:00', '2020-06-16 17:00:00', 32400, '2020-06-16 08:00:00');

-- 插入操作日志
INSERT INTO `sys_oper_log` (`id`, `user_id`, `username`, `real_name`, `module_name`, `business_type`, `method_name`, `request_method`, `request_url`, `request_params`, `oper_status`, `cost_time`, `ip_address`, `location`, `browser_type`, `browser_version`, `os_type`, `os_version`, `oper_time`, `create_time`) VALUES
(1, 1, 'admin', '系统管理员', '用户管理', '查询用户', 'queryUserList', 'GET', '/api/user/list', '{"pageNum":1,"pageSize":10}', 1, 50, '192.168.1.100', '北京市', 'Chrome', '90.0', 'Windows', '10', '2020-06-15 09:15:00', '2020-06-15 09:15:00'),
(2, 1, 'admin', '系统管理员', '用户管理', '新增用户', 'addUser', 'POST', '/api/user/add', '{"username":"testuser","password":"123456"}', 1, 100, '192.168.1.100', '北京市', 'Chrome', '90.0', 'Windows', '10', '2020-06-15 09:20:00', '2020-06-15 09:20:00'),
(3, 2, 'zhangsan', '张三', '角色管理', '查询角色', 'queryRoleList', 'GET', '/api/role/list', '{"pageNum":1,"pageSize":10}', 1, 45, '192.168.1.101', '北京市', 'Firefox', '88.0', 'Windows', '10', '2020-06-15 09:45:00', '2020-06-15 09:45:00'),
(4, 2, 'zhangsan', '张三', '角色管理', '修改角色', 'editRole', 'PUT', '/api/role/edit', '{"id":2,"roleName":"管理员"}', 0, 200, '192.168.1.101', '北京市', 'Firefox', '88.0', 'Windows', '10', '2020-06-15 09:50:00', '2020-06-15 09:50:00'),
(5, 3, 'lisi', '李四', '菜单管理', '查询菜单', 'queryMenuList', 'GET', '/api/menu/list', '{}', 1, 30, '192.168.1.102', '北京市', 'Safari', '14.0', 'iOS', '14.5', '2020-06-15 10:15:00', '2020-06-15 10:15:00');

-- 插入定时任务执行日志
INSERT INTO `sys_job_log` (`id`, `job_id`, `job_name`, `job_group`, `job_class`, `execute_time`, `execute_status`, `execute_result`, `cost_time`, `create_time`) VALUES
(1, 1, '清理过期日志', 'DEFAULT', 'com.example.job.CleanExpiredLogJob', '2020-06-15 02:00:00', 1, '清理了1000条过期日志', 5000, '2020-06-15 02:00:00'),
(2, 2, '数据统计', 'DEFAULT', 'com.example.job.DataStatisticsJob', '2020-06-15 01:00:00', 1, '统计数据完成', 10000, '2020-06-15 01:00:00'),
(3, 1, '清理过期日志', 'DEFAULT', 'com.example.job.CleanExpiredLogJob', '2020-06-14 02:00:00', 1, '清理了800条过期日志', 4500, '2020-06-14 02:00:00'),
(4, 2, '数据统计', 'DEFAULT', 'com.example.job.DataStatisticsJob', '2020-06-14 01:00:00', 1, '统计数据完成', 9500, '2020-06-14 01:00:00'),
(5, 1, '清理过期日志', 'DEFAULT', 'com.example.job.CleanExpiredLogJob', '2020-06-13 02:00:00', 0, '清理失败：数据库连接超时', 3000, '2020-06-13 02:00:00');