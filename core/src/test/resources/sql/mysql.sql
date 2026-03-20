CREATE TABLE person(  
  id int  PRIMARY KEY AUTO_INCREMENT comment '编号',  
  name VARCHAR(64) comment '姓名',  
  address VARCHAR(128) comment ' 地址',   
  tel VARCHAR(11) comment '电话'
) comment '会员';    
  
CREATE TABLE orders(  
  Id VARCHAR(36) PRIMARY KEY comment '主键',  
  Number VARCHAR(20) comment '编号',  
  Price INT comment '价格',  
  pid VARCHAR(36) comment '会员编号'  
) comment '订单';  
  
INSERT INTO person VALUES('001', 'Jack', 'Wuhan', '1234567');  
INSERT INTO orders VALUES('O_00001', '00001', 100, '001');  
INSERT INTO orders VALUES('O_00002', '00002', 200, '001');  
  
SELECT p.*, o.*  
FROM person p   
  JOIN orders o ON (p.Id=o.pid)  
WHERE p.Id = '001'  



CREATE TABLE `user` (
  `id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `username` varchar(64) NOT NULL DEFAULT '',
  `mobile` varchar(16) NOT NULL DEFAULT '',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8;

-- ----------------------------
-- Records of user
-- ----------------------------
INSERT INTO `user` VALUES ('1', 'yiibai', '13838009988');
INSERT INTO `user` VALUES ('2', 'User-name-1', '13838009988');



CREATE TABLE `group` (
  `group_id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `group_name` varchar(254) NOT NULL DEFAULT '',
  PRIMARY KEY (`group_id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8;

-- ----------------------------
-- Records of group


INSERT INTO `group` VALUES ('1', 'Group-1');
INSERT INTO `group` VALUES ('2', 'Group-2');

CREATE TABLE `user_group` (
  `user_id` int(10) unsigned NOT NULL DEFAULT '0',
  `group_id` int(10) unsigned NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- ----------------------------
-- Records of user_group
-- ----------------------------
INSERT INTO `user_group` VALUES ('1', '1');
INSERT INTO `user_group` VALUES ('2', '1');
INSERT INTO `user_group` VALUES ('1', '2');




-- http://www.yiibai.com/mybatis/mybatis-many2many.html
-- ----------------------------
INSERT INTO `group` VALUES ('1', 'Group-1');
INSERT INTO `group` VALUES ('2', 'Group-2');

