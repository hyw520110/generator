#if($!{DUBBO})
package ${dubboPackage};

import org.apache.dubbo.config.annotation.Reference;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import ${rootPackage}.${projectName}#if($!{moduleName}).${moduleName}#end.Booter;
import ${servicePackage}.${table.beanName}Service;

@RunWith(SpringRunner.class)
//@ContextConfiguration or @SpringBootTest(classes=Booter.class)
@SpringBootTest(classes=Booter.class)
public class ${table.beanName}DubboTest {

	@Reference
    private ${table.beanName}Service service;
    
    @Test
    public void testDubbo() throws Exception {
        System.out.println(service.getClass());
    }
}
#end
