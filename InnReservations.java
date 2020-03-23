import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException; 

import java.util.Map;
import java.util.Scanner;
import java.util.LinkedHashMap;
import java.time.LocalDate;
import java.util.List;
import java.util.ArrayList;
import java.util.Random; 
import java.text.SimpleDateFormat; 
import java.text.ParseException;
import java.util.Date; 
import java.util.Calendar; 

import java.time.ZoneId; 

/*
-- Shell init:
export CLASSPATH=$CLASSPATH:mysql-connector-java-8.0.16.jar:.
export HP_JDBC_URL=jdbc:mysql://db.labthreesixfive.com/your_username_here?; autoReconnect=true\&useSSL=false
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


   class ResevResult {
      public String roomCode; 
      public String roomName;
      public String bedType; 
      public String startDate; 
      public String endDate; 
      public ResevResult(String roomCode, String roomName, String bedType, String startDate, String endDate){
         this.roomCode = roomCode;
         this.roomName = roomName;
         this.bedType = bedType;
         this.startDate = startDate;
         this.endDate = endDate; 
         }
      public String toString(){
         return roomCode + " " + roomName + " " + bedType + " " + startDate + " " + endDate; 
      }
     } 

   public void makeReservation() throws SQLException {

      try (Connection conn = DriverManager.getConnection(System.getenv("HP_JDBC_URL"),
                                                                 System.getenv("HP_JDBC_USER"),
                                                                 System.getenv("HP_JDBC_PW"))) {
         conn.setAutoCommit(false); 
         Scanner scanner = new Scanner(System.in); 
         System.out.println("First name :");
         String firstName = scanner.nextLine();  
         System.out.println("Last name :");
         String lastName = scanner.nextLine();  
         System.out.println("Room Code ('Any' to indicate no preference):");
         String roomCode = scanner.nextLine();  
         System.out.println("Desired bed type ('Any' to indicate no preference):");
         String bedType = scanner.nextLine();
         System.out.println("Begin date of stay YYYY-MM-DD:");
         String startDate = scanner.nextLine();  
         System.out.println("End date of stay YYYY-MM-DD:");
         String endDate = scanner.nextLine();  
         System.out.println("Number of children:");
         int numChildren = scanner.nextInt();  
         System.out.println("Number of adults:");
         int numAdults = scanner.nextInt(); 

         //TODO: check if there are any rooms big with enough occupancy
         String checkOcc = "SELECT macOcc FROM " + ROOMS_TABLE; 
         try(PreparedStatement s = conn.prepareStatement(checkOcc)) {
         } catch(SQLException e){
         }        

         String queryString = "SELECT * FROM " + ROOMS_TABLE + " WHERE RoomCode NOT IN " +
            "( SELECT RoomCode FROM " + RESERVATIONS_TABLE + " JOIN " + ROOMS_TABLE + " ON Room = RoomCode " +
            "WHERE (CheckIn >= '" + startDate + "' and CheckIn <= '" + endDate + "' ) " + 
            "or (CheckOut >= '" + startDate + "' and CheckOut <= '" + endDate + "' )) AND maxOcc >= " + (numAdults+numChildren); 
         if(!roomCode.toLowerCase().equals("any")){
            queryString += " AND RoomCode = '" + roomCode.toUpperCase() + "' "; 
         }
         if(!bedType.toLowerCase().equals("any")){
            queryString += " AND bedType = '" + bedType.substring(0, 1).toUpperCase() + bedType.substring(1).toLowerCase() + "' " ; 
         }

         ArrayList<ResevResult> results = new ArrayList<>(); 

         try (PreparedStatement stmt = conn.prepareStatement(queryString)) {
            //stmt.setObject(1, code);
            ResultSet rs = stmt.executeQuery();
            rs.setFetchSize(10); 
            int ctr = 1; 
            while (rs.next()) {
               System.out.format(ctr + ") %s", rs.getString("RoomName"));
               System.out.println(); 
               ResevResult thisR = new ResevResult(rs.getString("RoomCode"), rs.getString("RoomName"), rs.getString("bedType"), startDate, endDate); 
               results.add(thisR); 
               ctr++; 
            }
            if(results.isEmpty()){
               System.out.println("No results for that request. Here is the same room with similar dates.");
               results = makeReccomendation(roomCode,startDate,endDate, conn);  
               ctr = 1;  
               for(ResevResult r : results) {
                  System.out.println(ctr + ") " + r.roomCode + " start date : " + r.startDate + " end date : " + r.endDate); 
                  ctr++; 
               }
            } 
         } catch(SQLException e) {
            System.out.println(e); 
            System.out.println("Rollback here !" ); 
            conn.rollback();
            System.out.println("Error making reservation"); 
            return; 
         } 
         System.out.println("Select an option : "); 
         int selection = scanner.nextInt(); 
         //read the remaining newline
         scanner.nextLine(); 
         ResevResult thisResult = results.get(selection-1);
         double rate = 150.00; 
         // double roomRate = calcWeekendRate(startDate, endDate); 
         // add resev dates and fix formatting
         System.out.format("%s %s %s %s %s Adults:%d Children:%d", firstName, lastName, thisResult.roomCode, thisResult.roomName, thisResult.bedType, numAdults, numChildren); 
         System.out.println(); 
         System.out.println("Enter CONFIRM or CANCEL:" );
         String option = scanner.nextLine(); 
         
         Random rand = new Random(); 
         int resvCode = rand.nextInt(100000);
         queryString = "INSERT INTO " + RESERVATIONS_TABLE + " (CODE,Room,CheckIn,Checkout,Rate,LastName,FirstName,Adults,Kids) " + 
            "VALUES( " + resvCode + ", '" + thisResult.roomCode + "', '" + startDate + "', '" + endDate + "', " + rate + ", '" + lastName + "', '" + firstName + "', " + numAdults +
            ", " + numChildren + ") ;"; 
         if(option.equals("CONFIRM")) {
            try(PreparedStatement stmt = conn.prepareStatement(queryString)){
            stmt.executeUpdate();
            conn.commit(); 
            }catch(SQLException e){
               System.out.println(e); 
               conn.rollback();
               System.out.println("Error making reservation"); 
               return; 
            }
         } else {
            System.out.println("Cancelling reservation"); 
         }
      }
      return; 
   } 
   
   /* 
    * TODO: Helper function for makeReservation that recommends similar reservations based on date
    */ 
   public ArrayList<ResevResult> makeReccomendation(String roomCode, String startDate, String endDate, Connection conn) {
      ArrayList<ResevResult> results = new ArrayList<>(); 
      SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd"); 
      try { 
         Date startObj = sdf.parse(startDate); 
         System.out.println(startObj); 
         Date tempStart = startObj; 
         Date endObj = sdf.parse(endDate); 
         Date tempEnd = endObj;
         int ctr = 0;  
         //shift the reservation by one day fowards and add each time it is available 
         while(results.size() < 3) { 
            tempStart = incrementDate(tempStart,1,sdf); 
            tempEnd = incrementDate(tempEnd,1,sdf);
            queryRoomForDate(roomCode,sdf.format(tempStart),sdf.format(tempEnd),conn,results);
         }
         //reset to original dates 
         tempStart = startObj;
         tempEnd = endObj;
         while(results.size() < 6) { 
            tempStart = incrementDate(tempStart,-1,sdf); 
            tempEnd = incrementDate(tempEnd,-1,sdf);
            queryRoomForDate(roomCode,sdf.format(tempStart),sdf.format(tempEnd),conn,results);
         }
      } catch (ParseException e){
         System.out.println(e); 
         System.out.println("Unable to make reccomendation"); 
         return results; 
      }
      return results; 
   } 

   //This is the only method of date incrementing i've found, but it doesn't wrap around the months, it just keeps incrementing days. 
   //TODO: 
   public Date incrementDate(Date thisDate, int delta, SimpleDateFormat sdf) {
      LocalDate ld = LocalDate.parse(sdf.format(thisDate)).plusDays(delta); 
      Date date = Date.from(ld.atStartOfDay(ZoneId.systemDefault()).toInstant());   
      return date;
   }


   /*   
    * Helper function for makeReccomendation that returns query
    */ 
   public void queryRoomForDate(String roomCode, String startDate, String endDate, Connection conn, ArrayList<ResevResult> results) {

      String queryString = "SELECT * FROM " + ROOMS_TABLE + " WHERE RoomCode NOT IN " +
            "( SELECT RoomCode FROM " + RESERVATIONS_TABLE + " JOIN " + ROOMS_TABLE + " ON Room = RoomCode " +
            "WHERE (CheckIn >= '" + startDate + "' and CheckIn <= '" + endDate + "' ) " +
            "or (CheckOut >= '" + startDate + "' and CheckOut <= '" + endDate + "'))" + 
            "AND RoomCode = '" + roomCode + "'";
      
      try(PreparedStatement stmt = conn.prepareStatement(queryString)){
         //System.out.println("sucessfully added?"); 
         ResultSet rs = stmt.executeQuery();
         //should only ever be one result
         while(rs.next()){
            results.add(new ResevResult(rs.getString("RoomCode"), rs.getString("RoomName"), rs.getString("bedType"), startDate, endDate)); 
         }
      }catch(SQLException e){
         System.out.println("Error searching for new room date");       
         System.out.println(e); 
         return; 
      }
      return; 
   }     

      /*
      * TODO: Calculate weekend rate based on number of weekend days and week days 
      */ 
      public double calcWeekendRate(String startDate, String endDate) {
         return 0.0; 
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
      InnReservations i = new InnReservations();
      //i.cancelReservation();
      //i.roomsAndRates();
      i.makeReservation();      
      /*try (Connection conn = DriverManager.getConnection(System.getenv("HP_JDBC_URL"),
                                                                 System.getenv("HP_JDBC_USER"),
                                                                 System.getenv("HP_JDBC_PW"))) {
         ArrayList<ResevResult> results = new ArrayList<ResevResult>(); 
         i.makeReccomendation("CAS", "2010-01-01", "2010-01-08", conn);
         //i.queryRoomForDate("CAS", "2010-01-01", "2010-01-08", conn, results);
         for(ResevResult r : results){
            System.out.println(results.toString()); 
         }         
      }*/
   }
}
