package ee.tenman.booker;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

@SpringBootTest
class UnRegisterServiceTest {

    @Resource
    private UnRegisterService unRegisterService;

    @Test
    void unRegister() {
        unRegisterService.unRegister("https://better.legendonlineservices.co.uk/poplar_baths/BookingsCentre/CancelBooking?bookingid=48879183");
    }
}