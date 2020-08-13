package org.hyw.tools.generator.web.controller;

import java.util.Map;

import javax.annotation.PostConstruct;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.hyw.tools.generator.Generator;
import org.hyw.tools.generator.conf.GlobalConf;
import org.hyw.tools.generator.conf.dao.DataSourceConf;
import org.hyw.tools.generator.enums.Component;
import org.hyw.tools.generator.enums.ProjectBuilder;
import org.hyw.tools.generator.web.model.Result;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SimplePropertyPreFilter;


/**
 * 代码生成器
 * 
 * @author heyw
 * @since 2020-05-26
 * @copyright: hyw Copyright (c) 2017-2020 All Rights Reserved.
 */
@RestController
@RequestMapping("/v1/gen")
public class CodeGenController {

	@Value("${spring.datasource.url}")
	private String jdbcUrl;
	@Value("${spring.datasource.username}")
	private String usr;
	@Value("${spring.datasource.password}")
	private String pwd;
	private Generator generator;

	@PostConstruct
	public void init() {
		generator = Generator.getInstance();
		DataSourceConf ds = generator.getDataSource();
		ds.setUrl(jdbcUrl);
		ds.setUsername(usr);
		ds.setPassword(pwd);
	}

	@PostMapping("/tables")
	public Result<String> getTables(String ipAndPort, String dbName, String username, String pwd, String include,
			String exclude,String tablePrefix) {
		if(StringUtils.isNotBlank(tablePrefix)) {
			generator.getGlobal().setTablePrefix(StringUtils.split(tablePrefix, ","));	
		}
		generator.getGlobal().setMatchMode(true);
		generator.getGlobal().setInclude(StringUtils.isNotBlank(include) ? include.split(",") : null);
		generator.getGlobal().setExclude(StringUtils.isNotBlank(exclude) ? exclude.split(",") : null);
		if (StringUtils.isBlank(ipAndPort)) {
			return toJson();
		}
		DataSourceConf ds = generator.getDataSource();
		ds.setIpAndPort(ipAndPort);
		ds.setDbName(dbName);
		ds.setUsername(username);
		ds.setPwd(pwd);
		return toJson();
	}

	/**
	 * 序列化客户端所需数据
	 * 
	 * @return
	 */
	private Result<String> toJson() {
		return new Result<>(JSON.toJSONString(generator, new SimplePropertyPreFilter("dataSource", "ipAndPort",
				"dbName", "username", "pwd", "tables", "name", "comment", "createTime")));
	}

	@PostMapping("/step1")
	public Result<Object> step1(String outputDir, String description, String rootPackage, String modules, 
			boolean delOutputDir,boolean fileOverride,boolean openDir) {
		GlobalConf global = generator.getGlobal();
		global.setOutputDir(outputDir);
		global.setDescription(description);
		global.setRootPackage(rootPackage);
		global.setModules(StringUtils.split(modules, ","));
		
		global.setDelOutputDir(delOutputDir);
		global.setFileOverride(fileOverride);
		global.setOpenDir(openDir);
		return Result.ok();
	}

	@PostMapping("/step2")
	public Result<Object> step2(String view, String projectBuilder, String springBootVersion, String springCloudVersion,String springCloudAlibabaVersion,
			String dubboVersion, String mybatisType,String connectString, String redisHost, String redisPassword, String sentinelAddr,
			String skywalkingAddr, String secure) {
		GlobalConf global = generator.getGlobal();
		ArrayUtils.removeElement(global.getComponents(), Component.VUE);
		ArrayUtils.removeElement(global.getComponents(), Component.THYMELEAF);
		Component viewComponent = Component.getComonent(view);
		global.setComponents((Component[]) ArrayUtils.add(global.getComponents(), viewComponent));
		if(viewComponent==Component.VUE) {
			global.setComponents((Component[]) ArrayUtils.add(global.getComponents(), Component.SHIRO));
			global.setComponents((Component[]) ArrayUtils.add(global.getComponents(), Component.JWT));
		}
		global.setProjectBuilder(ProjectBuilder.valueOf(projectBuilder));
		Map<Component, Map<String, String>> map = generator.getComponents();
		map.get(Component.SPRINGBOOT).put(Component.SPRINGBOOT.name().toLowerCase()+"_version", springBootVersion);
		map.get(Component.SPRINGCLOUD).put(Component.SPRINGCLOUD.name().toLowerCase()+"_version", springCloudVersion);
		map.get(Component.SPRINGCLOUD).put("springcloud_alibaba_version", springCloudAlibabaVersion);
		if(StringUtils.isBlank(dubboVersion)) {
			ArrayUtils.removeElement(global.getComponents(), Component.DUBBO);
		}else {
			map.get(Component.DUBBO).put(Component.DUBBO.name().toLowerCase()+"_version", dubboVersion);	
		}
		map.get(Component.MYBATIS).put("mapperType", mybatisType);	
		map.get(Component.ZOOKEEPER).put("connect-string", connectString);
		map.get(Component.REDIS).put("spring_redis_cluster_nodes", redisHost);
		map.get(Component.REDIS).put("spring_redis_password", redisPassword);
		map.get(Component.SENTINEL).put("dashboard.server", sentinelAddr);
		map.get(Component.SKYWALKING).put("skywalking.addr", skywalkingAddr);
		return Result.ok();
	}

	@PostMapping("/exec")
	public Result<?> exec(String tabName) {
		generator.getGlobal().setInclude(StringUtils.isBlank(tabName)?null:tabName.split(","));
		generator.getGlobal().setMatchMode(false);
		generator.execute();
		return Result.ok();
	}
}
