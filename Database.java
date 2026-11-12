import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class Database {
    private Connection conn;
    private String db_url,db_user,db_pass;

    public static void main(String[] args){ 
        Database db = new Database();

        try (Connection conn = db.connect()) {
            System.out.println("Database connected");
        } catch (SQLException e) {
            System.out.println("Database connection failed: " + e.getMessage());
        }
    }

    Connection connect() throws SQLException {
        db_url = "jdbc:sqlserver://localhost;"
        + "instanceName=SQLEXPRESS;"
        + "databaseName=sgr;"
        + "encrypt=true;trustServerCertificate=true;";
        
        Properties settings = new Properties();
        try (FileInputStream file = new FileInputStream("database.properties")) {
            settings.load(file);
        } catch (IOException e) {
            throw new SQLException("Cannot read database.properties; copy database.properties.example and set your login", e);
        }
        db_user = settings.getProperty("user");
        db_pass = settings.getProperty("password");
        conn = DriverManager.getConnection(db_url, db_user, db_pass);
        return conn;
    }
}
