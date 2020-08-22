package ee.tenman.booker;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class TestingController {

    private final BookingService bookingService;

    public TestingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @GetMapping("/test")
    public Map<String, Boolean> test() {
        return bookingService.login();
    }

}
