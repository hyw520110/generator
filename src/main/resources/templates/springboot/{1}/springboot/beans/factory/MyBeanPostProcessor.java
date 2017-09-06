package ${factoryPackage};

import java.net.URL;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.thymeleaf.spring4.templateresolver.SpringResourceTemplateResolver;
 
/**
 * 
 * Filename:    MyBeanPostProcessor.java  
 * Description: 更改模板路径，用于开发环境实现模板热部署:
 * 修改模板后免编译刷新页面即时看效果，浏览器安装livereload插件免刷新实时看效果<br/>
 * 知识备忘:<br/>
 *   BeanPostProcessor实例化bean之后执行
 *   BeanFactoryPostProcessor在bean实例化之前执行 
 * Copyright:   Copyright (c) 2015-2018 All Rights Reserved.
 * Company:     ycd.cn Inc.
 * @author:     heyiwu 
 * @version:    1.0  
 * Create at:   2017年9月5日 上午10:57:39  
 *
 */
@Profile("dev")
@Component
public class MyBeanPostProcessor implements BeanPostProcessor {

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        return setSpringResourceTemplateResolverPrefix(bean);
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        return setSpringResourceTemplateResolverPrefix(bean);
    }
    
    private Object setSpringResourceTemplateResolverPrefix(Object bean) {
        if (bean instanceof SpringResourceTemplateResolver) {
            URL resource = this.getClass().getClassLoader().getResource("templates/");
            String devResource = resource.getFile().toString().replaceAll("target/classes", "src/main/resources");
            ((SpringResourceTemplateResolver) bean).setPrefix("file:"+devResource);
        }
        return bean;
    }
}
