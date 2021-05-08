package ee.tenman.booker;

import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import static com.codeborne.selenide.Condition.exist;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.open;
import static org.openqa.selenium.By.id;

@Service
@Slf4j
public class TermsService {

    @Retryable(value = {Exception.class}, maxAttempts = 3, backoff = @Backoff(delay = 300))
    public boolean agreeToBookingTerms() {
        open("https://better.legendonlineservices.co.uk/poplar_baths/Basket/Index");
        $(id("agreeBookingTerms")).waitUntil(exist, 30_000);
        $(id("agreeBookingTerms")).click();
        $(id("btnPayNow")).waitUntil(exist, 30_000);
        $(id("btnPayNow")).click();
        log.info("Agreed to booking terms");
        return true;
    }

}
