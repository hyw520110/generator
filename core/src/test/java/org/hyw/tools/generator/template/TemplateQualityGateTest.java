package org.hyw.tools.generator.template;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Test;

public class TemplateQualityGateTest {

	private static final Path PROJECT_ROOT = Paths.get(System.getProperty("user.dir")).getParent();
	private static final Path TEMPLATES = PROJECT_ROOT.resolve("core/src/main/resources/templates");

	private static final List<Rule> RULES = Arrays.asList(
			new Rule("misspelled module path", "moudulePath"),
			new Rule("hard-coded row id in generated CRUD", "record.id",
					path -> path.toString().contains("components/#vue#/web/src/views")),
			new Rule("JUnit4 Test import in generated Java templates", "import org.junit.Test;",
					path -> isGeneratedJavaTemplate(path)),
			new Rule("JUnit4 Before import in generated Java templates", "import org.junit.Before;",
					path -> isGeneratedJavaTemplate(path)),
			new Rule("JUnit4 Assert import in generated Java templates", "import org.junit.Assert;",
					path -> isGeneratedJavaTemplate(path)),
			new Rule("JUnit4 runner import in generated Java templates", "org.junit.runner.RunWith",
					path -> isGeneratedJavaTemplate(path)),
			new Rule("Spring JUnit4 runner in generated Java templates", "@RunWith",
					path -> isGeneratedJavaTemplate(path)),
			new Rule("legacy Gradle maven plugin", "apply plugin: 'maven'"),
			new Rule("legacy Gradle compile dependency", "compile group:"),
			new Rule("legacy Gradle testCompile dependency", "testCompile"),
			new Rule("private Nexus repository leak", "dev.maven.com"),
			new Rule("private distribution management leak", "<distributionManagement>"),
			new Rule("hard-coded fastjson default", "preferred-json-mapper=fastjson"),
			new Rule("hard-coded fastjson YAML default", "preferred-json-mapper: fastjson"),
			new Rule("hard-coded fastjson config default", "preferred-json-mapper fastjson"),
			new Rule("broad JWT user whitelist", "/user/**"),
			new Rule("broad JWT system whitelist", "/sys/**"),
			new Rule("token value logging", "token:{}"),
			new Rule("hard-coded validation namespace", "javax.validation"),
			new Rule("hard-coded servlet namespace", "javax.servlet"),
			new Rule("hard-coded persistence namespace", "javax.persistence"),
			new Rule("hard-coded jakarta validation namespace", "jakarta.validation"),
			new Rule("hard-coded jakarta servlet namespace", "jakarta.servlet"),
			new Rule("hard-coded jakarta persistence namespace", "jakarta.persistence"),
			new Rule("FreeMarker default syntax in Velocity templates", "?default",
					path -> path.toString().contains("/templates/velocity/")),
			new Rule("FreeMarker presence syntax in Velocity templates", "?has_content",
					path -> path.toString().contains("/templates/velocity/")),
			new Rule("FreeMarker directive syntax in Velocity templates", "<#",
					path -> path.toString().contains("/templates/velocity/")));

	@Test
	public void templatesDoNotContainHighRiskLegacyPatterns() throws Exception {
		List<String> violations = textFiles(TEMPLATES).stream()
				.flatMap(file -> violations(file).stream())
				.collect(Collectors.toList());
		assertTrue("Template quality gate violations:\n" + String.join("\n", violations), violations.isEmpty());
	}

	private List<String> violations(Path file) {
		try {
			String content = read(file);
			return RULES.stream()
					.filter(rule -> rule.appliesTo(file))
					.filter(rule -> content.contains(rule.pattern))
					.map(rule -> file + " contains " + rule.description + " [" + rule.pattern + "]")
					.collect(Collectors.toList());
		} catch (IOException e) {
			throw new IllegalStateException("Failed to read " + file, e);
		}
	}

	private List<Path> textFiles(Path root) throws IOException {
		try (Stream<Path> stream = Files.walk(root)) {
			return stream.filter(Files::isRegularFile).filter(this::isTextTemplate).collect(Collectors.toList());
		}
	}

	private boolean isTextTemplate(Path file) {
		String name = file.getFileName().toString();
		return name.endsWith(".java") || name.endsWith(".js") || name.endsWith(".jsx") || name.endsWith(".vue")
				|| name.endsWith(".xml") || name.endsWith(".properties") || name.endsWith(".yml")
				|| name.endsWith(".yaml") || name.endsWith(".vm") || name.endsWith(".ftl")
				|| name.endsWith(".gradle");
	}

	private String read(Path file) throws IOException {
		return new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
	}

	private static boolean isGeneratedJavaTemplate(Path path) {
		String text = path.toString();
		return text.contains("/templates/freemarker/") || text.contains("/templates/velocity/");
	}

	private static class Rule {
		private final String description;
		private final String pattern;
		private final PathPredicate predicate;

		private Rule(String description, String pattern) {
			this(description, pattern, path -> true);
		}

		private Rule(String description, String pattern, PathPredicate predicate) {
			this.description = description;
			this.pattern = pattern;
			this.predicate = predicate;
		}

		private boolean appliesTo(Path path) {
			return predicate.test(path);
		}
	}

	private interface PathPredicate {
		boolean test(Path path);
	}
}
