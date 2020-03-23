# Inn Reservations
CSC 365: CLI for INN reservations

Nicole Schwartz  
Ty Farrris  
Lauren Hibbs  
Steven Gandham  
Caitlin Settles  
Sarah Bae  

# How to run
```
$ export CLASSPATH=$CLASSPATH:mysql-connector-java-8.0.16.jar:.
$ export HP_JDBC_URL=jdbc:mysql://db.labthreesixfive.com/your_username_here?autoReconnect=true\&useSSL=false
$ export HP_JDBC_USER=<username>
$ export HP_JDBC_PW=<pwd>
$ cd lab7_JDBC
$ javac InnReservations.java
$ java InnReservations
```

# Database
The data is stored in lab7_reservations and lab7_rooms tables under the user shbae.

# Known Deficiencies
The suggestion feature when making a reservation runs a bit slow. Depending on the user input, it can hang for a few seconds while computing. 

# Work Log

Nicole Schwartz - FR3  
Ty Farrris - FR5  
Lauren Hibbs - FR2  
Steven Gandham - FR6  
Sarah Bae - FR1  
Caitlin Settles - FR4 and combining/testing final code  

