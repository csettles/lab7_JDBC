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


/*-- Shell init:
export CLASSPATH=$CLASSPATH:mysql-connector-java-8.0.16.jar:.
export HP_JDBC_URL=jdbc:mysql://db.labthreesixfive.com/ssgandha?autoReconnect=true\&useSSL=false
export HP_JDBC_USER=ssgandha
export HP_JDBC_PW="WinterTwenty20_365_012926780"
*/
 
public class InnReservations {
   private static void setup() {
      try {
         Class.forName("com.mysql.cj.jdbc.Driver");
         System.out.println("MySQL JDBC Driver loaded");
      } catch (ClassNotFoundException ex) {
         System.err.println("Unable to load JDBC Driver");
         System.exit(-1);
      }
   }

   private void example() throws SQLException {
      try (Connection conn = DriverManager.getConnection(System.getenv("HP_JDBC_URL"),
                                                                 System.getenv("HP_JDBC_USER"),
                                                                 System.getenv("HP_JDBC_PW"))) {
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
                 for (i = 0; i < 180; i++){
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
   }

   public static void main(String[] arg) {
      System.out.println("Hello, World!");
      setup();

      try {
         InnReservations IR = new InnReservations();
         IR.example();
      } catch (SQLException e) {
          System.err.println("SQLException: " + e.getMessage());
      }
   }
}
