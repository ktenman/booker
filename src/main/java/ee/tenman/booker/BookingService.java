package ee.tenman.booker;

import com.codeborne.selenide.Configuration;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;
import com.google.common.collect.ImmutableMap;
import org.openqa.selenium.By;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.closeWebDriver;
import static com.codeborne.selenide.Selenide.open;

@Service
public class BookingService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private final Logger log = LoggerFactory.getLogger(this.getClass());
    private final String email;
    private final String password;
    private final String startTime;
    private final String endTime;
    private boolean registered = false;

    public BookingService(
            @Value("${password}") String password,
            @Value("${email}") String email,
            @Value("${startTime}") String startTime,
            @Value("${endTime}") String endTime
    ) {
        this.password = password;
        this.email = email;
        this.startTime = startTime;
        this.endTime = endTime;
        Configuration.startMaximized = true;
        Configuration.headless = true;
        Configuration.proxyEnabled = false;
        Configuration.screenshots = false;
        Configuration.browser = "firefox";
    }

    @Scheduled(cron = "00 59 17 * * ?", zone = "UTC")
    public void register() throws InterruptedException {
        long start = System.nanoTime();
        login();
        selectSwimmingActivity();
        if (!hasRegistered()) {
            log.info("Not registered.");
            tearDown(start);
            return;
        }
        agreeToBookingTerms();
        tearDown(start);
    }

//    @Scheduled(cron = "0 * * * * ?")
    public void registerToCustomTime() throws InterruptedException {
        if (registered) {
            return;
        }
        long start = System.nanoTime();
        login();
        selectSwimmingActivity();
        LocalDateTime now = LocalDateTime.now();
        if (!hasRegisteredToCustomDay(now, getStartingTimes(now))) {
            log.info("Not registered");
            tearDown(start);
            return;
        }
        agreeToBookingTerms();
        tearDown(start);
    }

    boolean hasRegisteredToCustomDay(LocalDateTime start, List<String> times) {
        open("https://better.legendonlineservices.co.uk/poplar_baths/BookingsCentre/Timetable?KeepThis=true&#");
        ElementsCollection selenideElements = $(By.tagName("table")).$$("tr");
        for (SelenideElement e : selenideElements) {
            String text = e.getText();
            boolean isValidDay = text.contains("London") && text.contains(start.format(DATE_FORMATTER));
            SelenideElement selenideElement = e.$$(By.tagName("a")).find(text("Add to Basket"));
            if (isValidDay && times.stream().anyMatch(text::contains) && selenideElement.exists()) {
                log.info("Found: {}", text);
                selenideElement.click();
                return true;
            }
        }
        return false;
    }

    List<String> getStartingTimes(LocalDateTime now) {
        LocalDateTime start = LocalDateTime.of(now.getYear(), now.getMonth(), now.getDayOfMonth(),
                Integer.parseInt(startTime.split(":")[0]),
                Integer.parseInt(startTime.split(":")[1]),
                0);
        LocalDateTime end = LocalDateTime.of(now.getYear(), now.getMonth(), now.getDayOfMonth(),
                Integer.parseInt(endTime.split(":")[0]),
                Integer.parseInt(endTime.split(":")[1]),
                0);
        List<String> result = new ArrayList<>();
        while (start.isBefore(end) || start.equals(end)){
            result.add(start.format(TIME_FORMATTER));
            start = start.plusMinutes(10);
        }
        return result;
    }

    private boolean hasRegistered() {
        open("https://better.legendonlineservices.co.uk/poplar_baths/BookingsCentre/Timetable?KeepThis=true&#");
        LocalDateTime localDateTime = LocalDateTime.now().plusDays(7);
        ElementsCollection selenideElements = $(By.tagName("table")).$$("tr");
        for (SelenideElement e : selenideElements) {
            String text = e.getText();
            SelenideElement selenideElement = e.$$(By.tagName("a")).find(text("Add to Basket"));
            if (text.contains("London") && text.contains(localDateTime.format(DATE_FORMATTER))
                    && text.contains("06:30") && text.contains("06:40") && selenideElement.exists()) {
                waitUntil6PmUtc();
                selenideElement.click();
                return true;
            }
        }
        return false;
    }

    private void tearDown(long start) {
        log.info("Finishing with: {}s", duration(start, System.nanoTime()));
        closeWebDriver();
    }

    private void agreeToBookingTerms() {
        open("https://better.legendonlineservices.co.uk/poplar_baths/Basket/Index");
        $(By.id("agreeBookingTerms")).click();
        $(By.id("btnPayNow")).click();
        registered = true;
        log.info("Registered");
    }

    private void waitUntil6PmUtc() {
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
        log.info("Swimming activity selected");
    }

    public Map<String, Object> login() {
        long start = System.nanoTime();
        try {
            closeWebDriver();
            open("https://better.legendonlineservices.co.uk/enterprise/account/login");
            $(By.id("login_Email")).setValue(email);
            $(By.id("login_Password")).setValue(password);
            $(By.id("login")).click();
            log.info("Login succeeded");
            return ImmutableMap.of("loginSucceed", true, "duration in seconds", duration(start, System.nanoTime()));
        } catch (Exception e) {
            log.error("Failed to login ", e);
            return ImmutableMap.of("loginSucceed", false);
        }
    }

    public double duration(long start, long finish) {
        return (finish - start) / 1_000_000_000.0;
    }

}
