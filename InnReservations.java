import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.DriverManager;
import java.sql.PreparedStatement;

import java.util.Map;
import java.util.Scanner;
import java.util.LinkedHashMap;
import java.time.LocalDate;
import java.util.List;
import java.util.ArrayList;

/*
-- Shell init:
export CLASSPATH=$CLASSPATH:mysql-connector-java-8.0.16.jar:.
export HP_JDBC_URL=jdbc:mysql://db.labthreesixfive.com/your_username_here?autoReconnect=true\&useSSL=false
export HP_JDBC_USER=
export HP_JDBC_PW=
 */
public class InnReservations {
   private static final String RESERVATIONS_TABLE = "shbae.lab7_reservations";
   private static final String ROOMS_TABLE = "shbae.lab7_rooms";

   private static void setup() {
      try {
         Class.forName("com.mysql.cj.jdbc.Driver");
         System.out.println("MySQL JDBC Driver loaded");
      } catch (ClassNotFoundException ex) {
         System.err.println("Unable to load JDBC Driver");
         System.exit(-1);
      }
   }

   /*
    * Prompts the user for a reservation code and deletes the row.
    */
   private void cancelReservation() throws SQLException {
      try (Connection conn = DriverManager.getConnection(System.getenv("HP_JDBC_URL"),
                                                                 System.getenv("HP_JDBC_USER"),
                                                                 System.getenv("HP_JDBC_PW"))) {
         String deleteSql = "DELETE FROM " + RESERVATIONS_TABLE + "WHERE Code = ?";
         String confirmSql = "SELECT FROM " + RESERVATIONS_TABLE + "WHERE Code = ?";

         Scanner scanner = new Scanner(System.in);
         System.out.println("Enter the reservation code you would like to cancel: ");
         String code = scanner.nextLine();

         try (Statement stmt = conn.createStatement();
               ResultSet rs = stmt.executeQuery(confirmSql);) {
            while (rs.next()) {
               System.out.format("\tDeleting reservation for %s %s...\n", rs.getString("Firstname"), rs.getString("Lastname"));
            }
         }

         System.out.print("Are you sure? (y/n)");
         String confirmation = scanner.nextLine().toLower();
         if (confirmation != "y") {
            System.out.prinln("Goodbye.");
            return;
         }

         conn.setAutoCommit(false);
         try (PreparedStatement stmt = conn.prepareStatement(deleteSql)) {
            stmt.setString(1, code);
            int rowCount = stmt.executeUpdate();
            System.out.format("Updated %d records for reservation %s\n", rowCount, code);
            conn.commit();
         } catch (SQLException e) {
            conn.rollback();
         }
      }
   }

   /*
    * Prints all rows/columns from the specified table.
    */
   private static void example(String table) throws SQLException {
      try (Connection conn = DriverManager.getConnection(System.getenv("HP_JDBC_URL"),
                                                                 System.getenv("HP_JDBC_USER"),
                                                                 System.getenv("HP_JDBC_PW"))) {
            // Step 2: Construct SQL statement
            String sql = "SELECT * FROM " + table;

            conn.setAutoCommit(false);

            // Step 4: Send SQL statement to DBMS
            try (Statement stmt = conn.createStatement();
               ResultSet rs = stmt.executeQuery(sql);) {

               ResultSetMetaData rsmd = rs.getMetaData();
               int columnsNumber = rsmd.getColumnCount();
                // Step 5: Receive results
               while (rs.next()) {
                  for (int i = 1; i <= columnsNumber; i++) {
                     if (i > 1) System.out.print(",\t");
                        String columnValue = rs.getString(i);
                     System.out.print(rsmd.getColumnName(i) + ": " + columnValue);
                  }
                  System.out.println("");
               }
               conn.commit();
            } catch (SQLException e) {
               conn.rollback();
            }
      }
   }

   public static void main(String[] arg) throws SQLException {
      System.out.println("Hello, World!");
      setup();
      example(RESERVATIONS_TABLE);
   }
}