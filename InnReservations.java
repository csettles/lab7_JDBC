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
import java.time.format.DateTimeFormatter;
/*
//-- Shell init:
export CLASSPATH=$CLASSPATH:mysql-connector-java-8.0.16.jar:.
export HP_JDBC_URL=jdbc:mysql://db.labthreesixfive.com/your_username_here?autoReconnect=true\&useSSL=false
export HP_JDBC_USER=
export HP_JDBC_PW=
 */
public class InnReservations {
	private static final String RESERVATIONS = "shbae.lab7_reservations";
   private static final String ROOMS = "shbae.lab7_rooms";

	public static void main(String[] arg) {
      System.out.println("Hello, World!");
      try{
         InnReservations inn = new InnReservations();
      	inn.change_reservation();
   	}catch (SQLException e){
   	   System.err.println("SQLException: " + e.getMessage());
    	}
   }

   private void setup() {
      try {
         Class.forName("com.mysql.jdbc.Driver");
         System.out.println("MySQL JDBC Driver loaded");
      } catch (ClassNotFoundException ex) {
         System.err.println("Unable to load JDBC Driver");
         System.exit(-1);
      }
   }

   private int validCheckin(LocalDate start,String code) throws SQLException{
   	try (Connection conn = DriverManager.getConnection(System.getenv("HP_JDBC_URL"),
                                                         System.getenv("HP_JDBC_USER"),
                                                         System.getenv("HP_JDBC_PW"))) {
    		
   		StringBuilder sb = new StringBuilder("SELECT res.checkout, res.Room from " + RESERVATIONS + " res where res.Code = ?");
   		LocalDate end;
   		String room;
   		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("YYYY-MM-DD");
   		try(PreparedStatement pstmt = conn.prepareStatement(sb.toString())){
   			pstmt.setString(1, code);
   			try (ResultSet rs = pstmt.executeQuery()){	
   				rs.next();
					end = java.sql.Date.valueOf(rs.getString("CheckOut")).toLocalDate();
					room =rs.getString("Room");
   			}
   		}
   		if(checkDate(start, end, code, room) ==1)
   			return 1;
   		else
   			return 0;
      	}
   }
   private int checkDate(LocalDate start, LocalDate end, String code, String room) throws SQLException{
      StringBuilder sb2 = new StringBuilder("SELECT res.checkout, res.checkin from " + RESERVATIONS + " res where res.Room = ? and res.code != ?");
      LocalDate checkout;
      LocalDate checkin;
      try (Connection conn = DriverManager.getConnection(System.getenv("HP_JDBC_URL"),
                                                         System.getenv("HP_JDBC_USER"),
                                                         System.getenv("HP_JDBC_PW"))) {
      	try(PreparedStatement pstmt = conn.prepareStatement(sb2.toString())){
   			pstmt.setString(1, room);
   			pstmt.setString(2, code);
   			try (ResultSet rs = pstmt.executeQuery()){
   				while(rs.next()){
		   			checkout = java.sql.Date.valueOf(rs.getString("CheckOut")).toLocalDate();
   					checkin = java.sql.Date.valueOf(rs.getString("CheckIn")).toLocalDate();
   			   	if(!((start.isAfter(checkout) | start.isEqual(checkout))|(end.isBefore(checkin) | end.isEqual(checkin)))){
   			   		return 0;
   			  		} 	
   				}
   			}
   		}
			return 1;
		}
   }
   private int validCheckOut(LocalDate end, String code) throws SQLException{
   	try (Connection conn = DriverManager.getConnection(System.getenv("HP_JDBC_URL"),
                                                         System.getenv("HP_JDBC_USER"),
                                                         System.getenv("HP_JDBC_PW"))) {
    		
   		StringBuilder sb = new StringBuilder("SELECT res.CheckIn, res.Room from " + RESERVATIONS + " res where res.Code = ?");
   		LocalDate start;
   		String room;
   		try(PreparedStatement pstmt = conn.prepareStatement(sb.toString())){
   			pstmt.setString(1, code);
   			try (ResultSet rs = pstmt.executeQuery()){
   				rs.next();
   				start = java.sql.Date.valueOf(rs.getString("CheckIn")).toLocalDate();
					room = rs.getString("Room");
   			}
   		}
         if(checkDate(start, end, code, room)==1)
         	return 1;
         else
         	return 0;
      }
   }
   private void change_reservation() throws SQLException{
		//Step 1: Establish connection to RDBMS
		try (Connection conn = DriverManager.getConnection(System.getenv("HP_JDBC_URL"),
                                                         System.getenv("HP_JDBC_USER"),
                                                         System.getenv("HP_JDBC_PW"))) {
    		//Step 2: Construct SQL statement
  		   List<Object> params = new ArrayList<Object>();
  			System.out.print("Reservation Code: ");
  			Scanner scanner = new Scanner(System.in);
  			String code = scanner.nextLine();
         StringBuilder sb = new StringBuilder("update " + RESERVATIONS + " res set");

         System.out.print("New first name for reservation (or 'no change'): ");
         String firstname = scanner.nextLine();
			if(!"no change".equalsIgnoreCase(firstname)){
				sb.append(" res.FirstName = ? ,");
				params.add(firstname);
			}

         System.out.print("New last name for reservation (or 'no change'): ");
         String lastname = scanner.nextLine();
			if(!"no change".equalsIgnoreCase(lastname)){
				sb.append(" res.LastName = ? ,");
				params.add(lastname);
			}
         System.out.print("New check-in date (YYYY-MM-DD) for reservation (or 'no change'): ");
         String checkin = String.valueOf(scanner.nextLine());
			if(!"no change".equalsIgnoreCase(checkin)){
				if(validCheckin(LocalDate.parse(checkin), code) ==1){
					sb.append(" res.CheckIn = ? ,");
					params.add(LocalDate.parse(checkin));
				}else{
					System.out.println("Conflict with reservation check-in date");
				}
			}
   
         System.out.print("New check-out date (YYYY-MM-DD) for reservation (or 'no change'): ");
         String checkout = String.valueOf(scanner.nextLine());
			if(!"no change".equalsIgnoreCase(checkout)){
				if(validCheckOut(LocalDate.parse(checkout), code) ==1){
					sb.append(" res.CheckOut = ? ,");
					params.add(LocalDate.parse(checkout));
				}else{
					System.out.println("Conflict with reservation check-out date");
				}
			}

         System.out.print("New number of children for reservation (or 'no change'): ");
         String children = String.valueOf(scanner.nextLine());
			if(!"no change".equalsIgnoreCase(children)){
				sb.append(" res.Kids = ? ,");
				params.add(Integer.valueOf(children));
			}

         System.out.print("New number of adults for reservation (or 'no change'): ");
         String adults = String.valueOf(scanner.nextLine());
			if(!"no change".equalsIgnoreCase(adults)){
				sb.append(" res.Adults = ? ,");
				params.add(Integer.valueOf(adults));
			}
	      sb.delete(sb.length()-1, sb.length());
	      sb.append(" where res.code = ?;");
	      params.add(code);
	   	//Step 3:Start Transaction
			conn.setAutoCommit(false);
	      try (PreparedStatement pstmt = conn.prepareStatement(sb.toString())) {
            if(params.size() >= 2){
            	int i = 1;
            	for (Object p : params) {
              		pstmt.setObject(i++, p);
            	}
		   		//Step 4: Send Update to DBMS
					int rowCount = pstmt.executeUpdate();
					//Step 5: Handle results
					System.out.format("Updated %d records for reservation code %s\n", rowCount, code);
					//Step 6: Commit or rollback transaction
				}
				conn.commit();
			}catch(SQLException e){
				System.out.println("Error");
				conn.rollback();
			}
		}
   }
}
