import java.util.Date;

public class booking {
    private int booking_id,passenger_id;
    private String origin_station,coach_number,booking_status,destination_station,seat_number;
    private Date travel_date;
    private double fare_amount;

    public void select_coach_and_seat(String coach_number, String seat_number) {
        //select coach and seat
    }

    public boolean confirm_booking() {
        //confirm booking
        return true;
    }

    public PrintableReceipt generate_receipt() {
        //generate receipt
    }

    public boolean cancel_ticket() {
        //cancel booking
        return true;
    }


}
