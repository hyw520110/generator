package ${controllerPackage};

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import ${rootPackage}.${projectName}#if($!{moduleName}).${moduleName}#end.controller.${table.beanName}Controller;
import ${rootPackage}.${projectName}#if($!{moduleName}).${moduleName}#end.Booter;

@RunWith(SpringRunner.class)
//@ContextConfiguration or @SpringBootTest(classes=Booter.class)
@SpringBootTest(classes = Booter.class)
public class ${className}   {
    
    @Autowired
    private ${table.beanName}Controller controller;
    private MockMvc mock;
    
    @Before
    public void setUp() throws Exception {
        mock = MockMvcBuilders.standaloneSetup(controller).build();
    }
    
    @Test
    public void testView() throws Exception {
        MvcResult result = mock.perform(MockMvcRequestBuilders.get("/${table.beanName}/view/{id}/view",3))  
                .andExpect(MockMvcResultMatchers.view().name("${table.beanName}/view"))  
                .andExpect(MockMvcResultMatchers.model().attributeExists("bean"))
                .andExpect(status().isOk())
                .andDo(MockMvcResultHandlers.print())  
                .andReturn();  
          
        Assert.assertNotNull(result.getModelAndView().getModel().get("bean"));  
    }
    
}
