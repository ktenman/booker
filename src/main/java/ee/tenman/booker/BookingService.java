package ee.tenman.booker;

import com.codeborne.selenide.Configuration;
import com.codeborne.selenide.SelenideElement;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.By;
import org.openqa.selenium.chrome.ChromeOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.closeWebDriver;
import static com.codeborne.selenide.Selenide.closeWindow;
import static com.codeborne.selenide.Selenide.open;

@Service
public class BookingService {

    private final Logger log = LoggerFactory.getLogger(this.getClass());
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM");

    @Value("${email}")
    private String email;

    @Value("${password}")
    private String password;

    @Scheduled(cron = "00 59 17 * * ?")
//    @PostConstruct
    public void register() throws InterruptedException {

        ChromeOptions options = new ChromeOptions();
        options.addArguments("start-maximized"); // open Browser in maximized mode
        options.addArguments("disable-infobars"); // disabling infobars
        options.addArguments("--disable-extensions"); // disabling extensions
        options.addArguments("--disable-gpu"); // applicable to windows os only
        options.addArguments("--disable-dev-shm-usage"); // overcome limited resource problems
        options.addArguments("--no-sandbox"); // Bypass OS security model

        WebDriverManager.chromedriver().setup();
        long start = System.nanoTime();
        Configuration.startMaximized = true;
        Configuration.headless = true;
        Configuration.proxyEnabled = false;
        Configuration.browser = "firefox";

        login();
        selectSwimmingActivity();
        waitUntil6Pm();

        if (!hasRegistered()) {
            log.info("Not registered.");
            tearDown(start);
            return;
        }
        agreeToBookingTerms();
        tearDown(start);
    }

    private boolean hasRegistered() {
        open("https://better.legendonlineservices.co.uk/poplar_baths/BookingsCentre/Timetable?KeepThis=true&#");
        LocalDateTime localDateTime = LocalDateTime.now().plusDays(7);
        for (SelenideElement e : $(By.tagName("table")).$$("tr")) {
            String text = e.getText();
            if (text.contains("London") && text.contains(localDateTime.format(DATE_TIME_FORMATTER)) && text.contains("06:30") && text.contains("06:40")) {
                e.$$(By.tagName("a")).find(text("Add to Basket")).click();
                return true;
            }
        }
        return false;
    }

    private void tearDown(long start) {
        log.info("Finishing with: {}s", duration(start, System.nanoTime()));
        closeWindow();
        closeWebDriver();
    }

    private void agreeToBookingTerms() {
        open("https://better.legendonlineservices.co.uk/poplar_baths/Basket/Index");
        $(By.id("agreeBookingTerms")).click();
        $(By.id("btnPayNow")).click();
    }

    private void waitUntil6Pm() {
        boolean isWait = true;
        while (isWait) {
            boolean isInPast = LocalDateTime.now()
                    .isAfter(LocalDateTime.of(LocalDate.now(), LocalDateTime.parse("2015-08-04T18:00:00").toLocalTime()));
            if (isInPast) {
                log.info("Continue working!");
                isWait = false;
            }
            try {
                TimeUnit.MILLISECONDS.sleep(100);
            } catch (Exception ignored) {
            }
        }
    }

    private void selectSwimmingActivity() throws InterruptedException {
        open("https://better.legendonlineservices.co.uk/poplar_baths/BookingsCentre/Index");
        $(By.linkText("Hackney")).click();
        $(By.className("cscLeftPane")).$$(By.className("clubResult")).find(text("London Fields Lido")).$(By.tagName("label")).click();
        $(By.linkText("Tower Hamlets")).click();
        $(By.className("cscLeftPane")).$$(By.className("clubResult")).find(text("Poplar Baths LC")).$(By.tagName("label")).click();
        TimeUnit.MILLISECONDS.sleep(100);
        $(By.id("behaviours")).$$(By.className("activityItem")).find(text("Swim")).$(By.tagName("label")).click();
        TimeUnit.MILLISECONDS.sleep(100);
        $(By.id("activities")).$$(By.className("activityItem")).find(text("Swim for Fitness")).$(By.tagName("label")).click();
        $(By.id("bottomsubmit")).click();
    }

    private void login() {
        open("https://better.legendonlineservices.co.uk/enterprise/account/login");
        $(By.id("login_Email")).setValue(email);
        $(By.id("login_Password")).setValue(password);
        $(By.id("login")).click();
        log.info("Login succeeded");
    }

    public double duration(long start, long finish) {
        return (finish - start) / 1_000_000_000.0;
    }

}
