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

//启动完成后打开浏览器.实现ApplicationRunner也可 
@Profile("dev")
@Component
public class StartupRunner implements CommandLineRunner {
    private static final Logger logger = LoggerFactory.getLogger(StartupRunner.class.getName());

	@Override
	public void run(String... arg0) throws Exception {
	    logger.info("服务启动完成!");
		openBrowser();
	}

	@Async
	public void openBrowser() {
		try {
			String url = "http://localhost:${server_port}/";
			if(Desktop.isDesktopSupported()){
				Desktop.getDesktop().browse(new URI(url));
				return ;
			}
			Runtime.getRuntime().exec("rundll32 url.dll,FileProtocolHandler "+url); 
		} catch (IOException | URISyntaxException e) {
			e.printStackTrace();
		}
	}

}
