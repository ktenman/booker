package ee.tenman.booker;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class BookingServiceIntegrationTest {

    @Resource
    BookingService bookingService;

    @Test
    void registerToCustomTime() throws InterruptedException {
        bookingService.registerToCustomTime();
    }
}