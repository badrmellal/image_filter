package event;

import java.sql.*;
import java.util.*;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import javax.swing.*;


public class DatabaseManager{
    private static final String DB_URL = "jdbc:postgresql://localhost:5432/filter_app";
    private static final String DB_USER = "postgres";
    private static final String DB_PASSWORD = "admin";

    private final Gson gson;

    public DatabaseManager(){
        this.gson = new Gson();
        initializeDatabase();
    }

    private void initializeDatabase() {
        try(Connection connection = getConnection()) {
            createTablesIfNotExist(connection);
        } catch (SQLException e) {
            handleDatabaseError("Error initializing database", e);
        }
    }

    private Connection getConnection() throws SQLException {
        try {
            Class.forName("org.postgresql.Driver");
            return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
        }  catch (ClassNotFoundException e) {
            throw new SQLException("PostgreSQL JDBC Driver not found", e);
        }
    }


    private void createTablesIfNotExist(Connection connection) throws SQLException {
        String createFilterTable = """
            CREATE TABLE IF NOT EXISTS filters (
                id SERIAL PRIMARY KEY,
                name VARCHAR(100) UNIQUE NOT NULL,
                values JSONB NOT NULL,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
        """;

        String createUpdateTrigger = """
            CREATE OR REPLACE FUNCTION update_updated_at()
            RETURNS TRIGGER AS $$
            BEGIN
                NEW.updated_at = CURRENT_TIMESTAMP;
                RETURN NEW;
            END;
            $$ LANGUAGE plpgsql;
            
            DROP TRIGGER IF EXISTS update_filters_timestamp ON filters;
            
            CREATE TRIGGER update_filters_timestamp
                BEFORE UPDATE ON filters
                FOR EACH ROW
                EXECUTE FUNCTION update_updated_at();
        """;

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(createFilterTable);
            stmt.execute(createUpdateTrigger);
        }
    }

    public void saveFilter(String name, Map<String, Integer> filterValues) {
        String sql = """
            INSERT INTO filters (name, values)
            VALUES (?, ?::jsonb)
            ON CONFLICT (name)
            DO UPDATE SET values = EXCLUDED.values, updated_at = CURRENT_TIMESTAMP
        """;

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            String jsonValues = gson.toJson(filterValues);
            pstmt.setString(1, name);
            pstmt.setString(2, jsonValues);

            int rowsAffected = pstmt.executeUpdate();

            if (rowsAffected > 0) {
                showSuccessMessage("Filter saved successfully!");
            } else {
                showErrorMessage("Failed to save filter");
            }

        } catch (SQLException e) {
            handleDatabaseError("Error saving filter", e);
        }
    }

    public Map<String, Map<String, Integer>> loadFilters() {
        Map<String, Map<String, Integer>> filters = new HashMap<>();
        String sql = "SELECT name, values FROM filters ORDER BY name";

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                String name = rs.getString("name");
                String jsonValues = rs.getString("values");

                // Convert JSON string to Map
                TypeToken<Map<String, Integer>> typeToken = new TypeToken<>() {};
                Map<String, Integer> filterValues = gson.fromJson(jsonValues, typeToken.getType());

                filters.put(name, filterValues);
            }

        } catch (SQLException e) {
            handleDatabaseError("Error loading filters", e);
        }

        return filters;
    }

    public void deleteFilter(String name) {
        String sql = "DELETE FROM filters WHERE name = ?";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, name);
            int rowsAffected = pstmt.executeUpdate();

            if (rowsAffected > 0) {
                showSuccessMessage("Filter deleted successfully!");
            } else {
                showErrorMessage("Filter not found");
            }

        } catch (SQLException e) {
            handleDatabaseError("Error deleting filter", e);
        }
    }

    public List<String> getFilterNames() {
        List<String> names = new ArrayList<>();
        String sql = "SELECT name FROM filters ORDER BY name";

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                names.add(rs.getString("name"));
            }

        } catch (SQLException e) {
            handleDatabaseError("Error loading filter names", e);
        }

        return names;
    }

    private void handleDatabaseError(String message, SQLException e) {
        String errorMessage = String.format("%s: %s", message, e.getMessage());
        showErrorMessage(errorMessage);
        System.err.println(errorMessage);
        e.printStackTrace();
    }

    private void showSuccessMessage(String message) {
        SwingUtilities.invokeLater(() ->
                JOptionPane.showMessageDialog(
                        null,
                        message,
                        "Success",
                        JOptionPane.INFORMATION_MESSAGE
                )
        );
    }

    private void showErrorMessage(String message) {
        SwingUtilities.invokeLater(() ->
                JOptionPane.showMessageDialog(
                        null,
                        message,
                        "Error",
                        JOptionPane.ERROR_MESSAGE
                )
        );
    }

    // SQL scripts for database setup
    public static String getDatabaseSetupScript() {
        return """
            -- Create the database
            CREATE DATABASE filter_app;
           \s
            -- Connect to the database
            \\c filter_app
           \s
            -- Create the filters table
            CREATE TABLE filters (
                id SERIAL PRIMARY KEY,
                name VARCHAR(100) UNIQUE NOT NULL,
                values JSONB NOT NULL,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            );
           \s
            -- Create the update trigger function
            CREATE OR REPLACE FUNCTION update_updated_at()
            RETURNS TRIGGER AS $$
            BEGIN
                NEW.updated_at = CURRENT_TIMESTAMP;
                RETURN NEW;
            END;
            $$ LANGUAGE plpgsql;
           \s
            -- Create the trigger
            CREATE TRIGGER update_filters_timestamp
                BEFORE UPDATE ON filters
                FOR EACH ROW
                EXECUTE FUNCTION update_updated_at();
           \s
            -- Create index for faster searches
            CREATE INDEX idx_filters_name ON filters(name);
           \s
            -- Grant necessary permissions
            GRANT ALL PRIVILEGES ON DATABASE filter_app TO postgres;
            GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO postgres;
            GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO postgres;
       \s""";
    }
}
