import java.sql.ResultSet;
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
import java.util.Arrays;

/*
-- Shell init:
export CLASSPATH=$CLASSPATH:mysql-connector-java-8.0.16.jar:.
export HP_JDBC_URL=jdbc:mysql://db.labthreesixfive.com/your_username_here?autoReconnect=true\&useSSL=false
export HP_JDBC_USER=
export HP_JDBC_PW=
 */
public class InnReservations {
   private void setup() {
      try {
         Class.forName("com.mysql.cj.jdbc.Driver");
         System.out.println("MySQL JDBC Driver loaded");
      } catch (ClassNotFoundException ex) {
         System.err.println("Unable to load JDBC Driver");
         System.exit(-1);
      }
   }

   private void fr5(String[] arg) throws SQLException {
      //Step 1: Establish connection to RBDMS
      try (Connection conn = DriverManager.getConnection(System.getenv("HP_JDBC_URL"), 
                                                         System.getenv("HP_JDBC_USER"),
                                                         System.getenv("HP_JDBC_PW"))) {
        Scanner scanner = new Scanner(System.in);
        System.out.print("Number of filters: ");
        int numFilters = Integer.valueOf(scanner.next());
        
        //parse input
        System.out.println("Filter options: FirstName, LastName, Date, RoomCode, ReservCode\n");
        ArrayList<String> filters = new ArrayList<String>();
        for(int i = 0; i < numFilters; i++)
        {
            System.out.printf("Filter %d by: ", i + 1);
            String filterType = scanner.next();
            filters.add(filterType);
            System.out.printf("Enter %s: ", filterType);
            filters.add(scanner.next());
            System.out.println();
        }

        //Step 2: Construct SQL statement;
        String query = "SELECT reservations.Code, reservations.Room, rooms.RoomName, ";
        query += "reservations.CheckIn, reservations.CheckOut, reservations.LastName, ";
        query += "reservations.FirstName, reservations.Adults, reservations.Kids ";
        query += "FROM shbae.lab7_reservations as reservations ";
        query += "JOIN shbae.lab7_rooms as rooms ON reservations.Room = rooms.RoomCode ";
        query += "WHERE reservations.Room = ?";

        System.out.println(query);
        //Step 3: Start transaction
        conn.setAutoCommit(false);


        try (PreparedStatement pstmt = conn.prepareStatement(query)) {

          //Step 4: Send SQL statement to DBMS
          pstmt.setString(1, filters.get(1));
          ResultSet rs = pstmt.executeQuery();

          //Step 5: Handle results
          String output = "";
          System.out.println("\nCode, Room, RoomName, CheckIn, CheckOut, LastName, FirstName, Adults, Kids");
          while (rs.next()) {

            System.out.printf("%d, %s, %s, %s, %s, %s, %s, %d, %d\n", rs.getInt("reservations.Code"), 
                                                                    rs.getString("reservations.Room"), 
                                                                    rs.getString("rooms.RoomName"),
                                                                    rs.getDate("reservations.checkIn").toString(), 
                                                                    rs.getDate("reservations.checkOut").toString(), 
                                                                    rs.getString("reservations.LastName"),
                                                                    rs.getString("reservations.FirstName"), 
                                                                    rs.getInt("reservations.Adults"),
                                                                    rs.getInt("reservations.Kids"));
          }

          //Step 6: Commit or rollback transaction
          conn.commit();
        }
        catch (SQLException e) {
          System.out.println(e);
          conn.rollback();
        }

        //Step 7: Close connection (handled implcitly by try-with-resources syntax)
        
        //System.out.println(Arrays.toString(filters.toArray()));
      }
   }

   private void example() throws SQLException {
      try (Connection conn = DriverManager.getConnection(System.getenv("HP_JDBC_URL"),
                                                                 System.getenv("HP_JDBC_USER"),
                                                                 System.getenv("HP_JDBC_PW"))) {
         Scanner scanner = new Scanner(System.in);
         System.out.print("Find pastries with price <=: ");
         Double price = Double.valueOf(scanner.nextLine());
         System.out.print("Filter by flavor (or 'Any'): ");
         String flavor = scanner.nextLine();

         List<Object> params = new ArrayList<Object>();
         params.add(price);
         StringBuilder sb = new StringBuilder("SELECT * FROM hp_goods WHERE price <= ?");
         if (!"any".equalsIgnoreCase(flavor)) {
             sb.append(" AND Flavor = ?");
             params.add(flavor);
         }
         
         try (PreparedStatement pstmt = conn.prepareStatement(sb.toString())) {
             int i = 1;
             for (Object p : params) {
                 pstmt.setObject(i++, p);
             }

             try (ResultSet rs = pstmt.executeQuery()) {
                 System.out.println("Matching Pastries:");
                 int matchCount = 0;
                 while (rs.next()) {
                     System.out.format("%s %s ($%.2f) %n", rs.getString("Flavor"), rs.getString("Food"), rs.getDouble("price"));
                     matchCount++;
                 }
                 System.out.format("----------------------%nFound %d match%s %n", matchCount, matchCount == 1 ? "" : "es");
             }
         }
      }
   }

   public static void main(String[] arg) {
      InnReservations test = new InnReservations();
      test.setup();
      
      try{
        test.fr5(arg);
      }
      catch (SQLException ex){
         System.err.println("SQLException for FR5");
         System.exit(-1);
      }

      System.out.println("\nDone\n");
      
   }
}
