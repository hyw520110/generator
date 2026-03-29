package org.hyw.tools.generator.utils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.lang.ArrayUtils;
import org.hyw.tools.generator.constants.Consts;
import org.hyw.tools.generator.template.TemplateResource;
import org.hyw.tools.generator.template.impl.FileTemplateResource;
import org.hyw.tools.generator.template.impl.JarTemplateResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileUtils extends org.apache.commons.io.FileUtils {
	private static final Logger logger = LoggerFactory.getLogger(FileUtils.class);

	/**
	 * 获取模板资源列表 (流式支持)
	 */
	public static List<TemplateResource> getTemplateResources(URL url, String subDirName, String[] binaryExtensions) {
		List<TemplateResource> resources = new ArrayList<>();
		if (url == null) return resources;

		if ("jar".equals(url.getProtocol())) {
			return getJarTemplateResources(url, subDirName, binaryExtensions);
		}

		File baseDir = new File(url.getPath(), subDirName);
		if (!baseDir.exists()) return resources;

		Collection<File> allFiles = org.apache.commons.io.FileUtils.listFiles(baseDir, null, true);
		for (File file : allFiles) {
			String relativePath = file.getAbsolutePath().replace(baseDir.getAbsolutePath(), "");
			if (relativePath.startsWith(File.separator)) relativePath = relativePath.substring(1);
			
			String normalizedPath = subDirName + Consts.PATH_SEPARATOR + relativePath.replace(Consts.PATH_WINDOWS_SEPARATOR, Consts.PATH_SEPARATOR);
			boolean isBinary = isBinaryPath(normalizedPath, binaryExtensions);
			resources.add(new FileTemplateResource(file, normalizedPath, isBinary));
		}
		return resources;
	}

	private static List<TemplateResource> getJarTemplateResources(URL url, String subDirName, String[] binaryExtensions) {
		List<TemplateResource> resources = new ArrayList<>();
		try {
			JarURLConnection conn = (JarURLConnection) url.openConnection();
			JarFile jarFile = conn.getJarFile();
			
			String basePath = url.getPath();
			if (basePath.contains("!/")) basePath = StringUtils.substringAfter(basePath, "!/");
			if (basePath.endsWith(Consts.PATH_SEPARATOR)) basePath = basePath.substring(0, basePath.length() - 1);
			String entryPrefix = basePath + Consts.PATH_SEPARATOR + subDirName;

			Enumeration<JarEntry> entries = jarFile.entries();
			while (entries.hasMoreElements()) {
				JarEntry entry = entries.nextElement();
				String name = entry.getName();
				if (name.startsWith(entryPrefix) && !entry.isDirectory()) {
					String relativePath = name.substring(entryPrefix.length());
					if (relativePath.startsWith(Consts.PATH_SEPARATOR)) relativePath = relativePath.substring(1);
					
					String virtualPath = subDirName + Consts.PATH_SEPARATOR + relativePath;
					boolean isBinary = isBinaryPath(virtualPath, binaryExtensions);
					resources.add(new JarTemplateResource(jarFile, entry, virtualPath, isBinary));
				}
			}
		} catch (IOException e) {
			logger.error("读取 JAR 资源失败: {}", url, e);
		}
		return resources;
	}

	private static boolean isBinaryPath(String path, String[] binaryExtensions) {
		if (binaryExtensions == null) return false;
		String ext = StringUtils.substringAfterLast(path.toLowerCase(), ".");
		return ArrayUtils.contains(binaryExtensions, ext);
	}

	public static Map<String, String> getJarEntries(URL url, String entryName, String excludeEntry,
			String[] byteFileExt, String... subEntryNames) {
		if (null == url || !"jar".equals(url.getProtocol())) {
			logger.debug("URL is null or not JAR protocol: {}", url);
			return null;
		}
		Map<String, String> map = new LinkedHashMap<String, String>();
		JarFile jarFile = null;
		try {
			jarFile = ((JarURLConnection) url.openConnection()).getJarFile();
			List<String> list = getEntryNames(entryName, subEntryNames);
			logger.debug("getJarEntries - entryName: {}, subEntryNames: {}, list: {}", entryName,
					Arrays.toString(subEntryNames), list);
			Enumeration<JarEntry> entries = jarFile.entries();
			int matchCount = 0;
			while (entries.hasMoreElements()) {
				JarEntry entry = entries.nextElement();
				String entryPath = entry.getName();
				if (!entryPath.startsWith(entryName)
						|| (StringUtils.isNotBlank(excludeEntry) && entryPath.startsWith(excludeEntry))
						|| entry.isDirectory()) {
					continue;
				}
				if (startWith(list, entryPath)) {
					matchCount++;
					if (null != byteFileExt
							&& ArrayUtils.contains(byteFileExt, StringUtils.substringAfterLast(entry.getName(), "."))) {
						ByteArrayOutputStream baos = new ByteArrayOutputStream();
						IOUtils.copy(jarFile.getInputStream(entry), baos);
						map.put(entry.getName(), java.util.Base64.getEncoder().encodeToString(baos.toByteArray()));
						continue;
					}
					StringWriter sWriter = new StringWriter();
					IOUtils.copy(jarFile.getInputStream(entry), sWriter);
					map.put(entry.getName(), sWriter.toString());
				}
			}
			logger.debug("getJarEntries - matched {} entries for entryName: {}", matchCount, entryName);
		} catch (Exception e) {
			logger.error("getJarEntries:jar:file:{}!{} ,exception:", jarFile, entryName, e);
		}
		return map;
	}

	private static boolean startWith(List<String> list, String entryName) {
		if (null == list || entryName.endsWith("/")) {
			return true;
		}
		for (String s : list) {
			if (entryName.startsWith(s)) {
				return true;
			}
		}
		return false;
	}

	private static List<String> getEntryNames(String entryName, String... subEntryNames) {
		if (isEmpty(subEntryNames)) {
			return null;
		}
		List<String> entryNames = new ArrayList<>();
		for (String sub : subEntryNames) {
			entryNames.add(entryName + "/" + sub);
		}
		return entryNames;
	}

	private static boolean isEmpty(String... name) {
		return null == name || name.length == 0;
	}

	public static File[] listFiles(File dir, String[] names) {
		File[] files = listFiles(dir, new DirectoryFileFilter() {
			private static final long serialVersionUID = 1L;

			@Override
			public boolean accept(File file) {
				String dirName = file.getPath().replace(dir.getAbsolutePath(), "").substring(1);
				int index = StringUtils.indexOf(dirName, File.separator);
				if (index != -1) {
					dirName = StringUtils.substring(dirName, 0, index);
				}
				return ArrayUtils.contains(names, dirName);
			}
		}, FileFilterUtils.trueFileFilter()).toArray(new File[] {});
		return sort(files);
	}

	public static File[] sort(File[] files) {
		Arrays.sort(files, new Comparator<File>() {
			@Override
			public int compare(File o1, File o2) {
				if (o1.isDirectory() && o2.isFile())
					return 1;
				if (o1.isFile() && o2.isDirectory())
					return -1;
				int value = o1.getParent().compareToIgnoreCase(o2.getParent());
				return value == 0 ? o1.getName().compareToIgnoreCase(o2.getName()) : value;
			}
		});
		return files;
	}

	/**
	 * 将文件夹打包为ZIP写入输出流
	 * 
	 * @param folder 要打包的文件夹
	 * @param zos    Zip输出流
	 * @throws IOException IO异常
	 */
	public static void zipFolder(File folder, ZipOutputStream zos) throws IOException {
		zipFolder(folder, folder.getName(), zos);
	}

	/**
	 * 递归打包文件夹
	 * 
	 * @param folder     要打包的文件夹
	 * @param parentPath 父路径（用于ZIP条目）
	 * @param zos        Zip输出流
	 * @throws IOException IO异常
	 */
	public static void zipFolder(File folder, String parentPath, ZipOutputStream zos) throws IOException {
		zipFolder(folder, parentPath, zos, new int[] { 0 });
	}

	/**
	 * 递归打包文件夹（带计数器）
	 */
	private static void zipFolder(File folder, String parentPath, ZipOutputStream zos, int[] count) throws IOException {
		File[] files = folder.listFiles();
		if (files == null) {
			return;
		}

		byte[] buffer = new byte[8192];

		for (File file : files) {
			String entryPath = parentPath + "/" + file.getName();

			if (file.isDirectory()) {
				// 递归处理子目录
				zipFolder(file, entryPath, zos, count);
			} else {
				// 添加文件到zip
				ZipEntry entry = new ZipEntry(entryPath);
				zos.putNextEntry(entry);

				try (FileInputStream fis = new FileInputStream(file)) {
					int len;
					while ((len = fis.read(buffer)) > 0) {
						zos.write(buffer, 0, len);
					}
				}

				zos.closeEntry();
				count[0]++;
			}
		}
	}

	/**
	 * 统计文件夹中的文件数量
	 */
	public static int countFiles(File folder) {
		File[] files = folder.listFiles();
		if (files == null) {
			return 0;
		}
		int count = 0;
		for (File file : files) {
			if (file.isDirectory()) {
				count += countFiles(file);
			} else {
				count++;
			}
		}
		return count;
	}

	/**
	 * 判定是否为二进制文件 通过检查文件前 1024 字节是否存在控制字符（非空白）来判定
	 */
	public static boolean isBinary(File file) {
		if (file == null || !file.exists() || file.isDirectory()) {
			return false;
		}
		try (FileInputStream fis = new FileInputStream(file)) {
			byte[] buffer = new byte[1024];
			int len = fis.read(buffer);
			if (len == -1)
				return false;
			for (int i = 0; i < len; i++) {
				byte b = buffer[i];
				if (b < 0x09)
					return true; // 存在非法控制字符
			}
		} catch (IOException e) {
			logger.warn("检查二进制文件失败: {}", file.getPath());
		}
		return false;
	}

	/**
	 * 判定字节数组是否为二进制内容
	 */
	public static boolean isBinary(byte[] bytes) {
		if (bytes == null || bytes.length == 0)
			return false;
		int len = Math.min(bytes.length, 1024);
		for (int i = 0; i < len; i++) {
			byte b = bytes[i];
			if (b < 0x09)
				return true;
		}
		return false;
	}

	/**
	 * 统一路径规约： 1. 统一使用正斜杠 / 2. 消除重复的斜杠 3. 去除首尾多余斜杠（保留相对路径语义）
	 */
	public static String normalizePath(String path) {
		if (path == null)
			return null;
		// 统一分隔符并处理重复斜杠
		String result = path.replace("\\", "/").replaceAll("/{2,}", "/");
		if (result.startsWith("/")) {
			result = result.substring(1);
		}
		if (result.endsWith("/")) {
			result = result.substring(0, result.length() - 1);
		}
		return result;
	}

	/**
	 * 获取指定目录中指定后缀的最新文件
	 * 
	 * @param dir        必须存在的目录
	 * @param extensions 文件后缀列表（可变参数，可为 null 或空表示接受所有文件）
	 * @return 最新修改的文件，如果没有匹配的文件则返回 null
	 */
	public static File getLatestFile(File dir, String... extensions) {
		if (dir == null || !dir.exists() || !dir.isDirectory()) {
			logger.debug("目录不存在或不是目录: {}", dir);
			return null;
		}

		File[] files = dir.listFiles();
		if (files == null || files.length == 0) {
			logger.debug("目录为空: {}", dir);
			return null;
		}
		boolean flag = (extensions == null || extensions.length == 0);
		// 筛选匹配后缀的文件
		List<File> matchedFiles = new ArrayList<>();
		for (File file : files) {
			if (file.isDirectory()) {
				continue;
			}
			String fileName = file.getName().toLowerCase();
			if (flag || (!flag && ArrayUtils.contains(extensions, StringUtils.substringAfterLast(fileName, ".")))) {
				matchedFiles.add(file);
			}
		}

		if (matchedFiles.isEmpty()) {
			logger.debug("目录中没有匹配后缀的文件: {}, 后缀: {}", dir, Arrays.toString(extensions));
			return null;
		}

		return getLatestFileByModification(matchedFiles.toArray(new File[0]));
	}

	/**
	 * 从文件数组中获取最新修改的文件
	 */
	private static File getLatestFileByModification(File[] files) {
		if (files == null || files.length == 0) {
			return null;
		}

		File latestFile = files[0];
		for (File file : files) {
			if (file.lastModified() > latestFile.lastModified()) {
				latestFile = file;
			}
		}
		return latestFile;
	}
}
