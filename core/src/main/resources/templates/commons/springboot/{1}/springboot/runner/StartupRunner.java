package ${runnerPackage};

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;
/**
 * 启动完成后打开浏览器.实现ApplicationRunner也可实现服务启动完成后执行指定操作
 */
@Profile("dev")
@Component
public class StartupRunner implements CommandLineRunner {
    private static final Logger logger = LoggerFactory.getLogger(StartupRunner.class.getName());
    @Value("${server.port}")
    private int port;
	@Override
	public void run(String... arg0) throws Exception {
	    logger.info("服务启动完成! 服务端口:{}",port);
#if($!{THYMELEAF})
	    openBrowser();
#end
	}
 
	@Async
	public void openBrowser() {
	    String url = "http://localhost:"+port;
		try {
			if(Desktop.isDesktopSupported()){
				Desktop.getDesktop().browse(new URI(url));
				return ;
			}
			String shell = (System.getProperty("os.name").startsWith("Mac") ? "open /Applications/Safari.app "
					: "rundll32 url.dll,FileProtocolHandler ") + url;
			logger.info("exec shell:{}", shell);
			Runtime.getRuntime().exec(shell);
		} catch (IOException | URISyntaxException e) {
			logger.warn("open url:{} ,{}",url,e.getLocalizedMessage());
		}
	}
}
