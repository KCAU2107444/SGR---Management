import java.sql.Time;
import java.time.Duration;
public class schedule {
    private int schedule_id,train_id,route_id;
    private Time departure_time,arrival_time;

    public int get_duration() {
        if (departure_time == null || arrival_time == null) {
            throw new IllegalStateException("Departure and arrival times must be set");
        }
        Duration duration = Duration.between(departure_time.toLocalTime(), arrival_time.toLocalTime());
        if (duration.isNegative()) {
            duration = duration.plusDays(1);
        }
        return (int) duration.toMinutes();
    }

    public String get_details() {
        return "Schedule ID: " + schedule_id
                + ", Train ID: " + train_id
                + ", Route ID: " + route_id
                + ", Departure: " + departure_time
                + ", Arrival: " + arrival_time
                + ", Duration: " + get_duration() + " minutes";
    }
}
