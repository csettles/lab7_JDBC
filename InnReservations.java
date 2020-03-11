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
         Class.forName("com.mysql.jdbc.Driver");
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
      System.out.println("Hello, World!");
   }
}