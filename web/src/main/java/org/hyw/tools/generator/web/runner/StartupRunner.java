package org.hyw.tools.generator.web.runner;

import java.awt.Desktop;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;

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
	public void runNode()  {
		try {
			StringWriter w=new StringWriter();
			IOUtils.copy(Runtime.getRuntime().exec("start-dev.sh").getInputStream(), w);
			System.out.println("run:"+w);
		} catch (Exception e) {
		}
	}

	@Async
	public void openBrowser() {
		String url = "http://localhost:" + port;
		try {
			if (Desktop.isDesktopSupported()) {
				Desktop.getDesktop().browse(new URI(url));
				return;
			}
			String shell = (System.getProperty("os.name").startsWith("Mac") ? "open /Applications/Safari.app "
					: System.getProperty("os.name").startsWith("Win")?"rundll32 url.dll,FileProtocolHandler ":"/usr/share/applications/google-chrome.desktop ") + url;
			logger.info("exec shell:{}", shell);
			Runtime.getRuntime().exec(shell);
		} catch (IOException | URISyntaxException e) {
			logger.warn("open url:{} ,{}", url, e.getLocalizedMessage());
		}
	}
}
