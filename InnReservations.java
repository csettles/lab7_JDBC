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
import java.util.List;
import java.util.ArrayList;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

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
			System.err.println("Unable to connect to database: " + e.getMessage());
			System.exit(-1); 
		}
	}

	/*displays a table of all rooms information: popularity, next available checkin, last known checkout, length of that stay*/
	private void roomsAndRates() throws SQLException {
		StringBuilder sb = new StringBuilder("with partA as " +
			"(select room, roomname, round(sum(checkout-checkin)/180,2) popularity "+
			"from " + ROOMS_TABLE + " rooms join "+ RESERVATIONS_TABLE + " reservations on roomcode=room " +
			"where checkout > date_sub(curdate(), interval 180 day) " +
			"group by room "+
			"order by popularity desc), " +

			"partB as " +
			"(select r1.room room, min(r1.checkout) nextAvailCheckin " +
			"from " + RESERVATIONS_TABLE + " r1 join " + RESERVATIONS_TABLE + " r2 " +
			"on r1.room=r2.room and r1.code<>r2.code " +
			"where r1.checkout > curdate() and r2.checkout > curdate() " +
			"and r1.checkout < r2.checkin " +
			"group by r1.room), " +

			"partC as " +
			"(with mostRecents as (select room, max(checkout) co " +
			"from " + ROOMS_TABLE + " rooms join " + RESERVATIONS_TABLE + " reservations on roomcode=room " +
			"group by room) " +

			"select mostRecents.room, datediff(checkout,checkin) lengthStay, co mostRecentCheckout " +
			"from " + RESERVATIONS_TABLE + " reservations join mostRecents " +
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

	private int validCheckin(LocalDate start,String code) throws SQLException{
		StringBuilder sb = new StringBuilder("SELECT res.checkout, res.Room from " + RESERVATIONS_TABLE + " res where res.Code = ?");
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

	private int checkDate(LocalDate start, LocalDate end, String code, String room) throws SQLException{
		StringBuilder sb2 = new StringBuilder("SELECT res.checkout, res.checkin from " + RESERVATIONS_TABLE + " res where res.Room = ? and res.code != ?");
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
		StringBuilder sb = new StringBuilder("SELECT res.CheckIn, res.Room from " + RESERVATIONS_TABLE + " res where res.Code = ?");
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

	private void change_reservation() throws SQLException{
		List<Object> params = new ArrayList<Object>();
		System.out.print("Reservation Code: ");
		Scanner scanner = new Scanner(System.in);
		String code = scanner.nextLine();
		StringBuilder sb = new StringBuilder("update " + RESERVATIONS_TABLE + " res set");

		System.out.print("New first name for reservation (or 'no change'): ");
		String firstname = scanner.nextLine();
		if(!"no change".equalsIgnoreCase(firstname)){
			sb.append(" res.FirstName = ? ,");
			params.add(firstname);

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
			} catch(SQLException e){
				System.out.println("Error");
				conn.rollback();
			}
		}
	}

	/*
	* Prompts the user for a reservation code and deletes the row.
	*/
	private void cancelReservation() throws SQLException {
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


	      //Builds the WHERE clause for the query
	private String buildQuery(String attribute, String input, int numFilters) {
		String query = "";

		if (numFilters > 0)
		{
			query += "AND ";
		}
		else
		{
			query += "WHERE ";
		}

		if (input.contains("%"))
		{
			query += "reservations." + attribute + " LIKE ?"; 
		}
		else
		{
			query += "reservations." + attribute + " = ?";
		}

		return query;
	}

	private void fr5() throws SQLException {

		String query = "SELECT reservations.Code, reservations.Room, rooms.RoomName, ";
		query += "reservations.CheckIn, reservations.CheckOut, reservations.Rate, reservations.LastName, ";
		query += "reservations.FirstName, reservations.Adults, reservations.Kids ";
		query += "FROM shbae.lab7_reservations as reservations ";
		query += "JOIN shbae.lab7_rooms as rooms ON reservations.Room = rooms.RoomCode ";

	      //parse input
		List<Object> params = new ArrayList<Object>();
		Scanner scanner = new Scanner(System.in);
		String input = "";
		int numFilters = 0;

	      //First name
		System.out.print("Enter FirstName: ");
		input = scanner.nextLine();

		if (!input.equals(""))
		{
			params.add(input);
			query += buildQuery("FirstName", input, numFilters);
			numFilters += 1;
		}

	      //Last name
		System.out.print("Enter LastName: ");
		input = scanner.nextLine();
		if (!input.equals(""))
		{
			params.add(input);
			query += buildQuery("LastName", input, numFilters);
			numFilters += 1;
		}

	      //CheckIn Date
		System.out.print("Enter CheckIn (YYYY-MM-DD): ");
		String checkIn = scanner.nextLine();

	      //CheckOut Date
		System.out.print("Enter CheckOut (YYYY-MM-DD): ");
		String checkOut = scanner.nextLine();

	      //Date range
		if (!checkIn.equals("") && !checkIn.equals("")) 
		{
			params.add(LocalDate.parse(checkIn));
			params.add(LocalDate.parse(checkIn));

			params.add(LocalDate.parse(checkOut));
			params.add(LocalDate.parse(checkOut));

			if (numFilters > 0)
			{
				query += "AND NOT((reservations.CheckIn < ? and reservations.CheckOut <= ?) ";
				query += "OR (reservations.CheckIn >= ? and reservations.CheckOut > ?))";
			}
			else
			{
				query += "WHERE NOT((reservations.CheckIn < ? and reservations.CheckOut <= ?) ";
				query += "OR (reservations.CheckIn >= ? and reservations.CheckOut > ?))";
			}

			numFilters += 2;
		}

	      //Room Code
		System.out.print("Enter Room Code: ");
		input = scanner.nextLine();
		if (!input.equals("")) 
		{  
			params.add(input);
			query += buildQuery("Room", input, numFilters);
			numFilters += 1;
		}

	      //Reservation Code
		System.out.print("Enter Reservation Code: ");
		input = scanner.nextLine();
		if (!input.equals("")) 
		{
			params.add(input);
			query += buildQuery("Code", input, numFilters);
			numFilters += 1;
		}
		System.out.println(); 

		conn.setAutoCommit(false);

		try (PreparedStatement pstmt = conn.prepareStatement(query)) 
		{
			int k = 1;
			for (Object p: params)
			{
				pstmt.setObject(k++, p);
			}

			try (ResultSet rs = pstmt.executeQuery()) 
			{
				String output = "";
				System.out.println("Code, Room, RoomName, CheckIn, CheckOut, Rate, LastName, FirstName, Adults, Kids");
				while (rs.next()) 
				{
					System.out.printf("%d, %s, %s, %s, %s, %.2f, %s, %s, %d, %d\n", rs.getInt("reservations.Code"), 
						rs.getString("reservations.Room"), 
						rs.getString("rooms.RoomName"),
						rs.getDate("reservations.checkIn").toString(), 
						rs.getDate("reservations.checkOut").toString(), 
						rs.getDouble("reservations.Rate"),
						rs.getString("reservations.LastName"),
						rs.getString("reservations.FirstName"), 
						rs.getInt("reservations.Adults"),
						rs.getInt("reservations.Kids"));
				}
				conn.commit();
			}
		}
		catch (SQLException e) 
		{
			System.out.println(e);
			conn.rollback();
		}
	}

	private void getRevenue() throws SQLException {
		StringBuilder sb = new StringBuilder();

		/* build R6 query */

		sb.append("WITH NotSameMonth AS (");
		sb.append("                        SELECT Code, Room, CheckIn, CheckOut, ");
		sb.append("                        MONTHNAME(CheckIn) AS Month1,");
		sb.append("                        MONTHNAME(CheckOut) AS Month2,");
		sb.append("                        DATEDIFF(LAST_DAY(CheckIn), CheckIn)+1 AS Month1Total,");
		sb.append("                        DATEDIFF(CheckOut, DATE_ADD(LAST_DAY(CheckIn), INTERVAL 1 DAY)) AS Month2Total, ");
		sb.append("                        Rate");
		sb.append("                        FROM shbae.lab7_reservations R");
		sb.append("                        WHERE MONTHNAME(CheckIn) <> MONTHNAME(CheckOut)");
		sb.append("                        ),");
		sb.append("SameMonth AS (");
		sb.append("                    SELECT Code, Room, CheckIn, CheckOut,");
		sb.append("                    MONTHNAME(CheckIn) AS Month1, ");
		sb.append("                    DATEDIFF(CheckOut, CheckIn) AS Month1Total, ");
		sb.append("                    Rate");
		sb.append("                    FROM shbae.lab7_reservations R");
		sb.append("                    WHERE MONTHNAME(CheckIn) = MONTHNAME(CheckOut)");
		sb.append("                    ),");
		sb.append("AllRoomsAndMonths AS (");
		sb.append("                    SELECT Room, MonthName, ROUND(SUM(MonthTotal), 0) AS MonthTotal");
		sb.append("                    FROM (SELECT Room, Month1 AS MonthName, SUM(Rate*Month1Total) AS MonthTotal");
		sb.append("                            FROM NotSameMonth");
		sb.append("                            GROUP BY Room, Month1");
		sb.append("                            ");
		sb.append("                            UNION ALL");
		sb.append("                ");
		sb.append("                            (SELECT Room, Month2 AS MonthName, SUM(Rate*Month2Total) AS MonthTotal");
		sb.append("                            FROM NotSameMonth");
		sb.append("                            GROUP BY Room, Month2) ");
		sb.append("                            ");
		sb.append("                            UNION ALL");
		sb.append("                            ");
		sb.append("                            (SELECT Room, Month1 AS MonthName, SUM(Rate*Month1Total) AS MonthTotal");
		sb.append("                            FROM SameMonth");
		sb.append("                            GROUP BY Room, Month1) ");
		sb.append("                            ) AS ALLMONTHS");
		sb.append("                    GROUP BY Room, MonthName");
		sb.append("                    )");
		sb.append("SELECT Room, ");
		sb.append("SUM(CASE ");
		sb.append("    WHEN MonthName = 'January' THEN MonthTotal ELSE 0 ");
		sb.append("END) AS January,");
		sb.append("SUM(CASE ");
		sb.append("    WHEN MonthName = 'February' THEN MonthTotal ELSE 0 ");
		sb.append("END) AS February,");
		sb.append("SUM(CASE ");
		sb.append("    WHEN MonthName = 'March' THEN MonthTotal ELSE 0 ");
		sb.append("END) AS March,");
		sb.append("SUM(CASE ");
		sb.append("    WHEN MonthName = 'April' THEN MonthTotal ELSE 0 ");
		sb.append("END) AS April,");
		sb.append("SUM(CASE ");
		sb.append("    WHEN MonthName = 'May' THEN MonthTotal ELSE 0 ");
		sb.append("END) AS May,");
		sb.append("SUM(CASE ");
		sb.append("    WHEN MonthName = 'June' THEN MonthTotal ELSE 0 ");
		sb.append("END) AS June,");
		sb.append("SUM(CASE ");
		sb.append("    WHEN MonthName = 'July' THEN MonthTotal ELSE 0 ");
		sb.append("END) AS July,");
		sb.append("SUM(CASE ");
		sb.append("    WHEN MonthName = 'August' THEN MonthTotal ELSE 0 ");
		sb.append ("END) AS August,");
		sb.append("SUM(CASE ");
		sb.append("    WHEN MonthName = 'September' THEN MonthTotal ELSE 0 ");
		sb.append("END) AS September,");
		sb.append("SUM(CASE ");
		sb.append("    WHEN MonthName = 'October' THEN MonthTotal ELSE 0 ");
		sb.append("END) AS October,");
		sb.append("SUM(CASE ");
		sb.append("    WHEN MonthName = 'November' THEN MonthTotal ELSE 0 ");
		sb.append("END) AS November,");
		sb.append("SUM(CASE ");
		sb.append("    WHEN MonthName = 'December' THEN MonthTotal ELSE 0 ");
		sb.append("END) AS December,");
		sb.append("SUM(MonthTotal) AS YearlyTotal ");
		sb.append("FROM AllRoomsAndMonths ");
		sb.append("GROUP BY Room ");
		sb.append("UNION ALL ");
		sb.append("SELECT 'Totals' AS Totals, ");
		sb.append("SUM(CASE ");
		sb.append("    WHEN MonthName = 'January' THEN MonthTotal ELSE 0 ");
		sb.append("END) AS January,");
		sb.append("SUM(CASE ");
		sb.append("    WHEN MonthName = 'February' THEN MonthTotal ELSE 0 ");
		sb.append("END) AS February,");
		sb.append("SUM(CASE ");
		sb.append("    WHEN MonthName = 'March' THEN MonthTotal ELSE 0 ");
		sb.append("END) AS March,");
		sb.append("SUM(CASE ");
		sb.append("    WHEN MonthName = 'April' THEN MonthTotal ELSE 0 ");
		sb.append("END) AS April,");
		sb.append("SUM(CASE ");
		sb.append("    WHEN MonthName = 'May' THEN MonthTotal ELSE 0 ");
		sb.append("END) AS May,");
		sb.append("SUM(CASE ");
		sb.append("    WHEN MonthName = 'June' THEN MonthTotal ELSE 0 ");
		sb.append("END) AS June,");
		sb.append("SUM(CASE ");
		sb.append("    WHEN MonthName = 'July' THEN MonthTotal ELSE 0 ");
		sb.append("END) AS July,");
		sb.append("SUM(CASE ");
		sb.append("    WHEN MonthName = 'August' THEN MonthTotal ELSE 0 ");
		sb.append("END) AS August,");
		sb.append("SUM(CASE ");
		sb.append("    WHEN MonthName = 'September' THEN MonthTotal ELSE 0 ");
		sb.append("END) AS September,");
		sb.append("SUM(CASE ");
		sb.append("    WHEN MonthName = 'October' THEN MonthTotal ELSE 0 ");
		sb.append("END) AS October,");
		sb.append("SUM(CASE ");
		sb.append("    WHEN MonthName = 'November' THEN MonthTotal ELSE 0 ");
		sb.append("END) AS November,");
		sb.append("SUM(CASE ");
		sb.append("    WHEN MonthName = 'December' THEN MonthTotal ELSE 0 ");
		sb.append("END) AS December,");
		sb.append("SUM(MonthTotal) AS YearlyTotal ");
		sb.append("FROM AllRoomsAndMonths ");
		sb.append("ORDER BY Room;");

		try (Statement stmt = conn.createStatement()){

			try (ResultSet rs = stmt.executeQuery(sb.toString())) {
				int matchCount = 0;
				System.out.format("| %10s | %10s | %10s | %10s | %10s | %10s | %10s | %10s | %10s | %10s | %10s | %10s | %10s | %10s |\n", 
					"Room", "January", "February", "March", "April", "May", "June", "July", "August",
					"September", "October", "November", "December", "YearTotal");
				int i = 0;
				for (i = 0; i < 180; i++) {
					System.out.print("-");
				}
				System.out.print("\n");

				while (rs.next()) {
					System.out.format("| %10s | %10s | %10s | %10s | %10s | %10s | %10s | %10s | %10s | %10s | %10s | %10s | %10s | %10s |\n", 
						rs.getString("Room"), rs.getInt("January"), rs.getInt("February"), 
						rs.getInt("March"), rs.getInt("April"), rs.getInt("May"), rs.getInt("June"), 
						rs.getInt("July"), rs.getInt("August"), rs.getInt("September"), 
						rs.getInt("October"), rs.getInt("November"), rs.getInt("December"), 
						rs.getInt("YearlyTotal"));
					matchCount++;
				}

				for (i = 0; i < 14; i++){
					System.out.print("+------------");
				}
				System.out.println("+\n");
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
		System.out.print("Please enter a command: ");

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
				i.change_reservation();
				break;  
				case "4": 
				i.cancelReservation();
				break; 
				case "5":
				i.fr5(); 
				break; 
				case "6":
				i.getRevenue();
				break; 
				default:
				System.out.println("Sorry, that's not a valid command\n"); 
				break;  
			}      

			System.out.print("Please enter a command: "); 
			input = scanner.nextLine();
		}
	}
}
