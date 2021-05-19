package ee.tenman.booker;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;
import com.google.common.collect.ImmutableList;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Selenide.$$;
import static com.codeborne.selenide.Selenide.closeWebDriver;
import static com.codeborne.selenide.Selenide.open;
import static java.time.LocalTime.MIDNIGHT;
import static org.openqa.selenium.By.tagName;

@Service
@Slf4j
public class RegisterService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private final String startTime;
    private final String endTime;
    private final LoginService loginService;
    private final BookingService bookingService;
    private final UnRegisterService unRegisterService;
    private final SelectionService selectionService;
    private final TermsService termsService;
    private Set<LocalDateTime> dates = new HashSet<>();
    private List<LocalDate> datesToRemove = ImmutableList.of(

    );

    public RegisterService(
            @Value("${startTime}") String startTime,
            @Value("${endTime}") String endTime,
            LoginService loginService, BookingService bookingService, UnRegisterService unRegisterService, SelectionService selectionService, TermsService termsService) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.loginService = loginService;
        this.bookingService = bookingService;
        this.unRegisterService = unRegisterService;
        this.selectionService = selectionService;
        this.termsService = termsService;
        LocalDateTime now = LocalDateTime.now();
        for (int i = 1; i <= 6; i++) {
            dates.add(LocalDateTime.of(now.plusDays(i).toLocalDate(), MIDNIGHT));
        }
        for (LocalDate localDate : datesToRemove) {
            dates.remove(LocalDateTime.of(localDate, MIDNIGHT));
        }
    }

    @Scheduled(cron = "45 * * * * ?")
    public void registerToCustomTime() {
        long start = System.nanoTime();
        LocalDateTime now = LocalDateTime.now();
        if (now.getHour() >= 18 || (now.getHour() == 17 && now.getMinute() == 59)) {
            dates.add(LocalDateTime.of(now.plusDays(7).toLocalDate(), MIDNIGHT));
        }
        loginService.login();
        List<Booking> activeBookings = bookingService.fetchActiveBookings();
        unRegisterNotNeededBookings(activeBookings);
        selectionService.selectSwimmingActivity();
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

    private void unRegisterNotNeededBookings(List<Booking> activeBookings) {
        for (Booking activeBooking : activeBookings) {
            if (datesToRemove.contains(activeBooking.getStartingDateTime().toLocalDate()) &&
                    activeBooking.getStartingDateTime().getHour() <= 7) {
                log.info("Date: {}. Unregistering not needed booking {}",
                        activeBooking.getStartingDateTime(), activeBooking);
                unRegisterService.unRegister(activeBooking.getCancelBookingUrl());
            }
        }
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
                        selectionService.selectSwimmingActivity();
                        open("https://better.legendonlineservices.co.uk/poplar_baths/BookingsCentre/Timetable?KeepThis=true&#");
                        elementsCollection = $$(tagName("a")).filter(text("basket"));
                        for (SelenideElement e2 : elementsCollection) {
                            text = e2.closest("tr").getText();
                            isValidDay = text.contains("London") && text.contains(start.format(DATE_FORMATTER));
                            if (isValidDay && times.stream().anyMatch(text::contains)) {
                                log.info("Found: {}", text);
                                log.info("Registering...");
                                e2.click();
                                termsService.agreeToBookingTerms();
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
                        termsService.agreeToBookingTerms();
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

    private boolean isBefore(LocalDateTime currentSlot, List<Booking> activeBookings) {
        Booking foundBooking = findClosestBooking(currentSlot, activeBookings);
        boolean before = foundBooking.getStartingDateTime().isBefore(currentSlot);
        if (before) {
            log.info("Found booking {} is before current slot {}", foundBooking, currentSlot);
        }
        return before;
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

    public double duration(long start, long finish) {
        return (finish - start) / 1_000_000_000.0;
    }

}
