package ee.tenman.booker;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class BookingServiceTest {

    BookingService bookingService;

    @Test
    void testRegisterToCustomTime() {
        bookingService = new BookingService("a", "b", "06:30", "07:00");

        List<String> times = bookingService.getStartingTimes(LocalDateTime.now());

        assertThat(times).containsExactlyInAnyOrder("06:30", "06:40", "06:50", "07:00");
    }

    @Test
    void testRegisterToCustomTimePm() {
        bookingService = new BookingService("a", "b", "17:00", "19:00");

        List<String> times = bookingService.getStartingTimes(LocalDateTime.now());

        assertThat(times).hasSize(13).contains("17:00", "18:30", "18:40", "19:00");
    }

}
