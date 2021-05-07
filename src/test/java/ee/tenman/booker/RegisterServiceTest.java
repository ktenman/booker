package ee.tenman.booker;

import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class RegisterServiceTest {

    LoginService loginService = new LoginService();
    UnRegisterService unRegisterService = new UnRegisterService();
    RegisterService registerService = new RegisterService("06:30", "07:00", loginService, unRegisterService);

    @Test
    void alreadyRegistered() {
        ImmutableList<Booking> bookings = ImmutableList.of(Booking.builder()
                .startingDateTime(LocalDateTime.parse("2021-05-07T06:40:00"))
                .build()
        );

        boolean alreadyRegistered = registerService.alreadyRegisteredToDay(LocalDateTime.parse("2021-05-07T06:30:00"), bookings);

        assertThat(alreadyRegistered).isTrue();
    }

    @Test
    void alreadyRegisteredToDayAndSixThirty() {
        ImmutableList<Booking> bookings = ImmutableList.of(Booking.builder()
                .startingDateTime(LocalDateTime.parse("2021-05-07T06:30:00"))
                .build()
        );

        boolean alreadyRegistered = registerService.alreadyRegisteredToDayAndSixThirty(LocalDateTime.parse("2021-05-07T06:30:00"), bookings);

        assertThat(alreadyRegistered).isTrue();
    }
}