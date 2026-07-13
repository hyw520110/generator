package org.hyw.tools.generator.template;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Test;

public class VueTemplateCompatibilityTest {

	private static final Path PROJECT_ROOT = Paths.get(System.getProperty("user.dir")).getParent();
	private static final Path FREEMARKER_VUE = PROJECT_ROOT
			.resolve("core/src/main/resources/templates/freemarker/modules/#vue#");
	private static final Path VELOCITY_VUE = PROJECT_ROOT
			.resolve("core/src/main/resources/templates/velocity/modules/#vue#");

	@Test
	public void vueTemplatesDoNotUseDeprecatedAliasesOrDeepSelectors() throws Exception {
		for (Path root : new Path[] { FREEMARKER_VUE, VELOCITY_VUE }) {
			for (Path file : textFiles(root)) {
				String content = read(file);
				assertFalse(file + " contains deprecated /deep/", content.contains("/deep/"));
				assertFalse(file + " contains deprecated >>>", content.contains(">>>"));
				assertFalse(file + " contains unsupported ~@ alias", content.contains("~@/"));
				assertFalse(file + " contains unsupported ~/@ alias", content.contains("~/@"));
			}
		}
	}

	@Test
	public void vueTemplatesKeepViteCompatibilityGuards() throws Exception {
		for (Path viteConfig : new Path[] { VELOCITY_VUE.resolve("web/vite.config.js"),
				FREEMARKER_VUE.resolve("web/vite.config.js.ftl") }) {
			String content = read(viteConfig);
			assertTrue(viteConfig + " should exclude duplicate Trend auto-registration",
					content.contains("!src/components/Charts/Trend.vue"));
			assertTrue(viteConfig + " should split vendor chunks", content.contains("manualChunks"));
		}
	}

	private List<Path> textFiles(Path root) throws IOException {
		try (Stream<Path> stream = Files.walk(root)) {
			return stream.filter(Files::isRegularFile).filter(this::isTextTemplate).collect(Collectors.toList());
		}
	}

	private boolean isTextTemplate(Path file) {
		String name = file.getFileName().toString();
		return name.endsWith(".vue") || name.endsWith(".js") || name.endsWith(".jsx") || name.endsWith(".ts")
				|| name.endsWith(".less") || name.endsWith(".ftl") || name.endsWith(".vm");
	}

	private String read(Path file) throws IOException {
		return new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
	}
}
