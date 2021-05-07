package ee.tenman.booker;

import com.codeborne.selenide.Selenide;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Selenide.$$;
import static com.codeborne.selenide.Selenide.open;
import static org.openqa.selenium.By.tagName;

@Service
@Slf4j
public class UnRegisterService {

    @Resource
    private LoginService loginService;

    public boolean unRegister(String link) {
        try {
            loginService.login();
            open(link);
            $$(tagName("a")).find(text("Cancel Booking")).click();
            Selenide.closeWebDriver();
            log.info("Unregistered {}", link);
            return true;
        } catch (Exception e) {
            log.error("Failed to unregister ", e);
            return false;
        }
    }

}
