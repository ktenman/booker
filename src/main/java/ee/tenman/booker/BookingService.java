package ee.tenman.booker;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.Selenide;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.openqa.selenium.By;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Selenide.$$;
import static com.codeborne.selenide.Selenide.open;
import static org.openqa.selenium.By.tagName;

@Service
@Slf4j
public class BookingService {

    private static final org.joda.time.format.DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormat.forPattern("dd MMM yyyy HH:mm");

    @Resource
    private LoginService loginService;

    @Retryable(value = {Exception.class}, maxAttempts = 3, backoff = @Backoff(delay = 500))
    public List<Booking> fetchActiveBookings() {
        log.info("Fetching active bookings");
        List<Booking> activeBookings = getBookings();
        if (getBookings().isEmpty()) {
            Selenide.refresh();
            loginService.login();
            activeBookings = getBookings();
            if (activeBookings.isEmpty()) {
                Selenide.refresh();
                loginService.login();
                activeBookings = getBookings();
            }
        }
        log.info("Active bookings {}", activeBookings);
        return activeBookings;
    }

    private List<Booking> getBookings() {
        open("https://better.legendonlineservices.co.uk/poplar_baths/BookingsCentre/MyBookings");
        ElementsCollection foundActiveBookings = $$(By.tagName("a"))
                .filter(text("Cancel Booking"));
        return foundActiveBookings.stream()
                .map(booking -> {
                    String place = booking.parent()
                            .parent()
                            .find(tagName("h5"))
                            .text();
                    String date = booking
                            .parent()
                            .text()
                            .split("Date: ")[1]
                            .split(" - ")[0];
                    DateTime dateTime = DATE_TIME_FORMATTER.parseDateTime(date);
                    LocalDateTime localDateTime = LocalDateTime.of(
                            dateTime.getYear(), dateTime.getMonthOfYear(), dateTime.getDayOfMonth(),
                            dateTime.getHourOfDay(), dateTime.getMinuteOfHour(), 0, 0
                    );
                    String cancelBookingUrl = booking.getAttribute("href");
                    return Booking.builder()
                            .startingDateTime(localDateTime)
                            .placeName(place)
                            .booked(true)
                            .cancelBookingUrl(cancelBookingUrl)
                            .build();
                })
                .collect(Collectors.toList());
    }

}
