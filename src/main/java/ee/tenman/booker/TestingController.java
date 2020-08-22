package ee.tenman.booker;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.Map;

@RestController
public class TestingController {

    @Resource
    private BookingService bookingService;

    @GetMapping("test")
    public Map<String, Boolean> test() {
        return bookingService.login();
    }

}
