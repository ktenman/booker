package ee.tenman.booker;

import com.codeborne.selenide.Configuration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
@EnableRetry
public class BookerApplication {

	static {
		Configuration.startMaximized = true;
		Configuration.headless = true;
		Configuration.proxyEnabled = false;
		Configuration.screenshots = false;
		Configuration.browser = "firefox";
	}

	public static void main(String[] args) {
		SpringApplication.run(BookerApplication.class, args);
	}


}
