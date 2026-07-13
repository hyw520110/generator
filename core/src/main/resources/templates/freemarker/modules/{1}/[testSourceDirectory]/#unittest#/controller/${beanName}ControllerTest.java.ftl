package ${packagePath}.${projectName}.${moduleName}.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import ${packagePath}.${projectName}.${moduleName}.service.${beanName}Service;

@ExtendWith(MockitoExtension.class)
public class ${beanName}ControllerTest {

    @InjectMocks
    private ${beanName}Controller ${entityNameLower}Controller;

    @Mock
    private ${beanName}Service ${entityNameLower}Service;

    @Test
    public void testContextLoads() {
        assertNotNull(${entityNameLower}Controller);
    }
}
