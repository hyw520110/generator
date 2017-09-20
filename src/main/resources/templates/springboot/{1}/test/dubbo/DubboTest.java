package ${dubboPackage};

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import com.alibaba.boot.dubbo.annotation.DubboConsumer;
import com.sun.tools.internal.ws.wsdl.document.Service;
import com.ycd360.backstage.api.service.AccountService;
import com.ycd360.backstage.app.Booter;

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
