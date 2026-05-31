package org.hyw.tools.generator.web.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.lang.reflect.Field;
import java.nio.file.Files;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class DownloadFileServiceTest {

	private File tempDir;
	private DownloadFileService service;

	@Before
	public void setUp() throws Exception {
		tempDir = Files.createTempDirectory("generator-download-service-test").toFile();
		service = new DownloadFileService();
		setField(service, "downloadDir", tempDir.getAbsolutePath());
	}

	@After
	public void tearDown() throws Exception {
		deleteRecursively(tempDir);
	}

	@Test
	public void createsUserScopedDownloadPath() throws Exception {
		File targetDir = service.createTargetDir("client-a", "127.0.0.1:3306", "demo/db");
		File zip = new File(targetDir, "demo.zip");
		assertTrue(zip.createNewFile());

		String relativePath = service.relativePath("client-a", zip);

		assertEquals("client-a/127.0.0.1_3306/demo_db/demo.zip", relativePath);
		assertEquals(zip.getCanonicalFile(), service.resolveUserFile("client-a", relativePath));
	}

	@Test
	public void rejectsCrossUserAccess() throws Exception {
		File targetDir = service.createTargetDir("client-a", "127.0.0.1:3306", "demo");
		File zip = new File(targetDir, "demo.zip");
		assertTrue(zip.createNewFile());
		String relativePath = service.relativePath("client-a", zip);

		try {
			service.resolveUserFile("client-b", relativePath);
			fail("跨用户文件访问应被拒绝");
		} catch (Exception e) {
			assertTrue(e.getMessage().contains("非法路径"));
		}
	}

	@Test
	public void rejectsTraversalAndUnsupportedFileTypes() throws Exception {
		try {
			service.resolveUserFile("client-a", "../client-b/demo.zip");
			fail("路径穿越应被拒绝");
		} catch (Exception e) {
			assertTrue(e.getMessage().contains("非法路径"));
		}

		try {
			service.resolveUserFile("client-a", "127.0.0.1_3306/demo/readme.txt");
			fail("非下载文件类型应被拒绝");
		} catch (Exception e) {
			assertTrue(e.getMessage().contains("不支持的文件类型"));
		}
	}

	private void setField(Object target, String name, Object value) throws Exception {
		Field field = target.getClass().getDeclaredField(name);
		field.setAccessible(true);
		field.set(target, value);
	}

	private void deleteRecursively(File file) throws Exception {
		if (file == null || !file.exists()) {
			return;
		}
		if (file.isDirectory()) {
			File[] children = file.listFiles();
			if (children != null) {
				for (File child : children) {
					deleteRecursively(child);
				}
			}
		}
		assertTrue(file.delete());
	}
}
