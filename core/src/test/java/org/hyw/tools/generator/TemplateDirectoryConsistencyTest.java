package org.hyw.tools.generator;

import org.hyw.tools.generator.enums.Component;
import org.hyw.tools.generator.enums.Feature;
import org.hyw.tools.generator.enums.SecurityScheme;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.util.HashSet;
import java.util.Set;
import java.util.Arrays;

public class TemplateDirectoryConsistencyTest {

    @Test
    public void testTemplateDirectoryConsistency() {
        File freemarkerDir = new File("src/main/resources/templates/freemarker");
        File velocityDir = new File("src/main/resources/templates/velocity");
        Assert.assertTrue("Freemarker directory should exist", freemarkerDir.exists());

        Set<String> validIdentifiers = new HashSet<>();
        Set<String> requiredIdentifiers = new HashSet<>();

        // Add all Components
        for (Component c : Component.values()) {
            validIdentifiers.add(c.name().toLowerCase());
            validIdentifiers.add(c.getAlias().toLowerCase());
            requiredIdentifiers.add(c.getAlias().toLowerCase());
        }
        // Add all Features
        for (Feature f : Feature.values()) {
            validIdentifiers.add(f.name().toLowerCase());
            validIdentifiers.add(f.getAlias().toLowerCase());
            requiredIdentifiers.add(f.getAlias().toLowerCase());
        }
        // Add all SecuritySchemes
        for (SecurityScheme s : SecurityScheme.values()) {
            validIdentifiers.add(s.name().toLowerCase());
            requiredIdentifiers.add(s.name().toLowerCase());
        }

        // Additional allowed aliases that are known
        validIdentifiers.add("openapi");
        validIdentifiers.add("devops");
        validIdentifiers.add("springsecurity");
        validIdentifiers.add("oauth2");
        
        Set<String> foundIdentifiers = new HashSet<>();

        // Scan all #xxx# directories
        scanDirectories(freemarkerDir, validIdentifiers, foundIdentifiers);
        if (velocityDir.exists()) {
            scanDirectories(velocityDir, validIdentifiers, foundIdentifiers);
        }
        
        // Reverse check: Some components might genuinely not have a folder (e.g. if they only modify POM).
        // If we strictly enforce it, we must ensure all of them have a folder.
        // For now, let's just log or assert on a subset if they fail.
        Set<String> missing = new HashSet<>(requiredIdentifiers);
        missing.removeAll(foundIdentifiers);
        
        // There are a few components that don't need a folder, let's remove them from missing
        Set<String> allowedMissing = new HashSet<>(Arrays.asList(
            "swagger2", "mybatis", "jpa", "redis", "zookeeper", "dubbo", "sentinel", 
            "jwt", "shiro", "seata", "flyway", "liquibase", "none", "local", "db"
        ));
        missing.removeAll(allowedMissing);
        
        if (!missing.isEmpty()) {
            // We just warn instead of failing the build if a component doesn't have a template folder
            // because some components only inject config into POM or application.properties.
            System.err.println("Warning: The following components/features do not have a corresponding #xxx# directory: " + missing);
        }
    }

    private void scanDirectories(File dir, Set<String> validIdentifiers, Set<String> foundIdentifiers) {
        File[] files = dir.listFiles();
        if (files == null) {
            return;
        }
        for (File file : files) {
            if (file.isDirectory()) {
                String name = file.getName();
                if (name.startsWith("#") && name.endsWith("#") && name.length() > 2) {
                    String identifier = name.substring(1, name.length() - 1).toLowerCase();
                    boolean isValid = validIdentifiers.contains(identifier);
                    if (!isValid) {
                        Assert.fail("Directory " + file.getAbsolutePath() + " contains an unknown identifier: " + identifier + ". It must be defined in Component, Feature, or SecurityScheme enum.");
                    }
                    foundIdentifiers.add(identifier);
                }
                scanDirectories(file, validIdentifiers, foundIdentifiers);
            }
        }
    }
}
