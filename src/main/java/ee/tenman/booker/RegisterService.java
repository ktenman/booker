package ee.tenman.booker;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.openqa.selenium.By;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.codeborne.selenide.Condition.exist;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;
import static com.codeborne.selenide.Selenide.closeWebDriver;
import static com.codeborne.selenide.Selenide.open;
import static java.time.LocalTime.MIDNIGHT;
import static org.openqa.selenium.By.className;
import static org.openqa.selenium.By.id;
import static org.openqa.selenium.By.linkText;
import static org.openqa.selenium.By.tagName;

@Service
@Slf4j
public class RegisterService {

    private static final org.joda.time.format.DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormat.forPattern("dd MMM yyyy HH:mm");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private final String startTime;
    private final String endTime;
    private final LoginService loginService;
    private final UnRegisterService unRegisterService;
    private Set<LocalDateTime> dates = new HashSet<>();

    public RegisterService(
            @Value("${startTime}") String startTime,
            @Value("${endTime}") String endTime,
            LoginService loginService, UnRegisterService unRegisterService) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.loginService = loginService;
        this.unRegisterService = unRegisterService;
        LocalDateTime now = LocalDateTime.now();
        for (int i = 1; i <= 6; i++) {
            dates.add(LocalDateTime.of(now.plusDays(i).toLocalDate(), MIDNIGHT));
        }
    }

    private List<Booking> fetchActiveBookings() {
        log.info("Fetching active bookings");
        open("https://better.legendonlineservices.co.uk/poplar_baths/BookingsCentre/MyBookings");
        ElementsCollection foundActiveBookings = $$(By.tagName("p")).filter(text("Cancel Booking"));
        List<Booking> activeBookings = foundActiveBookings.stream()
                .map(booking -> {
                    String place = booking.parent().find(By.tagName("h5")).text();
                    String date = booking.text()
                            .split("Date: ")[1]
                            .split(" - ")[0];
                    DateTime dateTime = DATE_TIME_FORMATTER.parseDateTime(date);
                    LocalDateTime localDateTime = LocalDateTime.of(
                            dateTime.getYear(), dateTime.getMonthOfYear(), dateTime.getDayOfMonth(),
                            dateTime.getHourOfDay(), dateTime.getMinuteOfHour(), 0, 0
                    );
                    String cancelBookingUrl = booking.find(By.linkText("Cancel Booking"))
                            .getAttribute("href");
                    return Booking.builder()
                            .startingDateTime(localDateTime)
                            .placeName(place)
                            .booked(true)
                            .cancelBookingUrl(cancelBookingUrl)
                            .build();
                })
                .collect(Collectors.toList());
        log.info("Active bookings {}", activeBookings);
        return activeBookings;
    }

    @Scheduled(cron = "45 1/2 * * * ?")
    public void registerToCustomTime() {
        long start = System.nanoTime();
        LocalDateTime now = LocalDateTime.now();
        if (now.getHour() >= 18 || (now.getHour() == 17 && now.getMinute() == 59)) {
            dates.add(LocalDateTime.of(now.plusDays(7).toLocalDate(), MIDNIGHT));
        }
        loginService.login();
        List<Booking> activeBookings = fetchActiveBookings();
        selectSwimmingActivity();
        List<LocalDateTime> datesToRemove = new ArrayList<>();
        dates.stream()
                .filter(dateTime -> alreadyRegisteredToDayAndSixThirty(dateTime, activeBookings))
                .forEach(dateTime -> {
                    log.info("Already registered. dateTime: {}, activeBooking {}", dateTime, activeBookings);
                    datesToRemove.add(dateTime);
                });
        if (!datesToRemove.isEmpty()) {
            log.info("Dates to remove {}", datesToRemove);
            dates.removeAll(datesToRemove);
            log.info("Dates: {}", dates);
        }
        dates.forEach(dateTime -> register(dateTime, getStartingTimes(dateTime), activeBookings));
        logout();
        tearDown(start);
    }

    private void logout() {
        open("https://better.legendonlineservices.co.uk/poplar_baths/Account/Logout");
        Selenide.closeWindow();
    }

    boolean alreadyRegisteredToDay(LocalDateTime dateTime, List<Booking> activeBookings) {
        return activeBookings.stream()
                .anyMatch(booking -> booking.getStartingDateTime().getDayOfMonth() == dateTime.getDayOfMonth() &&
                        booking.getStartingDateTime().getMonthValue() == dateTime.getMonthValue() &&
                        booking.getStartingDateTime().getYear() == dateTime.getYear());
    }

    boolean alreadyRegisteredToDayAndSixThirty(LocalDateTime dateTime, List<Booking> activeBookings) {
        return activeBookings.stream()
                .anyMatch(booking -> booking.getStartingDateTime().getDayOfMonth() == dateTime.getDayOfMonth() &&
                        booking.getStartingDateTime().getMonthValue() == dateTime.getMonthValue() &&
                        booking.getStartingDateTime().getYear() == dateTime.getYear() &&
                        booking.getStartingDateTime().getHour() == 6 &&
                        booking.getStartingDateTime().getMinute() == 30
                );
    }

    boolean register(LocalDateTime start, List<String> times, List<Booking> activeBookings) {
        try {
            open("https://better.legendonlineservices.co.uk/poplar_baths/BookingsCentre/Timetable?KeepThis=true&#");
            ElementsCollection elementsCollection = $$(tagName("a")).filter(text("basket"));
            log.info("Starting to analyze... {}", start);
            for (SelenideElement e : elementsCollection) {
                String text = e.closest("tr").getText();
                boolean isValidDay = text.contains("London") && text.contains(start.format(DATE_FORMATTER));
                if (isValidDay && times.stream().anyMatch(text::contains)) {
                    log.info("Found: {}", text);
                    String[] strings = text.split(" - ")[0].split(" ");
                    String time = strings[strings.length - 1];
                    LocalDateTime currentSlot = LocalDateTime.of(start.toLocalDate(), LocalTime.parse(time, DateTimeFormatter.ofPattern("HH:mm")));
                    if (alreadyRegisteredToDay(start, activeBookings) && isLater(currentSlot, activeBookings)) {
                        log.info("Trying to unregister");
                        boolean unRegistered = unRegisterService.unRegister(findClosestBooking(start, activeBookings).getCancelBookingUrl());
                        if (!unRegistered) {
                            return false;
                        }
                        loginService.login();
                        selectSwimmingActivity();
                        open("https://better.legendonlineservices.co.uk/poplar_baths/BookingsCentre/Timetable?KeepThis=true&#");
                        elementsCollection = $$(tagName("a")).filter(text("basket"));
                        for (SelenideElement e2 : elementsCollection) {
                            text = e2.closest("tr").getText();
                            isValidDay = text.contains("London") && text.contains(start.format(DATE_FORMATTER));
                            if (isValidDay && times.stream().anyMatch(text::contains)) {
                                log.info("Found: {}", text);
                                log.info("Registering...");
                                e2.click();
                                log.info("Registered!");
                                return true;
                            }
                        }
                        return false;
                    } else if (alreadyRegisteredToDay(start, activeBookings)) {
                        log.info("Skipping. Already registered {}", findClosestBooking(start, activeBookings));
                        return false;
                    } else {
                        log.info("Registering...");
                        e.click();
                        agreeToBookingTerms();
                        log.info("Registered!");
                        return true;
                    }
                }
            }
            return false;
        } catch (Exception e) {
            log.error("Failed to register. Error ", e);
            return false;
        }
    }

    private boolean isLater(LocalDateTime currentSlot, List<Booking> activeBookings) {
        Booking foundBooking = findClosestBooking(currentSlot, activeBookings);
        boolean after = foundBooking.getStartingDateTime().isAfter(currentSlot);
        if (after) {
            log.info("Found booking {} is after current slot {}", foundBooking, currentSlot);
        }
        return after;
    }

    Booking findClosestBooking(LocalDateTime localDateTime, List<Booking> activeBookings) {
        return activeBookings.stream()
                .filter(b -> b.getStartingDateTime().toLocalDate().equals(localDateTime.toLocalDate()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Couldn't find the closest date"));
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
        while (start.isBefore(end) || start.equals(end)) {
            result.add(start.format(TIME_FORMATTER));
            start = start.plusMinutes(10);
        }
        return result;
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
            log.info("Agreed to booking terms");
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void selectSwimmingActivity() {
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

    public double duration(long start, long finish) {
        return (finish - start) / 1_000_000_000.0;
    }

}
