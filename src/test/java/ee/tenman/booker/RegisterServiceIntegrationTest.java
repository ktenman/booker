package ee.tenman.booker;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

@SpringBootTest
class RegisterServiceIntegrationTest {

    @Resource
    RegisterService bookingService;

    @Test
    void registerToCustomTime() throws InterruptedException {
        bookingService.registerToCustomTime();
    }
}