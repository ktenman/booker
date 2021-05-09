package ee.tenman.booker;

import com.codeborne.selenide.ElementsCollection;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;
import static com.codeborne.selenide.Selenide.open;
import static org.openqa.selenium.By.className;
import static org.openqa.selenium.By.id;
import static org.openqa.selenium.By.tagName;

@Service
@Slf4j
public class SelectionService {

    @Retryable(value = {Exception.class}, maxAttempts = 3, backoff = @Backoff(delay = 500))
    public void selectSwimmingActivity() {
        open("https://better.legendonlineservices.co.uk/poplar_baths/BookingsCentre/Index");
        ElementsCollection selenideElements = $(className("cscLeftPane"))
                .$$(className("clubResult"));
        ElementsCollection links = $$(tagName("a"));
        links.find(text("Tower Hamlets"))
                .click();
        selenideElements.find(text("Poplar Baths LC"))
                .$(tagName("label"))
                .click();
        links.find(text("Hackney"))
                .click();
        selenideElements.find(text("London Fields Lido"))
                .$(tagName("label"))
                .click();
        $(id("behaviours")).$$(className("activityItem"))
                .find(text("Swim"))
                .$(tagName("label"))
                .click();
        $(id("activities")).$$(className("activityItem"))
                .find(text("Swim for Fitness"))
                .$(tagName("label"))
                .click();
        $(id("bottomsubmit"))
                .click();
        log.info("Swimming activity selected");
    }

}
