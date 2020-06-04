package org.hyw.tools.generator.utils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.StringWriter;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.lang.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileUtils extends org.apache.commons.io.FileUtils {
	private static final Logger logger = LoggerFactory.getLogger(FileUtils.class);

	public static Map<String, String> getJarEntries(URL url, String entryName, String excludeEntry,
			String[] byteFileExt, String... subEntryNames) {
		if (null == url || !"jar".equals(url.getProtocol())) {
			return null;
		}
		Map<String, String> map = new LinkedHashMap<String, String>();
		JarFile jarFile = null;
		try {
			jarFile = ((JarURLConnection) url.openConnection()).getJarFile();
			List<String> list = getEntryNames(entryName, subEntryNames);
			Enumeration<JarEntry> entries = jarFile.entries();
			while (entries.hasMoreElements()) {
				JarEntry entry = entries.nextElement();
				if (!entry.getName().startsWith(entryName)
						|| (StringUtils.isNotBlank(excludeEntry) && entry.getName().startsWith(excludeEntry))
						|| entry.isDirectory()) {
					continue;
				}
				if (startWith(list, entry.getName())) {
					if (ArrayUtils.contains(byteFileExt, StringUtils.substringAfterLast(entry.getName(), "."))) {
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
		} catch (Exception e) {
			logger.error("getJarEntries:jar:file:{}!{}/{} ,exception:{}", jarFile, entryName, e);
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
}
