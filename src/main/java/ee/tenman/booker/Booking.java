package ee.tenman.booker;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.Objects;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class Booking {
    private LocalDateTime startingDateTime;
    private boolean booked;
    private String placeName;
    private String cancelBookingUrl;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Booking booking = (Booking) o;
        return startingDateTime.equals(booking.startingDateTime) &&
                placeName.equals(booking.placeName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(startingDateTime, placeName);
    }
}
