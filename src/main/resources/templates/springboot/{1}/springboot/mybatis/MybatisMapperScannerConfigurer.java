package ${mybatisPackage};

import org.mybatis.spring.mapper.MapperScannerConfigurer;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@AutoConfigureAfter({MybatisAutoConfigurer.class})
public class MybatisMapperScannerConfigurer  {
    
    @Bean
    public MapperScannerConfigurer mapperScanner() {
        MapperScannerConfigurer scanner=new MapperScannerConfigurer();
        scanner.setSqlSessionFactoryBeanName("sqlSessionFactory");
        scanner.setBasePackage("$!{mapperPackage}");
        return scanner;
    }

     

}
