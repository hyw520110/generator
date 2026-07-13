package org.hyw.tools.generator.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.hyw.tools.generator.conf.GlobalConf;
import org.hyw.tools.generator.enums.Component;
import org.hyw.tools.generator.enums.Feature;
import org.hyw.tools.generator.enums.SecurityScheme;
import org.junit.Test;

public class ConfigValidatorSecurityTest {

    @Test
    public void shiroAndSpringSecurityAreMutuallyExclusive() {
        GlobalConf config = baseConfig();
        config.setComponents(new Component[] { Component.MYBATIS, Component.SPRINGBOOT, Component.SHIRO,
                Component.SPRINGSECURITY });

        ConfigValidator.ValidationResult result = ConfigValidator.validateWithResult(config);

        assertTrue(result.getErrorMessage(), result.hasErrors());
        assertTrue(result.getErrorMessage(), result.getErrorMessage().contains("SHIRO 与 SPRINGSECURITY 只能二选一"));
    }

    @Test
    public void oauth2RequiresSpringSecurity() {
        GlobalConf config = baseConfig();
        config.setComponents(new Component[] { Component.MYBATIS, Component.SPRINGBOOT, Component.JWT });
        config.setFeatures(new Feature[] { Feature.OAUTH2 });

        ConfigValidator.ValidationResult result = ConfigValidator.validateWithResult(config);

        assertTrue(result.getErrorMessage(), result.hasErrors());
        assertTrue(result.getErrorMessage(), result.getErrorMessage().contains("启用 OAUTH2 特性必须选择 SPRINGSECURITY"));
    }

    @Test
    public void shiroIsRejectedForDefaultBoot3Target() {
        GlobalConf config = baseConfig();
        config.setJavaVersion("17");
        config.setComponents(new Component[] { Component.MYBATIS, Component.SPRINGBOOT, Component.SHIRO });

        ConfigValidator.ValidationResult result = ConfigValidator.validateWithResult(config);

        assertTrue(result.getErrorMessage(), result.hasErrors());
        assertTrue(result.getErrorMessage(), result.getErrorMessage().contains("SHIRO 仅作为 Boot2 安全方案保留"));
    }

    @Test
    public void shiroIsRejectedForExplicitBoot4Target() {
        GlobalConf config = baseConfig();
        config.setJavaVersion("21");
        config.setPlatformId("java21-boot4");
        config.setComponents(new Component[] { Component.MYBATIS, Component.SPRINGBOOT, Component.SHIRO });

        ConfigValidator.ValidationResult result = ConfigValidator.validateWithResult(config);

        assertTrue(result.getErrorMessage(), result.hasErrors());
        assertTrue(result.getErrorMessage(), result.getErrorMessage().contains("Boot3 及以上"));
    }

    @Test
    public void shiroIsAllowedForExplicitJava17Boot2Target() {
        GlobalConf config = baseConfig();
        config.setJavaVersion("17");
        config.setPlatformId("java17-boot2");
        config.setComponents(new Component[] { Component.MYBATIS, Component.SPRINGBOOT, Component.SHIRO });

        ConfigValidator.ValidationResult result = ConfigValidator.validateWithResult(config);

        assertFalse(result.getErrorMessage(), result.hasErrors());
    }

    @Test
    public void explicitSpringSecurityOauth2NormalizesComponentsAndFeatures() {
        GlobalConf config = baseConfig();
        config.setSecurity(SecurityScheme.SPRING_SECURITY_OAUTH2);
        config.setComponents(new Component[] { Component.MYBATIS, Component.SPRINGBOOT, Component.SHIRO, Component.JWT });

        ConfigValidator.ValidationResult result = ConfigValidator.validateWithResult(config);

        assertFalse(result.getErrorMessage(), result.hasErrors());
        assertEquals(SecurityScheme.SPRING_SECURITY_OAUTH2, config.getSecurity());
        assertTrue(Arrays.asList(config.getComponents()).contains(Component.SPRINGSECURITY));
        assertFalse(Arrays.asList(config.getComponents()).contains(Component.SHIRO));
        assertFalse(Arrays.asList(config.getComponents()).contains(Component.JWT));
        assertTrue(Arrays.asList(config.getFeatures()).contains(Feature.OAUTH2));
    }

    @Test
    public void explicitShiroNormalizesJwtAndRemovesSpringSecurity() {
        GlobalConf config = baseConfig();
        config.setSecurity(SecurityScheme.SHIRO);
        config.setComponents(new Component[] { Component.MYBATIS, Component.SPRINGBOOT, Component.SPRINGSECURITY });
        config.setFeatures(new Feature[] { Feature.OAUTH2 });

        ConfigValidator.ValidationResult result = ConfigValidator.validateWithResult(config);

        assertFalse(result.getErrorMessage(), result.hasErrors());
        assertEquals(SecurityScheme.SHIRO, config.getSecurity());
        assertTrue(Arrays.asList(config.getComponents()).contains(Component.SHIRO));
        assertTrue(Arrays.asList(config.getComponents()).contains(Component.JWT));
        assertFalse(Arrays.asList(config.getComponents()).contains(Component.SPRINGSECURITY));
        assertFalse(Arrays.asList(config.getFeatures()).contains(Feature.OAUTH2));
    }

    @Test
    public void legacySpringSecurityOauth2SelectionIsInferred() {
        GlobalConf config = baseConfig();
        config.setComponents(new Component[] { Component.MYBATIS, Component.SPRINGBOOT, Component.SPRINGSECURITY });
        config.setFeatures(new Feature[] { Feature.OAUTH2 });

        ConfigValidator.ValidationResult result = ConfigValidator.validateWithResult(config);

        assertFalse(result.getErrorMessage(), result.hasErrors());
        assertEquals(SecurityScheme.SPRING_SECURITY_OAUTH2, config.getSecurity());
    }

    private GlobalConf baseConfig() {
        GlobalConf config = new GlobalConf();
        config.setOutputDir("target/generated-test");
        config.setRootPackage("com.example");
        config.setModules(new String[] { "demo" });
        config.setJavaVersion("8");
        config.setVersion("1.0.0");
        config.setEncoding("UTF-8");
        config.setFeatures(new Feature[0]);
        config.setComponents(new Component[] { Component.MYBATIS, Component.SPRINGBOOT });
        return config;
    }
}
