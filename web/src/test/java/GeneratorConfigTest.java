import java.util.Arrays;

import org.hyw.tools.generator.Generator;
import org.hyw.tools.generator.conf.GlobalConf;
import org.hyw.tools.generator.conf.dao.DataSourceConf;
import org.junit.Test;

import lombok.extern.slf4j.Slf4j;

/**
 * Generator 配置测试用例
 * 输出 Generator 实例的关键配置信息
 */
@Slf4j
public class GeneratorConfigTest {

    @Test
    public void testGenerator() {
        try {
            log.info("获取 Generator 实例...");
            Generator generator = Generator.getInstance();
            
            if (generator == null) {
                log.error("Generator 实例为空");
                throw new RuntimeException("Generator 实例为空");
            }
            
            log.info("获取数据源配置...");
            DataSourceConf ds = generator.getDataSource();
            
            if (ds == null) {
                log.error("数据源配置为空");
                throw new RuntimeException("数据源配置为空");
            }
            
            log.info("原始数据库名: {}", ds.getDbName());
            ds.setDbName("db_20260317202850");
            generator.setDataSource(ds);
            log.info("修改后数据库名: {}", ds.getDbName());
            
            // 输出全局配置
            printGlobalConfig(generator);

            // 输出输出配置
            printOutputConfig(generator);
            
            log.info("开始执行代码生成...");
            generator.execute();
            log.info("代码生成完成");
            
        } catch (Exception e) {
            log.error("执行失败: {}", e.getMessage(), e);
            throw new RuntimeException("测试执行失败", e);
        }
    }
 

    private void printGlobalConfig(Generator generator) {
        log.info("--- 全局配置 ---");
        GlobalConf global = generator.getGlobal();
        if (global != null) {
            log.info("项目名称: {}", global.getProjectName());
            log.info("根包名: {}", global.getRootPackage());
            log.info("生成目录: {}", global.getOutputDir());
            
            log.info("包含的表: {}", Arrays.toString(global.getInclude()));
            log.info("排除的表: {}", Arrays.toString(global.getExclude()));
            
            log.info("组件列表: {}", Arrays.toString(global.getComponents()));
        } else {
            log.warn("全局配置为空");
        }
    }

 

    private void printOutputConfig(Generator generator) {
        log.info("--- 输出配置 ---");
        GlobalConf global = generator.getGlobal();
        if (global != null) {
            log.info("输出目录: {}", global.getOutputDir());
            log.info("是否覆盖文件: {}", global.isFileOverride());
        } else {
            log.warn("输出配置为空");
        }
    }
}