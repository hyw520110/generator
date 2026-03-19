package org.hyw.tools.generator.web.runner;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 启动完成后打开浏览器.实现ApplicationRunner也可实现服务启动完成后执行指定操作
 */
@Component
@Profile("dev")
public class StartupRunner implements CommandLineRunner {
	private static final Logger logger = LoggerFactory.getLogger(StartupRunner.class.getName());
	@Value("${server.port}")
	private int port;

	@Override
	public void run(String... arg0) throws Exception {
		logger.info("服务启动完成! 服务端口:{}", port);
		runNode();
		openBrowser();
	}

	@Async
	public void runNode() {
		execScriptFile("start-dev.sh", true);
	}

	@Async
	public void openBrowser() {
		// 启动前端开发服务
		execScriptFile("open-brower.sh", false);
	}

	private void execScriptFile(String shell, boolean async) {
		File sh = new File(shell).getAbsoluteFile();
		if (!sh.exists()) {
			return;
		}
		try {
			ProcessBuilder pb = new ProcessBuilder(sh.getAbsolutePath());
			Process process = pb.start();
			if (!async) {
				return;
			}
			// 异步处理输出流
			Thread outputThread = new Thread(() -> {
				try {
					IOUtils.copy(process.getInputStream(), System.out);
				} catch (IOException e) {
					logger.error("Error reading output stream from script: {}", shell, e);
				}
			});
			outputThread.start();

			// 异步处理错误流
			Thread errorThread = new Thread(() -> {
				try {
					IOUtils.copy(process.getErrorStream(), System.err);
				} catch (IOException e) {
					logger.error("Error reading error stream from script: {}", shell, e);
				}
			});
			errorThread.start();

			// 等待脚本执行完成
			int exitCode = process.waitFor();
			logger.info("{} exited with code: {}", shell, exitCode);
		} catch (IOException | InterruptedException e) {
			logger.warn("exec {} Failed: {}", shell, e.getLocalizedMessage());
		}
	}
}