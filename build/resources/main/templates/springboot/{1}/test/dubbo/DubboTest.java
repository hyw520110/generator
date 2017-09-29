package ${dubboPackage};

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import com.alibaba.boot.dubbo.annotation.DubboConsumer;
import ${rootPackage}.${projectName}.${moduleName}.Booter;
import ${servicePackage}.${table.beanName}Service;

@RunWith(SpringRunner.class)
//@ContextConfiguration or @SpringBootTest(classes=Booter.class)
@SpringBootTest(classes=Booter.class)
public class DubboTest {

    @DubboConsumer
    private ${table.beanName}Service service;
    
    @Test
    public void testDubbo() throws Exception {
        System.out.println(service.getClass());
    }
}
