package ee.tenman.booker;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@RestController
public class TestingController {

    @Resource
    private BookingService bookingService;

    @GetMapping("test")
    public void test() {
        bookingService.login();
    }

}
