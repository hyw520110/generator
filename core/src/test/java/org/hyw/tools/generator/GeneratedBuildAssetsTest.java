package org.hyw.tools.generator;

import org.junit.Assume;
import org.junit.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class GeneratedBuildAssetsTest {

    @Test
    public void shouldMakeGeneratedShellScriptsExecutable() throws Exception {
        Assume.assumeFalse(System.getProperty("os.name").toLowerCase().contains("win"));
        Path script = Files.createTempFile("generator-build-", ".sh");
        script.toFile().setExecutable(false, false);

        Generator.applyOutputPermissions(script.toFile());

        assertTrue(Files.isExecutable(script));
    }

    @Test
    public void backendBuildTemplateShouldNotInvokeFrontendBuild() throws Exception {
        Path generatorRoot = generatorRoot();
        Path script = generatorRoot.resolve("core/src/main/resources/templates/assets/package.sh");
        String content = new String(Files.readAllBytes(script), StandardCharsets.UTF_8);

        assertFalse(content.contains("yarn install"));
        assertFalse(content.contains("mv ./dist"));
        assertFalse(Files.exists(generatorRoot.resolve("core/src/main/resources/templates/assets/{1}/package.sh")));
    }

    private Path generatorRoot() {
        Path current = new File(System.getProperty("user.dir")).toPath().toAbsolutePath();
        return current.getFileName().toString().equals("core") ? current.getParent() : current;
    }
}
