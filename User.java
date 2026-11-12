import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import at.favre.lib.crypto.bcrypt.BCrypt;

public class User {
    private int id;
    private String fullname, username, password;

    private String userRole;

    void save() throws SQLException {
        create_user(fullname, username, password);
    }

    String password_hash(String password) {
        // BCrypt generates a random salt and includes it in the stored hash.
        return BCrypt.withDefaults().hashToString(12, password.toCharArray());
    }

    void create_user(String fullname, String username, String password) throws SQLException {
        if (fullname == null || fullname.isBlank() || username == null || username.isBlank()
                || password == null || password.isEmpty()) {
            throw new IllegalArgumentException("Full name, username and password are required");
        }
        Database db = new Database();
        String hash = password_hash(password);
        String sql = "INSERT INTO dbo.Users (FullName, Username, PasswordHash, UserRole) VALUES (?, ?, ?, ?)";
        try (Connection conn = db.connect();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, fullname);
            stmt.setString(2, username);
            stmt.setString(3, hash);
            // CreatedAt uses its database default; new accounts are passengers.
            stmt.setString(4, "Passenger");
            stmt.executeUpdate();
        }
    }

    boolean login(String username, String password) throws SQLException {
        id = 0;
        userRole = null;
        if (username == null || password == null) {
            return false;
        }
        Database db = new Database();
        String sql = "Select UserID, PasswordHash, UserRole from dbo.Users WHERE Username = ?";
        try (Connection conn = db.connect();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next() && BCrypt.verifyer()
                        .verify(password.toCharArray(), rs.getString("PasswordHash")).verified) {
                    id = rs.getInt("UserID");
                    userRole = rs.getString("UserRole");
                    return true;
                }
            }
        }
        return false;
    }

    void logout() {
        id = 0;
        userRole = null;
    }

    void view_trains() throws SQLException {
        String sql = "SELECT TrainID, TrainName, TrainType, TrainStatus, Capacity FROM dbo.Trains";
        Database db = new Database();

        try (Connection conn = db.connect();
                PreparedStatement stmt = conn.prepareStatement(sql);
                ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                System.out.printf("%d | %s | %s | %s | Capacity: %d%n",
                        rs.getInt("TrainID"), rs.getString("TrainName"),
                        rs.getString("TrainType"), rs.getString("TrainStatus"),
                        rs.getInt("Capacity"));
            }
        }
    }

    void passenger_details() {

    };

    void admin_control() {

    };

}
