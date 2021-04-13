package ee.tenman.booker;

import com.codeborne.selenide.Configuration;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;
import com.google.common.collect.ImmutableMap;
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

import static com.codeborne.selenide.Condition.exist;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;
import static com.codeborne.selenide.Selenide.closeWebDriver;
import static com.codeborne.selenide.Selenide.open;
import static org.openqa.selenium.By.className;
import static org.openqa.selenium.By.id;
import static org.openqa.selenium.By.linkText;
import static org.openqa.selenium.By.tagName;

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

    @Scheduled(cron = "00 59 17 * * ?")
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
        ElementsCollection elementsCollection = $$(tagName("a")).filter(text("basket"));
        for (SelenideElement e : elementsCollection) {
            String text = e.closest("tr").getText();
            boolean isValidDay = text.contains("London") && text.contains(start.format(DATE_FORMATTER));
            if (isValidDay && times.stream().anyMatch(text::contains)) {
                log.info("Found: {}", text);
                e.click();
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
        LocalDateTime start = LocalDateTime.now().plusDays(7);
        open("https://better.legendonlineservices.co.uk/poplar_baths/BookingsCentre/Timetable?KeepThis=true&#");
        ElementsCollection elementsCollection = $$(tagName("a")).filter(text("basket"));
        for (SelenideElement e : elementsCollection) {
            String text = e.closest("tr").getText();
            boolean isValidDay = text.contains("London") && text.contains(start.format(DATE_FORMATTER));
            if (isValidDay && text.contains("07:50") && text.contains("08:00")) {
                log.info("Found: {}", text);
                e.click();
                return true;
            }
        }
        return false;
    }

    private void tearDown(long start) {
        log.info("Finishing with: {}s", duration(start, System.nanoTime()));
        closeWebDriver();
    }

    private boolean agreeToBookingTerms() {
        try {
            open("https://better.legendonlineservices.co.uk/poplar_baths/Basket/Index");
            $(id("agreeBookingTerms")).waitUntil(exist, 30_000);
            $(id("agreeBookingTerms")).click();
            $(id("btnPayNow")).waitUntil(exist, 30_000);
            $(id("btnPayNow")).click();
            log.info("Registered");
            return true;
        } catch (Exception e) {
            return false;
        }
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
        $(linkText("Hackney")).click();
        ElementsCollection selenideElements = $(className("cscLeftPane")).$$(className("clubResult"));
        selenideElements.find(text("London Fields Lido")).$(tagName("label")).click();
        $(linkText("Tower Hamlets")).click();
        selenideElements.find(text("Poplar Baths LC")).$(tagName("label")).click();
        $(id("behaviours")).waitUntil(exist, 30);
        $(id("behaviours")).$$(className("activityItem")).find(text("Swim")).$(tagName("label")).click();
        $(id("activities")).waitUntil(exist, 30);
        $(id("activities")).$$(className("activityItem")).find(text("Swim for Fitness")).$(tagName("label")).click();
        $(id("bottomsubmit")).click();
        log.info("Swimming activity selected");
    }

    public Map<String, Object> login() {
        long start = System.nanoTime();
        try {
            closeWebDriver();
            open("https://better.legendonlineservices.co.uk/enterprise/account/login");
            $(id("login_Email")).setValue(email);
            $(id("login_Password")).setValue(password);
            $(id("login")).click();
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
