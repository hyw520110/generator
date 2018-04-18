package ${mybatisPackage};

import java.io.IOException;
import java.util.Properties;

import javax.sql.DataSource;

import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.mapper.MapperScannerConfigurer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.TransactionManagementConfigurer;

import com.github.pagehelper.PageInterceptor;

@Configuration("mybatis")
@EnableTransactionManagement
public class MybatisAutoConfigurer implements TransactionManagementConfigurer {
    @Autowired
    private DataSource dataSource;
    private String     mapperLocations="classpath:mappers/*.xml";
    private String     typeAliasesPackage="$!{entityPackage}";
 

    @Bean(name="sqlSessionFactory")
    public SqlSessionFactory sqlSessionFactory() {
        SqlSessionFactoryBean sqlSessionFactoryBean = new SqlSessionFactoryBean();
        sqlSessionFactoryBean.setDataSource(dataSource);
        sqlSessionFactoryBean.setTypeAliasesPackage(typeAliasesPackage);
        PageInterceptor page = new PageInterceptor();
        Properties properties = new Properties();
        //TODO 
        properties.setProperty("reasonable", "true");
        properties.setProperty("supportMethodsArguments", "true");
        page.setProperties(properties);
        sqlSessionFactoryBean.setPlugins(new Interceptor[] { page });
        try {
            sqlSessionFactoryBean.setMapperLocations(getMapperLocations());
            return sqlSessionFactoryBean.getObject();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Resource[] getMapperLocations() throws IOException { 
        return new PathMatchingResourcePatternResolver().getResources(mapperLocations);
    }

    @Bean
    @Override
    public PlatformTransactionManager annotationDrivenTransactionManager() {
        DataSourceTransactionManager dtm = new DataSourceTransactionManager(dataSource);
        dtm.setNestedTransactionAllowed(true);
        return dtm;
    }
    
    @Bean
    public SqlSessionTemplate sqlSessionTemplate(SqlSessionFactory sqlSessionFactory) {
        return new SqlSessionTemplate(sqlSessionFactory);
    }
}
