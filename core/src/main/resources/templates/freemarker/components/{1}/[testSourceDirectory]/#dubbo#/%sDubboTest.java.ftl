<#if DUBBO?? && DUBBO>
package ${dubboPackage!};

import org.apache.dubbo.config.annotation.Reference;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import ${rootPackage!}.${projectName!}<#if moduleName?has_content>.${moduleName!}</#if>.Booter;
import ${servicePackage!}.${table.beanName!}Service;

//@ContextConfiguration or @SpringBootTest(classes=Booter.class)
@SpringBootTest(classes=Booter.class)
public class ${table.beanName!}DubboTest {

	@Reference
    private ${table.beanName!}Service service;
    
    @Test
    public void testDubbo() throws Exception {
        System.out.println(service.getClass());
    }
}
</#if>
