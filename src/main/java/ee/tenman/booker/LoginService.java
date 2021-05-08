package ee.tenman.booker;

import com.codeborne.selenide.Selenide;
import com.google.common.collect.ImmutableMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.util.Map;

import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.open;
import static org.openqa.selenium.By.id;

@Service
@Slf4j
public class LoginService {

    @Value("${password}")
    String password;

    @Value("${email}")
    String email;

    @Retryable(value = {Exception.class}, maxAttempts = 2, backoff = @Backoff(delay = 300))
    public Map<String, Object> login() {
        long start = System.nanoTime();
        Selenide.closeWebDriver();
        open("https://better.legendonlineservices.co.uk/enterprise/account/login");
        $(id("login_Email")).setValue(email);
        $(id("login_Password")).setValue(password);
        $(id("login")).click();
        log.info("Login succeeded");
        return ImmutableMap.of("loginSucceed", true, "duration in seconds", duration(start, System.nanoTime()));
    }

    public double duration(long start, long finish) {
        return (finish - start) / 1_000_000_000.0;
    }

}
