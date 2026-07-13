package ${packagePath}.${projectName}.${moduleName}.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import ${packagePath}.${projectName}.${moduleName}.mapper.${beanName}Mapper;

@ExtendWith(MockitoExtension.class)
public class ${beanName}ServiceTest {

    @InjectMocks
    private ${beanName}ServiceImpl ${entityNameLower}Service;

    @Mock
    private ${beanName}Mapper ${entityNameLower}Mapper;

    @Test
    public void testContextLoads() {
        assertNotNull(${entityNameLower}Service);
    }
}
