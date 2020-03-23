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
   private Connection conn; 


   public InnReservations() throws SQLException {
      try {
         Class.forName("com.mysql.cj.jdbc.Driver");
         System.out.println("MySQL JDBC Driver loaded");
      } catch (ClassNotFoundException ex) {
         System.err.println("Unable to load JDBC Driver");
         System.exit(-1);
      }

      try{ 
         conn = DriverManager.getConnection(System.getenv("HP_JDBC_URL"),
                                            System.getenv("HP_JDBC_USER"),
                                            System.getenv("HP_JDBC_PW"));
      } catch (SQLException e) {
         System.err.println("Unable to connect to database");
         System.exit(-1); 
      }
   }

/*displays a table of all rooms information: popularity, next available checkin, last known checkout, length of that stay*/
   private void roomsAndRates() throws SQLException {
      try (Connection conn = DriverManager.getConnection(System.getenv("HP_JDBC_URL"),
                                                                 System.getenv("HP_JDBC_USER"),
                                                                 System.getenv("HP_JDBC_PW"))) {

         StringBuilder sb = new StringBuilder("with partA as " +
                            "(select room, roomname, round(sum(checkout-checkin)/180,2) popularity "+
                            "from lab7_rooms rooms join lab7_reservations reservations on roomcode=room " +
                            "where checkout > date_sub(curdate(), interval 180 day) " +
                            "group by room "+
                            "order by popularity desc), " +

                            "partB as " +
                            "(select r1.room room, min(r1.checkout) nextAvailCheckin " +
                            "from lab7_reservations r1 join lab7_reservations r2 " +
                            "on r1.room=r2.room and r1.code<>r2.code " +
                            "where r1.checkout > curdate() and r2.checkout > curdate() " +
                            "and r1.checkout < r2.checkin " +
                            "group by r1.room), " +

                            "partC as " +
                            "(with mostRecents as (select room, max(checkout) co " +
                            "from lab7_rooms rooms join lab7_reservations reservations on roomcode=room " +
                            "group by room) " +

                            "select mostRecents.room, datediff(checkout,checkin) lengthStay, co mostRecentCheckout " +
                            "from lab7_reservations reservations join mostRecents " +
                            "on reservations.room=mostRecents.room and co=checkout " +
                            "order by datediff(checkout, checkin) desc " +
                            ") " +

                            "select partA.room, roomname, popularity, nextAvailCheckin, lengthStay, mostRecentCheckout " +
                            "from partC join partA on partC.room=partA.room " +
                            "join partB on partB.room=partC.room " +
                            ";");

         try (PreparedStatement pstmt = conn.prepareStatement(sb.toString())) {

             try (ResultSet rs = pstmt.executeQuery()) {
                 System.out.println("Room Info:");
                 while (rs.next()) {
                     System.out.format("%s %s ($%.2f) %s %s %d %n", rs.getString("room"), rs.getString("roomname"), rs.getDouble("popularity"), rs.getDate("nextAvailCheckin").toString(), rs.getDate("mostRecentCheckout").toString(), rs.getInt("lengthStay"));
                 }
             }
         }
      }
   }

   /*
    * Prompts the user for a reservation code and deletes the row.
    */
   private void cancelReservation() throws SQLException {
      try (Connection conn = DriverManager.getConnection(System.getenv("HP_JDBC_URL"),
                                                                 System.getenv("HP_JDBC_USER"),
                                                                 System.getenv("HP_JDBC_PW"))) {
         String deleteSql = "DELETE FROM " + RESERVATIONS_TABLE + " WHERE CODE = ?";
         String confirmSql = "SELECT * FROM " + RESERVATIONS_TABLE + " WHERE CODE = ?";

         Scanner scanner = new Scanner(System.in);
         System.out.print("Enter the reservation code you would like to cancel: ");
         Integer code = Integer.valueOf(scanner.nextLine());

         try (PreparedStatement stmt = conn.prepareStatement(confirmSql)) {
            stmt.setObject(1, code);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
               System.out.format("\tDeleting reservation for %s %s...\n", rs.getString("Firstname"), rs.getString("Lastname"));
            }
         }

         System.out.print("Are you sure? (y/n) ");
         String confirmation = scanner.nextLine().toLowerCase();
         if (!confirmation.equals("y")) {
            System.out.println("Goodbye.");
            return;
         }

         conn.setAutoCommit(false);
         try (PreparedStatement stmt = conn.prepareStatement(deleteSql)) {
            stmt.setObject(1, code);
            int rowCount = stmt.executeUpdate();
            System.out.format("Updated %d records for reservation %d\n", rowCount, code.intValue());
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
      InnReservations i = new InnReservations();
      
      Scanner scanner = new Scanner(System.in);
      System.out.println("Welcome to the reservation system");
      System.out.println("\t1. List popular rooms");
      System.out.println("\t2. Make a reservation");
      System.out.println("\t3. Alter a reservation");
      System.out.println("\t4. Cancel a reservation");
      System.out.println("\t5. View reservation details");
      System.out.println("\t6. List monthly revenue");
      System.out.println("Please enter a command: ");

      String input = scanner.nextLine(); 

      while(!input.equals("exit")) {
         
        switch(input){
          case "1" : 
             i.roomsAndRates();
             break;
          case "2":
             //fr2();
             break;
          case "3":
             //fr3();
             break;  
          case "4": 
             i.cancelReservation();
             break; 
          case "5":
             //fr5(); 
             break; 
          case "6":
             //fr6(); 
             break; 
          default:
             System.out.println("Sorry, that's not a valid command\n"); 
             break;  
        }      
      
      System.out.println("Please enter a command:"); 
      input = scanner.nextLine();
   }
 }
}
