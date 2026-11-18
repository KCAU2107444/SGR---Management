package ui.gui;

import db.DatabaseManager;
import model.Booking;
import model.Passenger;
import model.PrintableReceipt;
import model.Route;
import model.Schedule;
import model.SeatClass;
import model.Train;
import model.Transaction;
import service.CardPayment;
import service.MpesaPayment;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.List;
import java.util.stream.IntStream;

public class PassengerMenu {
    private static final DatabaseManager db = DatabaseManager.getInstance();

    static void open(GuiMain app, Passenger passenger) {
        Runnable schedules = () -> app.showPage("Train schedules", "No schedules available.",
                RecordTable::schedules,
                new String[] {"Book a ticket"}, () -> bookTicket(passenger));
        app.nav("Train schedules", schedules);
        app.nav("My bookings", () -> app.showPage("My bookings", "You have no bookings yet.",
                () -> RecordTable.bookings(db.getBookingsForPassenger(passenger.getUserId()), false), new String[] {"Cancel ticket", "View receipt"},
                () -> cancelTicket(passenger), () -> printReceipt(passenger)));
        schedules.run();
    }

    private static void bookTicket(Passenger p) {
        List<Schedule> schedules = db.getRecords("schedule").stream().map(Schedule.class::cast).toList();
        if (schedules.isEmpty()) {
            InlineForm.println("No schedules available.");
            return;
        }
        Schedule schedule = null;
        Train train = null;
        Route route = null;
        LocalDate date = null;
        Integer coach = null;
        String seat = null;
        String phone = "";
        String holder = "";
        String number = "";
        String expiry = "";
        double fare = 0;
        int method = 0;
        int step = 0;
        Transaction tx = null;
        DateTimeFormatter dateLabel = DateTimeFormatter.ofPattern("EEEE, d MMM yyyy", Locale.ENGLISH);
        try {
            while (tx == null) {
                InlineForm.setBackEnabled(step > 0);
                try {
                    switch (step) {
                        case 0 -> {
                            int row = InlineForm.selectRow("Select a schedule", RecordTable.schedules(schedules),
                                    "Continue", schedule == null ? 0 : schedules.indexOf(schedule));
                            schedule = schedules.get(row);
                            train = (Train) db.findRecord("train", schedule.getTrainId());
                            route = (Route) db.findRecord("route", schedule.getRouteId());
                            if (train == null || route == null || !"Active".equalsIgnoreCase(train.getStatus())
                                    || train.getTotalCoaches() < 1) {
                                InlineForm.println("This schedule is unavailable. Select another schedule.");
                                continue;
                            }
                        }
                        case 1 -> date = InlineForm.choose("Select a travel date", Booking.upcomingTravelDates(),
                                dateLabel::format, Booking.BOOKABLE_DATE_COUNT, date);
                        case 2 -> {
                            List<Integer> coaches = IntStream.rangeClosed(1, train.getTotalCoaches()).boxed().toList();
                            Route selectedRoute = route;
                            Object[][] rows = coaches.stream().map(candidate -> {
                                SeatClass seatClass = SeatClass.forCoach(candidate);
                                return new Object[] {candidate, seatClass.getName(), selectedRoute.calculateFare(seatClass)};
                            }).toArray(Object[][]::new);
                            RecordTable coachOptions = new RecordTable(new String[] {"Coach", "Class", "Fare (KES)"}, rows);
                            int row = InlineForm.selectRow("Select a coach", coachOptions, "Continue",
                                    coach == null ? 0 : coaches.indexOf(coach));
                            coach = coaches.get(row);
                        }
                        case 3 -> {
                            List<String> taken = db.getTakenSeats(schedule.getScheduleId(), date, String.valueOf(coach));
                            List<String> seats = IntStream.rangeClosed(1, train.getSeatsPerCoach())
                                    .mapToObj(String::valueOf).filter(candidate -> !taken.contains(candidate)).toList();
                            if (seats.isEmpty()) {
                                InlineForm.println("No seats available in this coach. Select another coach.");
                                step = 2;
                                continue;
                            }
                            seat = InlineForm.selectSeat(seats, seat);
                        }
                        case 4 -> {
                            SeatClass seatClass = SeatClass.forCoach(coach);
                            fare = route.calculateFare(seatClass);
                            InlineForm.reviewTrip(new Object[][] {
                                    {"Train", train.getTrainName()},
                                    {"Journey", route.getOrigin() + " → " + route.getDestination()},
                                    {"Travel date", dateLabel.format(date)},
                                    {"Departure", schedule.getDepartureTime()},
                                    {"Arrival", schedule.getArrivalTime()},
                                    {"Class", seatClass.getName()},
                                    {"Coach", coach},
                                    {"Seat", seat},
                                    {"Total fare", String.format("KES %,.2f", fare)}
                            });
                        }
                        case 5 -> method = InlineForm.select("Payment method", "M-Pesa", "Debit / Credit card");
                        case 6 -> {
                            if (method == 1) {
                                phone = InlineForm.validatedText("M-Pesa phone number", phone, MpesaPayment::isValidPhone,
                                        "Enter a valid number, e.g. 0712345678 or +254712345678.");
                            } else {
                                holder = requiredText("Name on card", holder);
                            }
                        }
                        case 7 -> {
                            if (method == 1) {
                                int seconds = java.util.concurrent.ThreadLocalRandom.current().nextInt(1, 6);
                                InlineForm.processing("Waiting for M-Pesa approval on " + phone, seconds * 1000);
                                tx = new MpesaPayment(phone).process(fare);
                            } else {
                                number = requiredText("Card number", number);
                            }
                        }
                        case 8 -> expiry = requiredText("Expiry date (MM/YY)", expiry);
                        case 9 -> {
                            String cvv = InlineForm.password("CVV (3 or 4 digits on the back)");
                            try {
                                tx = new CardPayment(holder, number, expiry, cvv).process(fare);
                            } catch (IllegalArgumentException e) {
                                InlineForm.println("Card declined: " + e.getMessage());
                                step = 6;
                                continue;
                            }
                        }
                    }
                    step++;
                } catch (InlineForm.Back ignored) {
                    step--;
                }
            }
        } finally {
            InlineForm.setBackEnabled(false);
        }

        Booking b = new Booking(p.getUserId(), schedule.getScheduleId(), route.getOrigin(), route.getDestination(), date, fare);
        b.setPassengerName(p.getFullName());
        b.selectCoachAndSeat(String.valueOf(coach), seat);
        b.setPayment(tx);
        if (b.confirmBooking()) {
            PrintableReceipt r = b.generateReceipt();
            Path saved = r.saveToFile();
            String success = "Payment received via " + tx.describeMethod() + " (" + tx.getReferenceCode()
                    + "). Booking confirmed!";
            if (method == 1) {
                InlineForm.println(success);
                InlineForm.acknowledge("Payment successful");
            } else {
                InlineForm.println(success);
            }
            InlineForm.showReceipt(r);
            InlineForm.println("Receipt saved to: " + saved);
        } else {
            InlineForm.println("Sorry, that seat was just taken. You have not been booked; please try again.");
        }
    }

    private static String requiredText(String prompt, String previous) {
        return InlineForm.validatedText(prompt, previous, value -> !value.isEmpty(), "Please enter a value.");
    }

    private static Booking selectBooking(Passenger passenger, String prompt) {
        List<Booking> bookings = db.getBookingsForPassenger(passenger.getUserId());
        if (bookings.isEmpty()) {
            InlineForm.println("You have no bookings yet.");
            return null;
        }
        return InlineForm.choose(prompt, bookings, Booking::toString);
    }

    private static void cancelTicket(Passenger p) {
        Booking b = selectBooking(p, "Select a booking to cancel");
        if (b == null || !InlineForm.confirm("Cancel booking #" + b.getBookingId() + "?")) return;
        InlineForm.println(b.cancelTicket()
                ? "Ticket cancelled. Your refund is marked pending."
                : "Only CONFIRMED tickets can be cancelled.");
    }

    private static void printReceipt(Passenger p) {
        List<Booking> bookings = db.getBookingsForPassenger(p.getUserId()).stream()
                .filter(booking -> db.findTransactionByBooking(booking.getBookingId()) != null)
                .toList();
        if (bookings.isEmpty()) {
            InlineForm.println("You have no receipts yet.");
            return;
        }
        int row = InlineForm.selectRow("Select a booking to view its receipt",
                RecordTable.bookings(bookings, false), "View receipt");
        InlineForm.showReceipt(bookings.get(row).generateReceipt());
    }
}
