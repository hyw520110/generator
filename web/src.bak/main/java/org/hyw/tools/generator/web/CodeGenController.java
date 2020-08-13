package org.hyw.tools.generator.web;

import java.util.Arrays;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.ibatis.reflection.ArrayUtil;
import org.hyw.tools.generator.Generator;
import org.hyw.tools.generator.conf.GlobalConf;
import org.hyw.tools.generator.conf.dao.DataSourceConf;
import org.hyw.tools.generator.enums.Component;
import org.hyw.tools.generator.enums.ProjectBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SimplePropertyPreFilter;
import com.big.box.demo.api.vo.Result;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

/**
 * 代码生成器
 * 
 * @author heyw
 * @since 2020-05-26
 * @copyright: hyw Copyright (c) 2017-2020 All Rights Reserved.
 */
@Api(value = "代码生成器", tags = "代码生成器")
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
	@ApiOperation(value = "获取所有数据库表", notes = "获取所有数据库表")
	public Result<String> getTables(String ipAndPort, String dbName, String username, String pwd, String include,
			String exclude) {
		DataSourceConf ds = generator.getDataSource();
		generator.getGlobal().setInclude(StringUtils.isNotBlank(include) ? include.split(",") : null);
		generator.getGlobal().setExclude(StringUtils.isNotBlank(exclude) ? exclude.split(",") : null);
		if (StringUtils.isBlank(ipAndPort)) {
			return toJson();
		}
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
	@ApiOperation(value = "代码生成之全局设置", notes = "代码生成之全局设置")
	public Result<Object> step1(String outputDir, String description, String rootPackage, String modules, String tablePrefix,
			boolean delOutputDir,boolean fileOverride,boolean openDir) {
		GlobalConf global = generator.getGlobal();
		global.setOutputDir(outputDir);
		global.setDescription(description);
		global.setRootPackage(rootPackage);
		global.setModules(StringUtils.split(modules, ","));
		global.setTablePrefix(StringUtils.split(tablePrefix, ","));
		global.setDelOutputDir(delOutputDir);
		global.setFileOverride(fileOverride);
		global.setOpenDir(openDir);
		return Result.ok();
	}

	@PostMapping("/step2")
	@ApiOperation(value = "代码生成之全局设置", notes = "代码生成之全局设置")
	public Result<Object> step2(String view, String projectBuilder, String springBootVersion, String springCloudAlibabaVersion,
			String dubboVersion, String connectString, String redisHost, String redisPassword, String sentinelAddr,
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
		map.get(Component.SPRINGCLOUD).put("springcloud_alibaba_version", springCloudAlibabaVersion);
		map.get(Component.DUBBO).put(Component.DUBBO.name().toLowerCase()+"_version", dubboVersion);
		map.get(Component.ZOOKEEPER).put("connect-string", connectString);
		map.get(Component.REDIS).put("spring_redis_cluster_nodes", redisHost);
		map.get(Component.REDIS).put("spring_redis_password", redisPassword);
		map.get(Component.SENTINEL).put("dashboard.server", sentinelAddr);
		map.get(Component.SKYWALKING).put("skywalking.addr", skywalkingAddr);
		return Result.ok();
	}

	@PostMapping("/exec")
	@ApiOperation(value = "生成代码", notes = "生成代码")
	public Result<?> exec(String tabName) {
		generator.getGlobal().setInclude(StringUtils.isNotBlank(tabName) ? tabName.split(",") : null);
		generator.execute();
		return Result.ok();
	}
}
