package org.hyw.tools.generator.web.service;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class DownloadFileService {

	@Value("${app.download-dir:${user.home}/Downloads/generator}")
	private String downloadDir;

	public File createTargetDir(String clientKey, String ipAndPort, String dbName) throws IOException {
		File targetDir = new File(userRoot(clientKey), buildDataSourceSubDir(ipAndPort, dbName)).getCanonicalFile();
		if (!targetDir.toPath().startsWith(userRoot(clientKey).toPath())) {
			throw new IOException("非法路径");
		}
		if (!targetDir.exists() && !targetDir.mkdirs()) {
			throw new IOException("创建下载目录失败");
		}
		return targetDir;
	}

	public File listTargetDir(String clientKey, String ipAndPort, String dbName) throws IOException {
		return new File(userRoot(clientKey), buildDataSourceSubDir(ipAndPort, dbName)).getCanonicalFile();
	}

	public String relativePath(String clientKey, String ipAndPort, String dbName, String fileName) {
		return clientKey + "/" + buildDataSourceSubDir(ipAndPort, dbName) + "/" + fileName;
	}

	public String relativePath(String clientKey, File file) throws IOException {
		File root = root().getCanonicalFile();
		File canonicalFile = file.getCanonicalFile();
		if (!canonicalFile.toPath().startsWith(userRoot(clientKey).toPath())) {
			throw new IOException("非法路径");
		}
		return root.toPath().relativize(canonicalFile.toPath()).toString().replace(File.separatorChar, '/');
	}

	public File resolveUserFile(String clientKey, String path) throws IOException {
		if (StringUtils.isBlank(path)) {
			throw new IOException("请指定文件路径");
		}
		File userRoot = userRoot(clientKey);
		String normalizedPath = path.replace('\\', '/');
		int slashIndex = normalizedPath.indexOf('/');
		if (slashIndex > 0) {
			String firstSegment = normalizedPath.substring(0, slashIndex);
			if (isClientKeySegment(firstSegment) && !firstSegment.equals(clientKey)) {
				throw new IOException("非法路径");
			}
		}
		File file = normalizedPath.startsWith(clientKey + "/") ? new File(root(), normalizedPath)
				: new File(userRoot, normalizedPath);
		file = file.getCanonicalFile();
		if (!file.toPath().startsWith(userRoot.toPath())) {
			throw new IOException("非法路径");
		}
		if (!isSupportedDownloadFile(file.getName())) {
			throw new IOException("不支持的文件类型");
		}
		return file;
	}

	private File root() throws IOException {
		return new File(downloadDir).getCanonicalFile();
	}

	private File userRoot(String clientKey) throws IOException {
		return new File(root(), clientKey).getCanonicalFile();
	}

	private String buildDataSourceSubDir(String ipAndPort, String dbName) {
		return sanitizeSegment(StringUtils.defaultString(ipAndPort, "unknown").replace(":", "_")) + "/"
				+ sanitizeSegment(StringUtils.defaultString(dbName, "unknown"));
	}

	private String sanitizeSegment(String value) {
		String sanitized = value.replaceAll("[^a-zA-Z0-9._-]", "_");
		return StringUtils.isBlank(sanitized) ? "unknown" : sanitized;
	}

	private boolean isSupportedDownloadFile(String fileName) {
		String lower = fileName.toLowerCase(Locale.ROOT);
		return lower.endsWith(".zip") || lower.endsWith(".docx") || lower.endsWith(".pdf");
	}

	private boolean isClientKeySegment(String value) {
		return value.startsWith("ip-") || value.startsWith("client-") || value.startsWith("session-");
	}
}
