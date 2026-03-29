#if($!{SWAGGER2})
package $!{swagger2Package};

import java.util.List;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.github.xiaoymin.swaggerbootstrapui.annotations.EnableSwaggerBootstrapUI;
import com.google.common.collect.Lists;

import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.ApiKey;
import springfox.documentation.service.AuthorizationScope;
import springfox.documentation.service.Contact;
import springfox.documentation.service.SecurityReference;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spi.service.contexts.SecurityContext;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

@Configuration
@EnableSwagger2
@EnableAutoConfiguration
@EnableSwaggerBootstrapUI
public class Swagger2Configuration {
	
	@Bean
    public Docket createRestApi() {
        return new Docket(DocumentationType.SWAGGER_2).enable(true).useDefaultResponseMessages(false)
                .apiInfo(apiInfo())
                .select()
                .apis(RequestHandlerSelectors.basePackage("${rootPackage}.${projectName}.${moduleName}.controller"))
//                .paths(PathSelectors.any())
                .paths(PathSelectors.regex("^(?!auth).*$"))
                .build()
                .securitySchemes(Lists.newArrayList(new ApiKey("Authorization", "Authorization", "header")))
                .securityContexts(Lists.newArrayList(SecurityContext.builder()
                        .securityReferences(defaultAuth())
                        .forPaths(PathSelectors.regex("^(?!auth).*$"))
                        .build()));
    }

    private ApiInfo apiInfo() {
        return new ApiInfoBuilder()
                .title("APIS")
                .description("RESTful APIs")
                .termsOfServiceUrl("http://www.github.com/hyw520110")
                .contact(new Contact("hyw","http://www.github.com/hyw520110","419140278@qq.com"))
                .version("1.0")
                .build();
    }
    
    //点击右上角的Authorize，弹出认证窗口之后输入请求token,之后的每次请求的请求头都会带有token认证信息
    List<SecurityReference> defaultAuth() {
        AuthorizationScope authorizationScope = new AuthorizationScope("global", "accessEverything");
        AuthorizationScope[] authorizationScopes = new AuthorizationScope[1];
        authorizationScopes[0] = authorizationScope;
        return Lists.newArrayList(
                new SecurityReference("Authorization", authorizationScopes));
    }
}
#end