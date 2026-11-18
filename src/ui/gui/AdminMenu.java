package ui.gui;

import db.DatabaseManager;
import model.Admin;
import model.Booking;
import model.Route;
import model.Schedule;
import model.Train;

import java.time.LocalTime;
import java.util.List;

final class AdminMenu {
    private static final DatabaseManager db = DatabaseManager.getInstance();
    private static final String[] EDIT_ACTIONS = {"Add", "Update", "Delete"};

    static void open(GuiMain app, Admin admin) {
        Runnable trains = () -> app.showPage("Trains", "No trains available.",
                RecordTable::trains, EDIT_ACTIONS,
                () -> addTrain(admin), () -> updateTrain(admin), () -> deleteTrain(admin));
        app.nav("Trains", trains);
        app.nav("Routes", () -> app.showPage("Routes", "No routes available.",
                RecordTable::routes, EDIT_ACTIONS,
                () -> addRoute(admin), () -> updateRoute(admin), () -> deleteRoute(admin)));
        app.nav("Schedules", () -> app.showPage("Schedules", "No schedules available.",
                RecordTable::schedules, EDIT_ACTIONS,
                () -> addSchedule(admin), () -> updateSchedule(admin), () -> deleteSchedule(admin)));
        app.nav("Transactions", () -> app.showPage("Transactions", "No transactions yet.",
                () -> RecordTable.transactions(admin.viewAllTransactions()), new String[0]));
        app.nav("Bookings", () -> app.showPage("All bookings", "No bookings yet.",
                () -> RecordTable.bookings(db.getRecords("booking").stream().map(Booking.class::cast).toList(), true), new String[0]));
        trains.run();
    }

    private static <T> T choose(String type, Class<T> model, String prompt) {
        List<T> records = db.getRecords(type).stream().map(model::cast).toList();
        if (records.isEmpty()) {
            InlineForm.println("No " + type + " records available.");
            throw new InlineForm.Cancelled();
        }
        return InlineForm.choose(prompt, records, Object::toString);
    }

    private static void addTrain(Admin admin) {
        String name = InlineForm.text("Train name");
        String type = InlineForm.select("Train type", "Express", "Intercounty") == 2 ? "Intercounty" : "Express";
        admin.addTrain(new Train(0, name, type, askStatus(), InlineForm.integer("Total coaches")));
        InlineForm.println("Train added.");
    }

    private static void updateTrain(Admin admin) {
        Train train = choose("train", Train.class, "Select a train to update");
        String name = InlineForm.optional("New name (leave blank to keep " + train.getTrainName() + ")");
        String status = askStatus();
        int coaches = InlineForm.integer("Total coaches (currently " + train.getTotalCoaches() + ")");
        admin.updateTrain(new Train(train.getTrainId(), name.isEmpty() ? train.getTrainName() : name,
                train.getTrainType(), status, coaches));
        InlineForm.println("Train updated.");
    }

    private static void deleteTrain(Admin admin) {
        Train train = choose("train", Train.class, "Select a train to delete");
        if (InlineForm.confirm("Delete train " + train.getTrainName() + "?")) {
            admin.deleteTrain(train.getTrainId());
            InlineForm.println("Train deleted.");
        }
    }

    private static String askStatus() {
        return InlineForm.select("Train status", "Active", "Maintenance") == 2 ? "Maintenance" : "Active";
    }

    private static void addRoute(Admin admin) {
        admin.addRoute(new Route(0, InlineForm.text("Origin"), InlineForm.text("Destination"), InlineForm.number("Distance (km)")));
        InlineForm.println("Route added.");
    }

    private static void updateRoute(Admin admin) {
        Route route = choose("route", Route.class, "Select a route to update");
        double distance = InlineForm.number("New distance in km (currently " + route.getDistanceKm() + ")");
        admin.updateRoute(new Route(route.getRouteId(), route.getOrigin(), route.getDestination(), distance));
        InlineForm.println("Route updated.");
    }

    private static void deleteRoute(Admin admin) {
        Route route = choose("route", Route.class, "Select a route to delete");
        if (InlineForm.confirm("Delete the selected route?")) {
            admin.deleteRoute(route.getRouteId());
            InlineForm.println("Route deleted.");
        }
    }

    private static void addSchedule(Admin admin) {
        Route route = choose("route", Route.class, "Select a route");
        Train train = choose("train", Train.class, "Select a train");
        saveSchedule(admin, 0, route.getRouteId(), train.getTrainId());
    }

    private static void updateSchedule(Admin admin) {
        Schedule schedule = choose("schedule", Schedule.class, "Select a schedule to update");
        saveSchedule(admin, schedule.getScheduleId(), schedule.getRouteId(), schedule.getTrainId());
    }

    private static void saveSchedule(Admin admin, int id, int routeId, int trainId) {
        LocalTime departure = InlineForm.time("Departure time (HH:MM)");
        LocalTime arrival = InlineForm.time("Arrival time (HH:MM)");
        Schedule schedule = new Schedule(id, routeId, trainId, departure, arrival);
        if (id == 0) {
            admin.addSchedule(schedule);
            InlineForm.println("Schedule added.");
        } else {
            admin.updateSchedule(schedule);
            InlineForm.println("Schedule updated.");
        }
    }

    private static void deleteSchedule(Admin admin) {
        Schedule schedule = choose("schedule", Schedule.class, "Select a schedule to delete");
        if (InlineForm.confirm("Delete the selected schedule?")) {
            admin.deleteSchedule(schedule.getScheduleId());
            InlineForm.println("Schedule deleted.");
        }
    }
}
