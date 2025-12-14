package com.example;

import java.sql.*;
import java.util.Arrays;
import java.util.Scanner;

public class Main {

    static void main(String[] args) {
        if (isDevMode(args)) {
            DevDatabaseInitializer.start();
        }
        new Main().run();
    }

    public void run() {
        // Resolve DB settings with precedence: System properties -> Environment variables
        String jdbcUrl = resolveConfig("APP_JDBC_URL", "APP_JDBC_URL");
        String dbUser = resolveConfig("APP_DB_USER", "APP_DB_USER");
        String dbPass = resolveConfig("APP_DB_PASS", "APP_DB_PASS");


        if (jdbcUrl == null || dbUser == null || dbPass == null) {
            throw new IllegalStateException(
                    "Missing DB configuration. Provide APP_JDBC_URL, APP_DB_USER, APP_DB_PASS " +
                            "as system properties (-Dkey=value) or environment variables.");
        }

        Connection connection = null;
        try {
            connection = DriverManager.getConnection(jdbcUrl, dbUser, dbPass);
            Scanner sc = new Scanner(System.in);
            System.out.println("Username: ");
            String username = sc.nextLine();

            System.out.println("Password: ");
            String password = sc.nextLine();


            String sql = "SELECT * FROM account WHERE name = ? AND password = ?";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {

                statement.setString(1, username);
                statement.setString(2, password);

                try (ResultSet resultSet = statement.executeQuery()) {
                    if (!resultSet.next()) {
                        System.out.println("Invalid username or password.");
                        return;
                    }
                }
            }

            //nytt resultset varje gång för varje test
            //behöver inte nytt objekt varje gång men = statement.executeQuery behövs
            //kan återanvända connection och statement
            //måste ställa ny fråga varje gång också

            System.out.println("Login Sucessful");

            int choice = -1;
            while (choice != 0) {
            System.out.println("\nThese are the menu options: ");
            System.out.println("1) List moon missions (prints spacecraft names from `moon_mission`)");
            System.out.println("2) Get a moon mission by mission_id (prints details for that mission)");
            System.out.println("3) Count missions for a given year (prompts: year; prints the number of missions launched that year)");
            System.out.println("4) Create an account (prompts: first name, last name, ssn, password; prints confirmation)");
            System.out.println("5) Update an account password (prompts: user_id, new password; prints confirmation).");
            System.out.println("6) Delete an account (prompts: user_id; prints confirmation).");
            System.out.println("0) Exit.");

            System.out.print("Enter choice: ");
            choice = sc.nextInt();

            switch (choice) {
            case 1 -> listSpacecrafts(connection);
            case 2 -> moonMission(connection, sc);
            case 3 -> countMissionByYear(connection, sc);
            case 4 -> createAccount(connection, sc);
            case 5 -> updateAccount(connection, sc);
            case 6 -> deleteAccount(connection, sc);
            case 0 -> System.out.println("Exiting...");
            default -> System.out.println("Invalid choice, try again");

            }
       }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        }


    public void listSpacecrafts(Connection connection) throws SQLException {
        String sql = "SELECT spacecraft FROM moon_mission";
        try (PreparedStatement stmt = connection.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                System.out.println(rs.getString("spacecraft"));
            }
        }
    }

    public void moonMission(Connection connection, Scanner sc) throws SQLException {
        System.out.println("Enter Mission ID: ");
        int id = sc.nextInt();
        System.out.println("Mission ID: " + id);

        String sql = "SELECT * FROM moon_mission WHERE mission_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
             stmt.setInt(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    System.out.println("Spacecraft: " + rs.getString("spacecraft"));
                    System.out.println("Launch Date: " + rs.getDate("launch_date"));
                    System.out.println("Carrier: " + rs.getString("carrier_rocket"));
                    System.out.println("Operator: " + rs.getString("Operator"));
                    System.out.println("Mission Type: " + rs.getString("mission_type"));
                    System.out.println("Outcome: " + rs.getString("outcome"));
                } else {
                    System.out.println("No mission found with that ID.");
                }
            }
        }
    }

    public void countMissionByYear(Connection connection, Scanner sc) throws SQLException {
        System.out.println("Enter year: ");
        int year = sc.nextInt();
        System.out.println("Year: " + year);

        String sql = "SELECT count(*) FROM moon_mission WHERE YEAR(launch_date) = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, year);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    int count = rs.getInt(1);

                    if (count == 0) {
                        System.out.println("No missions launched that year.");
                    } else {
                        System.out.println("Number of missions that launched that year: " + count);
                    }
                }
            }
        }
    }

    public void createAccount(Connection connection, Scanner sc) throws SQLException {
        System.out.println("Enter First name: ");
        String first_name = sc.next();
        System.out.println("Enter Last name: ");
        String last_name = sc.next();
        System.out.println("Enter ssn: ");
        String ssn = sc.next();
        System.out.println("Enter password: ");
        String password = sc.next();


        String sql = "INSERT INTO account (first_name, last_name, ssn, password) VALUES (?, ?, ?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, first_name);
            stmt.setString(2, last_name);
            stmt.setString(3, ssn);
            stmt.setString(4, password);
            stmt.executeUpdate();

            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                int id = rs.getInt(1);
                System.out.println("Created an account with ID: " + id);
            }
        }
    }

    public void updateAccount(Connection connection, Scanner sc) throws SQLException {
        System.out.println("Enter user_id: ");
        String user_id = sc.next();
        System.out.println("Enter new password: ");
        String password = sc.next();


        String sql = "UPDATE account SET password = ? WHERE user_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, password);
            stmt.setString(2, user_id);

            int rowsUpdated = stmt.executeUpdate();

            if (rowsUpdated > 0) {
                System.out.println("Password updated.");
            } else {
                System.out.println("No account found with that user_id.");
            }
        }
    }

    public void deleteAccount(Connection connection, Scanner sc) throws SQLException {
        System.out.println("Enter user_id: ");
        String user_id = sc.next();

        String sql = "DELETE FROM account WHERE user_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, user_id);

            int rowsDeleted = stmt.executeUpdate();

            if (rowsDeleted > 0) {
                System.out.println("Account Deleted.");
            } else {
                System.out.println("No account found with that user_id.");
            }
        }

    }

    /**
     * Determines if the application is running in development mode based on system properties,
     * environment variables, or command-line arguments.
     *
     * @param args an array of command-line arguments
     * @return {@code true} if the application is in development mode; {@code false} otherwise
     */
    private static boolean isDevMode(String[] args) {
        if (Boolean.getBoolean("devMode"))  //Add VM option -DdevMode=true
            return true;
        if ("true".equalsIgnoreCase(System.getenv("DEV_MODE")))  //Environment variable DEV_MODE=true
            return true;
        return Arrays.asList(args).contains("--dev"); //Argument --dev
    }

    /**
     * Reads configuration with precedence: Java system property first, then environment variable.
     * Returns trimmed value or null if neither source provides a non-empty value.
     */
    private static String resolveConfig(String propertyKey, String envKey) {
        String v = System.getProperty(propertyKey);
        if (v == null || v.trim().isEmpty()) {
            v = System.getenv(envKey);
        }
        return (v == null || v.trim().isEmpty()) ? null : v.trim();
    }
}


//780112-1350
//MB=V4cbAqPz4vqmQ
//nytt resultset varje gång för varje test
//behöver inte nytt objekt varje gång men = statement.executeQuery behövs
//kan återanvända connection och statement
//måste ställa ny fråga varje gång också