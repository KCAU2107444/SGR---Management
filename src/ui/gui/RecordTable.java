package ui.gui;

import db.DatabaseManager;
import model.*;

import java.util.List;

record RecordTable(String[] columns, Object[][] rows, String summary) {
    RecordTable(String[] columns, Object[][] rows) {
        this(columns, rows, "");
    }

    private static final DatabaseManager db = DatabaseManager.getInstance();

    static RecordTable trains() {
        return new RecordTable(new String[] {"ID", "Train", "Type", "Status", "Coaches", "Seats / coach"},
                db.getRecords("train").stream().map(Train.class::cast).map(t -> new Object[] {
                        t.getTrainId(), t.getTrainName(), t.getTrainType(), t.getStatus(),
                        t.getTotalCoaches(), t.getSeatsPerCoach()
                }).toArray(Object[][]::new));
    }

    static RecordTable routes() {
        return new RecordTable(new String[] {"ID", "Origin", "Destination", "Distance (km)"},
                db.getRecords("route").stream().map(Route.class::cast).map(r -> new Object[] {
                        r.getRouteId(), r.getOrigin(), r.getDestination(), r.getDistanceKm()
                }).toArray(Object[][]::new));
    }

    static RecordTable schedules() {
        return schedules(db.getRecords("schedule").stream().map(Schedule.class::cast).toList());
    }

    static RecordTable schedules(List<Schedule> schedules) {
        return new RecordTable(new String[] {"ID", "Origin", "Destination", "Train", "Status",
                "Departure", "Arrival", "Duration (min)"},
                schedules.stream().map(s -> {
                    Route route = (Route) db.findRecord("route", s.getRouteId());
                    Train train = (Train) db.findRecord("train", s.getTrainId());
                    return new Object[] {s.getScheduleId(), route == null ? "#" + s.getRouteId() : route.getOrigin(),
                            route == null ? null : route.getDestination(),
                            train == null ? "#" + s.getTrainId() : train.getTrainName(),
                            train == null ? null : train.getStatus(), s.getDepartureTime(),
                            s.getArrivalTime(), s.getDuration()};
                }).toArray(Object[][]::new));
    }

    static RecordTable bookings(List<Booking> bookings, boolean showPassenger) {
        String[] columns = showPassenger
                ? new String[] {"ID", "Passenger", "Origin", "Destination", "Travel date", "Coach", "Seat", "Fare (KES)", "Status"}
                : new String[] {"ID", "Origin", "Destination", "Travel date", "Coach", "Seat", "Fare (KES)", "Status"};
        return new RecordTable(columns, bookings.stream().map(b -> {
            if (showPassenger) {
                String passenger = b.getPassengerName() == null ? "#" + b.getPassengerId() : b.getPassengerName();
                return new Object[] {b.getBookingId(), passenger, b.getOriginStation(), b.getDestinationStation(),
                        b.getTravelDate(), b.getCoachNumber(), b.getSeatNumber(), b.getFareAmount(), b.getBookingStatus()};
            }
            return new Object[] {b.getBookingId(), b.getOriginStation(), b.getDestinationStation(),
                    b.getTravelDate(), b.getCoachNumber(), b.getSeatNumber(), b.getFareAmount(), b.getBookingStatus()};
        }).toArray(Object[][]::new));
    }

    static RecordTable transactions(List<Transaction> transactions) {
        double total = transactions.stream().filter(t -> "SUCCESS".equals(t.getStatus()))
                .mapToDouble(Transaction::getAmount).sum();
        return new RecordTable(new String[] {"ID", "Booking", "Method", "Reference", "Amount (KES)", "Paid at", "Status"},
                transactions.stream().map(t -> new Object[] {t.getTransactionId(), t.getBookingId(),
                        t.describeMethod(), t.getReferenceCode(), t.getAmount(), t.getPaymentDate(), t.getStatus()
                }).toArray(Object[][]::new), String.format("Total collected: KES %,.2f", total));
    }
}
