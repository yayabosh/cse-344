package flightapp;

import java.io.*;
import java.sql.*;
import java.util.*;
import java.security.*;
import java.security.spec.*;
import javax.crypto.*;
import javax.crypto.spec.*;

/**
 * Runs queries against a back-end database
 */
public class Query {
  // DB Connection
  private Connection conn;

  // Password hashing parameter constants
  private static final int HASH_STRENGTH = 65536;
  private static final int KEY_LENGTH = 128;

  // For check dangling
  private static final String TRANCOUNT_SQL = "SELECT @@TRANCOUNT AS tran_count";
  private PreparedStatement tranCountStatement;

  /** Instance variables */
  private boolean loggedIn;                    // True if a user is currently logged in, false otherwise
  private String currentUser;                  // Logged in user's username
  private List<ComparableFlight> itineraries;  // Most recently searched-for itineraries

  /** PreparedStatements */
  // Inserts given user into Users
  private static final String CREATE_USER = "INSERT INTO Users (username, salt, hash, balance) VALUES (?, ?, ?, ?)";
  private PreparedStatement createUser;

  // Returns the top ? flights sorted by duration from the given origin to the destination
  private static final String SEARCH_ONE_HOP = "SELECT TOP(?) fid,carrier_id,flight_num,actual_time,capacity,price FROM Flights WHERE canceled = 0 AND origin_city = ? AND dest_city = ? AND day_of_month = ? ORDER BY actual_time ASC";
  private PreparedStatement searchOneHop;

  private static final String SEARCH_TWO_HOP = "SELECT TOP(?) f1.fid AS fid1, f2.fid AS fid2, f1.carrier_id AS carrier1, f2.carrier_id AS carrier2, f1.flight_num AS flightNum1, f2.flight_num AS flightNum2, f1.dest_city AS stopCity, f1.actual_time AS time1, f2.actual_time AS time2, f1.capacity AS capacity1, f2.capacity AS capacity2, f1.price AS price1, f2.price AS price2 FROM Flights f1, Flights f2 WHERE f1.canceled = 0 AND f2.canceled = 0 AND f1.origin_city = ? AND f1.dest_city = f2.origin_city AND f2.dest_city = ? AND f1.day_of_month = ? AND f2.day_of_month = ? ORDER BY f1.actual_time + f2.actual_time";
  private PreparedStatement searchTwoHop;

  private static final String GET_RESERVATIONS = "SELECT * FROM Reservations WHERE canceled = 0 AND username = ? ORDER BY rid";
  private PreparedStatement getReservations;

  private static final String GET_FLIGHT_WITH_FID = "SELECT * FROM Flights WHERE fid = ?";
  private PreparedStatement getFlightWithFID;

  private static final String GET_SEATS_TAKEN_1 = "SELECT COUNT(*) AS count FROM Reservations WHERE fid1 = ? AND canceled = 0";
  private PreparedStatement getSeatsTaken1;
  
  private static final String GET_SEATS_TAKEN_2 = "SELECT COUNT(*) AS count FROM Reservations WHERE fid2 = ? AND canceled = 0";
  private PreparedStatement getSeatsTaken2;

  private static final String GET_NEXT_RESERVATION_ID = "SELECT COUNT(*) AS count FROM Reservations";
  private PreparedStatement getNextReservationID;

  private static final String INSERT_BOOKING = "INSERT INTO Reservations (rid, paid, canceled, fid1, fid2, username) VALUES(?, ?, ?, ?, ?, ?)";
  private PreparedStatement insertBooking;

  private static final String GET_RESERVATION_WITH_RID = "SELECT * FROM Reservations WHERE canceled = 0 AND rid = ?";
  private PreparedStatement getReservationWithRID;

  private static final String GET_USER_WITH_USERNAME = "SELECT * FROM Users WHERE username = ?";
  private PreparedStatement getUserWithUsername;

  private static final String UPDATE_BALANCE = "UPDATE Users SET balance = ? WHERE username = ?";
  private PreparedStatement updateBalance;

  private static final String UPDATE_RESERVATION_PAYMENT = "UPDATE Reservations SET paid = ? WHERE rid = ?";
  private PreparedStatement updateReservationPayment;

  private static final String UPDATE_RESERVATION_CANCELED = "UPDATE Reservations SET canceled = ? WHERE rid = ?";
  private PreparedStatement updateReservationCanceled;

  private static final String CLEAR_RESERVATIONS = "DELETE FROM Reservations";
  private PreparedStatement clearReservations;

  private static final String CLEAR_USERS = "DELETE FROM Users";
  private PreparedStatement clearUsers;


  public Query() throws SQLException, IOException {
    this(null, null, null, null);
  }

  protected Query(String serverURL, String dbName, String adminName, String password)
      throws SQLException, IOException {
    conn = serverURL == null ? openConnectionFromDbConn()
        : openConnectionFromCredential(serverURL, dbName, adminName, password);

    prepareStatements();
  }

  /**
   * Return a connection by using dbconn.properties file
   *
   * @throws SQLException
   * @throws IOException
   */
  public static Connection openConnectionFromDbConn() throws SQLException, IOException {
    // Connect to the database with the provided connection configuration
    Properties configProps = new Properties();
    configProps.load(new FileInputStream("dbconn.properties"));
    String serverURL = configProps.getProperty("flightapp.server_url");
    String dbName = configProps.getProperty("flightapp.database_name");
    String adminName = configProps.getProperty("flightapp.username");
    String password = configProps.getProperty("flightapp.password");
    return openConnectionFromCredential(serverURL, dbName, adminName, password);
  }

  /**
   * Return a connection by using the provided parameter.
   *
   * @param serverURL example: example.database.widows.net
   * @param dbName    database name
   * @param adminName username to login server
   * @param password  password to login server
   *
   * @throws SQLException
   */
  protected static Connection openConnectionFromCredential(String serverURL, String dbName,
      String adminName, String password) throws SQLException {
    String connectionUrl =
        String.format("jdbc:sqlserver://%s:1433;databaseName=%s;user=%s;password=%s", serverURL,
            dbName, adminName, password);
    Connection conn = DriverManager.getConnection(connectionUrl);

    // By default, automatically commit after each statement
    conn.setAutoCommit(true);

    // By default, set the transaction isolation level to serializable
    conn.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);

    return conn;
  }

  /**
   * Get underlying connection
   */
  public Connection getConnection() {
    return conn;
  }

  /**
   * Closes the application-to-database connection
   */
  public void closeConnection() throws SQLException {
    conn.close();
  }

  /**
   * Clear the data in any custom tables created.
   * 
   * WARNING! Do not drop any tables and do not clear the Flights table.
   */
  public void clearTables() {
    try {
      // Log the user out
      this.loggedIn    = false;
      this.currentUser = null;

      // Clear Reservations table
      this.clearReservations.execute();
      // Clear Users table
      this.clearUsers.execute();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /*
   * Prepare all the SQL statements in this method.
   */
  private void prepareStatements() throws SQLException {
    this.tranCountStatement        = this.conn.prepareStatement(TRANCOUNT_SQL);
    this.createUser                = this.conn.prepareStatement(CREATE_USER);
    this.searchOneHop              = this.conn.prepareStatement(SEARCH_ONE_HOP);
    this.searchTwoHop              = this.conn.prepareStatement(SEARCH_TWO_HOP);
    this.getReservations           = this.conn.prepareStatement(GET_RESERVATIONS);
    this.getFlightWithFID          = this.conn.prepareStatement(GET_FLIGHT_WITH_FID);
    this.getSeatsTaken1            = this.conn.prepareStatement(GET_SEATS_TAKEN_1);
    this.getSeatsTaken2            = this.conn.prepareStatement(GET_SEATS_TAKEN_2);
    this.getNextReservationID      = this.conn.prepareStatement(GET_NEXT_RESERVATION_ID);
    this.insertBooking             = this.conn.prepareStatement(INSERT_BOOKING);
    this.getReservationWithRID     = this.conn.prepareStatement(GET_RESERVATION_WITH_RID);
    this.getUserWithUsername       = this.conn.prepareStatement(GET_USER_WITH_USERNAME);
    this.updateBalance             = this.conn.prepareStatement(UPDATE_BALANCE);
    this.updateReservationPayment  = this.conn.prepareStatement(UPDATE_RESERVATION_PAYMENT);
    this.updateReservationCanceled = this.conn.prepareStatement(UPDATE_RESERVATION_CANCELED);
    this.clearUsers                = this.conn.prepareStatement(CLEAR_USERS);
    this.clearReservations         = this.conn.prepareStatement(CLEAR_RESERVATIONS);
  }

  /**
   * Takes a user's username and password and attempts to log the user in.
   *
   * @param username user's username
   * @param password user's password
   *
   * @return If someone has already logged in, then return "User already logged in\n" For all other
   *         errors, return "Login failed\n". Otherwise, return "Logged in as [username]\n".
   */
  public String transaction_login(String username, String password) {
    ResultSet users = null;
    try {
      // Verify a user isn't already logged in
      if (this.loggedIn) {
        return "User already logged in\n";
      }

      // Usernames are case-insensitive
      username = username.toLowerCase();

      // Get the current user's data from Users
      this.getUserWithUsername.clearParameters();
      this.getUserWithUsername.setString(1, username);
      users = this.getUserWithUsername.executeQuery();

      // Login user if they exist in the database
      if (users.next()) {
        // Compare password hashes
        byte[] expected = users.getBytes("hash");
        byte[] actual   = this.getHash(password, users.getBytes("salt"));
        // Passwords don't match, exit
        if (!Arrays.equals(expected, actual)) return "Login failed\n";

        // Log the user in
        this.loggedIn = true;
        this.currentUser = username;

        return "Logged in as " + username + "\n";
      }

      // User was not found in database, exit
      return "Login failed\n";
    } catch (SQLException e) {
      return "Login failed\n";
    } finally {
      // Clean up resources
      if (users != null) {
        try {
          users.close();
        } catch (SQLException e) {}
      }
      checkDanglingTransaction();
    }
  }

  /**
   * Implement the create user function.
   *
   * @param username   new user's username. User names are unique the system.
   * @param password   new user's password.
   * @param initAmount initial amount to deposit into the user's account, should be >= 0 (failure
   *                   otherwise).
   *
   * @return either "Created user {@code username}\n" or "Failed to create user\n" if failed.
   */
  public String transaction_createCustomer(String username, String password, int initAmount) {
    ResultSet users = null;
    try {
      // Verify the user's initial balance is non-negative
      if (initAmount < 0) return "Failed to create user\n";

      // All usernames are case-insensitive
      username = username.toLowerCase();

      this.getUserWithUsername.clearParameters();
      this.getUserWithUsername.setString(1, username);
      users = this.getUserWithUsername.executeQuery();

      // Verify username is unique and doesn't exist in the database
      if (users.next()) return "Failed to create user\n";

      this.createUser.clearParameters();
      // Set username
      this.createUser.setString(1, username);
      // Set salt
      byte[] salt = this.getSalt();
      this.createUser.setBytes(2, salt);
      // Set hash
      byte[] hash = this.getHash(password, salt);
      this.createUser.setBytes(3, hash);
      // Set balance
      this.createUser.setInt(4, initAmount);

      // Insert user into database
      this.createUser.execute();

      return "Created user " + username + "\n";
    } catch (SQLException e) {
      return "Failed to create user\n";
    } finally {
      // Clean up resources
      if (users != null) {
        try {
          users.close();
        } catch (SQLException e) {}
      }
      checkDanglingTransaction();
    }
  }

  // Helper method which returns a random cryptographic salt.
  private byte[] getSalt() {
    // Generate a random cryptographic salt
    SecureRandom random = new SecureRandom();
    byte[] salt = new byte[16];
    random.nextBytes(salt);
    return salt;
  }

  // Helper method which returns a hash given a password and salt.
  private byte[] getHash(String password, byte[] salt) {
    // Specify the hash parameters
    KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, HASH_STRENGTH, KEY_LENGTH);

    // Generate the hash
    SecretKeyFactory factory = null;
    byte[] hash = null; 
    try {
      factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
      hash = factory.generateSecret(spec).getEncoded();
      return hash;
    } catch (NoSuchAlgorithmException | InvalidKeySpecException ex) {
      throw new IllegalStateException();
    }
  }

  // Private class for sorting itineraries based on time and flight IDs. 
  private class ComparableFlight implements Comparable<ComparableFlight> {
    // Flight ID of first flight in itinerary -- cannot be null
    private int fid1;
    // Flight ID of second flight in itinerary -- can be null if direct flight
    private Integer fid2;
    // Total time of the itinerary
    private int time;
    // Description of the itinerary implemented in Flight.toString()
    private String description;

    // Constructs a new ComparableFlight
    public ComparableFlight(int fid1, Integer fid2, int time, String description) {
      this.fid1 = fid1;
      this.fid2 = fid2;
      this.time = time;
      this.description = description;
    }

    // Returns the number of flights in this itinerary.
    public int numFlights() {
      return this.isDirect() ? 1 : 2;
    }

    // Returns true if the itinerary only contains a direct flight.
    public boolean isDirect() {
      return this.fid2 == null;
    }

    // Compares two itineraries based on their total time, breaking ties on flight IDs.
    @Override
    public int compareTo(ComparableFlight o) {
      int cmp = this.time - o.time;
      if (cmp != 0) return cmp;

      cmp = this.fid1 - o.fid1;
      if (cmp != 0) return cmp;

      if (!this.isDirect() && !o.isDirect()) return this.fid2 - o.fid2;
      return 0;
    }
  }

  /**
   * Implement the search function.
   *
   * Searches for flights from the given origin city to the given destination city, on the given day
   * of the month. If {@code directFlight} is true, it only searches for direct flights, otherwise
   * is searches for direct flights and flights with two "hops." Only searches for up to the number
   * of itineraries given by {@code numberOfItineraries}.
   *
   * The results are sorted based on total flight time.
   *
   * @param originCity
   * @param destinationCity
   * @param directFlight        if true, then only search for direct flights, otherwise include
   *                            indirect flights as well
   * @param dayOfMonth
   * @param numberOfItineraries number of itineraries to return
   *
   * @return If no itineraries were found, return "No flights match your selection\n". If an error
   *         occurs, then return "Failed to search\n".
   *
   *         Otherwise, the sorted itineraries printed in the following format:
   *
   *         Itinerary [itinerary number]: [number of flights] flight(s), [total flight time]
   *         minutes\n [first flight in itinerary]\n ... [last flight in itinerary]\n
   *
   *         Each flight should be printed using the same format as in the {@code Flight} class.
   *         Itinerary numbers in each search should always start from 0 and increase by 1.
   *
   * @see Flight#toString()
   */
  public String transaction_search(String originCity, String destinationCity, boolean directFlight,
      int dayOfMonth, int numberOfItineraries) {
    ResultSet oneHopResults = null;
    ResultSet twoHopResults = null;
    try {
      StringBuffer sb = new StringBuffer();
      // Clear last searched-for itineraries
      this.itineraries = new ArrayList<>();

      /* Search for numberOfItineraries direct flights */
      this.searchOneHop.clearParameters();
      // Set max number of itineraries to be returned
      this.searchOneHop.setInt(1, numberOfItineraries);
      // Set origin city
      this.searchOneHop.setString(2, originCity);
      // Set destination city
      this.searchOneHop.setString(3, destinationCity);
      // Set day of month
      this.searchOneHop.setInt(4, dayOfMonth);

      oneHopResults = this.searchOneHop.executeQuery();

      // Add up to numberOfItineraries direct itineraries to final result (oneHopResults could contain less)
      while (oneHopResults.next()) {
        int fid        = oneHopResults.getInt("fid");
        int time       = oneHopResults.getInt("actual_time");
        String carrier = oneHopResults.getString("carrier_id");
        String num     = oneHopResults.getString("flight_num");
        int capacity   = oneHopResults.getInt("capacity");
        int price      = oneHopResults.getInt("price");

        // Add itinerary to final result
        Flight f = new Flight(fid, dayOfMonth, carrier, num, originCity, destinationCity, time, capacity, price);
        this.itineraries.add(new ComparableFlight(fid, null, time, f.toString()));

        // Update number of itineraries added so far
        numberOfItineraries--;
      }

      // Continue adding indirect flights to final result
      if (!directFlight && numberOfItineraries > 0) {
        this.searchTwoHop.clearParameters();

        // Set max number of itineraries to be returned
        this.searchTwoHop.setInt(1, numberOfItineraries);
        // Set origin city
        this.searchTwoHop.setString(2, originCity);
        // Set destination city
        this.searchTwoHop.setString(3, destinationCity);
        // Set day of month
        this.searchTwoHop.setInt(4, dayOfMonth);
        this.searchTwoHop.setInt(5, dayOfMonth);

        twoHopResults = this.searchTwoHop.executeQuery();

        // Add numberOfItineraries indirect itineraries to final result
        while (twoHopResults.next()) {
          int fid1          = twoHopResults.getInt("fid1");
          int fid2          = twoHopResults.getInt("fid2");
          String carrier1   = twoHopResults.getString("carrier1");
          String carrier2   = twoHopResults.getString("carrier2");
          String flightNum1 = twoHopResults.getString("flightNum1");
          String flightNum2 = twoHopResults.getString("flightNum1");
          String stopCity   = twoHopResults.getString("stopCity");
          int time1         = twoHopResults.getInt("time1");
          int time2         = twoHopResults.getInt("time2");
          int capacity1     = twoHopResults.getInt("capacity1");
          int capacity2     = twoHopResults.getInt("capacity2");
          int price1        = twoHopResults.getInt("price1");
          int price2        = twoHopResults.getInt("price2");

          Flight f1 = new Flight(fid1, dayOfMonth, carrier1, flightNum1, originCity, stopCity, time1, capacity1, price1);
          Flight f2 = new Flight(fid2, dayOfMonth, carrier2, flightNum2, stopCity, destinationCity, time2, capacity2, price2);
          this.itineraries.add(new ComparableFlight(fid1, fid2, time1 + time2, f1.toString() + f2.toString()));
        }
      }

      if (itineraries.isEmpty()) return "No flights match your selection\n";

      // Sort itineraries
      Collections.sort(this.itineraries);

      // Add itineraries to result string
      for (int i = 0; i < this.itineraries.size(); i++) {
        ComparableFlight f = this.itineraries.get(i);
        String header = String.format("Itinerary %d: %d flight(s), %d minutes\n", i, f.numFlights(), f.time);
        sb.append(header).append(f.description);
      }

      return sb.toString();
    } catch (SQLException e) {
      e.printStackTrace();
      return "Failed to search\n";
    } finally {
      // Clean up resources
      if (oneHopResults != null) {
        try {
          oneHopResults.close();
        } catch (SQLException e) {}
      }
      if (twoHopResults != null) {
        try {
          twoHopResults.close();
        } catch (SQLException e) {}
      }
      checkDanglingTransaction();
    }
  }

  /**
   * Implements the book itinerary function.
   *
   * @param itineraryId ID of the itinerary to book. This must be one that is returned by search in
   *                    the current session.
   *
   * @return If the user is not logged in, then return "Cannot book reservations, not logged in\n".
   *         If the user is trying to book an itinerary with an invalid ID or without having done a
   *         search, then return "No such itinerary {@code itineraryId}\n". If the user already has
   *         a reservation on the same day as the one that they are trying to book now, then return
   *         "You cannot book two flights in the same day\n". For all other errors, return "Booking
   *         failed\n".
   *
   *         And if booking succeeded, return "Booked flight(s), reservation ID: [reservationId]\n"
   *         where reservationId is a unique number in the reservation system that starts from 1 and
   *         increments by 1 each time a successful reservation is made by any user in the system.
   */
  public String transaction_book(int itineraryId) {
    ResultSet flight1             = null;
    ResultSet flight2             = null;
    ResultSet reservations        = null;
    ResultSet previousReservation = null;
    ResultSet seatsTaken1         = null;
    ResultSet seatsTaken2         = null;
    ResultSet reservationCount    = null;
    try {
      // Execute all SQL statements in this method in one transaction
      this.conn.setAutoCommit(false);

      // Verify user is logged in
      if (!this.loggedIn) {
        return "Cannot book reservations, not logged in\n";
      } else if (this.itineraries == null || this.itineraries.isEmpty() ||
                 itineraryId < 0 || itineraryId >= this.itineraries.size()) {
        // Verify last searched-for itineraries exist and returned results and
        // that the given itinerary ID is valid
        return "No such itinerary " + itineraryId + "\n";
      }

      // Get itinerary to be booked
      ComparableFlight f = this.itineraries.get(itineraryId);

      // Get first flight in itinerary to be booked
      this.getFlightWithFID.setInt(1, f.fid1);
      flight1 = this.getFlightWithFID.executeQuery();
      flight1.next();
      // Get first flight's day of month
      int dayOfMonth = flight1.getInt("day_of_month");
      // Get first flight's total capacity
      int totalCapacity1 = flight1.getInt("capacity");
      // Clean up resource
      flight1.close();

      // Get reservations for user who is booking the itinerary
      this.getReservations.setString(1, this.currentUser);
      reservations = this.getReservations.executeQuery();

      // Iterate through each previous user reservation to verify that the user is not booking the
      // new reservation on a previously booked day
      while (reservations.next()) {
        // Get first flight for previously booked itinerary
        this.getFlightWithFID.clearParameters();
        this.getFlightWithFID.setInt(1, reservations.getInt("fid1"));
        previousReservation = this.getFlightWithFID.executeQuery();
        previousReservation.next();

        // Make sure the day of the new flight has not been previously booked
        if (previousReservation.getInt("day_of_month") == dayOfMonth) {
          this.conn.rollback();
          return "You cannot book two flights in the same day\n";
        }
      }

      // Get number of seats taken on first flight
      this.getSeatsTaken1.setInt(1, f.fid1);
      seatsTaken1 = this.getSeatsTaken1.executeQuery();
      seatsTaken1.next();

      // Calculate seats left in first flight
      int seatsLeft1 = totalCapacity1 - seatsTaken1.getInt("count");

      // Verify there is capacity for user on first flight
      if (seatsLeft1 < 1) {
        this.conn.rollback();
        return "Booking failed\n";
      }

      // Do the same for the second flight, if it exists
      if (f.fid2 != null) {
        // Get second flight in the itinerary to be booked
        this.getFlightWithFID.clearParameters();
        this.getFlightWithFID.setInt(1, f.fid2);
        flight2 = this.getFlightWithFID.executeQuery();
        flight2.next();

        // Get second flight's total capacity
        int totalCapacity2 = flight2.getInt("capacity");

        // Get number of seats taken on second flight
        this.getSeatsTaken2.setInt(1, f.fid2);
        seatsTaken2 = this.getSeatsTaken2.executeQuery();
        seatsTaken2.next();

        // Calculate seats left in second flight
        int seatsLeft2 = totalCapacity2 - seatsTaken2.getInt("count");

        // Verify there is capacity for user on second flight
        if (seatsLeft2 < 1) {
          this.conn.rollback();
          return "Booking failed\n";
        }
      }

      // Get reservation ID for current booking (count of Reservations + 1)
      reservationCount = this.getNextReservationID.executeQuery();
      reservationCount.next();
      int reservationID = reservationCount.getInt("count") + 1;

      // Insert booking for reservation with reservation ID
      this.insertBooking.clearParameters();
      // Set reservation ID
      this.insertBooking.setInt(1, reservationID);
      // Set payment status to not paid
      this.insertBooking.setInt(2, 0);
      // Set cancellation status to not
      this.insertBooking.setInt(3, 0);
      // Set flight 1 FID
      this.insertBooking.setInt(4, f.fid1);
      // Set flight 2 FID
      if (f.fid2 != null) {
        this.insertBooking.setInt(5, f.fid2);
      } else {
        this.insertBooking.setNull(5, java.sql.Types.INTEGER);
      }
      // Set username
      this.insertBooking.setString(6, this.currentUser);

      // Insert booking
      this.insertBooking.execute();

      // Save changes in current transaction
      this.conn.commit();

      return "Booked flight(s), reservation ID: " + reservationID + "\n";
    } catch (SQLException e) {
      // Undo changes in current transaction
      try {
        this.conn.rollback();
      } catch (SQLException e1) {}

      // Try again
      if (isDeadLock(e)) return this.transaction_book(itineraryId);

      return "Booking failed\n";
    } finally {
      // Clean up resources
      if (flight1 != null) {
        try {
          flight1.close();
        } catch (SQLException e) {}
      }
      if (flight2 != null) {
        try {
          flight2.close();
        } catch (SQLException e) {}
      }
      if (reservations != null) {
        try {
          reservations.close();
        } catch (SQLException e) {}
      }
      if (previousReservation != null) {
        try {
          previousReservation.close();
        } catch (SQLException e) {}
      }
      if (seatsTaken1 != null) {
        try {
          seatsTaken1.close();
        } catch (SQLException e) {}
      }
      if (seatsTaken2 != null) {
        try {
          seatsTaken2.close();
        } catch (SQLException e) {}
      }
      if (reservationCount != null) {
        try {
          reservationCount.close();
        } catch (SQLException e) {}
      }

      // Future SQL statements will execute as individual transactions
      try {
        this.conn.setAutoCommit(true);
      } catch (SQLException e) {}

      checkDanglingTransaction();
    }
  }

  /**
   * Implements the pay function.
   *
   * @param reservationId the reservation to pay for.
   *
   * @return If no user has logged in, then return "Cannot pay, not logged in\n" If the reservation
   *         is not found / not under the logged in user's name, then return "Cannot find unpaid
   *         reservation [reservationId] under user: [username]\n" If the user does not have enough
   *         money in their account, then return "User has only [balance] in account but itinerary
   *         costs [cost]\n" For all other errors, return "Failed to pay for reservation
   *         [reservationId]\n"
   *
   *         If successful, return "Paid reservation: [reservationId] remaining balance:
   *         [balance]\n" where [balance] is the remaining balance in the user's account.
   */
  public String transaction_pay(int reservationId) {
    ResultSet result  = null;
    ResultSet user    = null;
    ResultSet flight1 = null;
    ResultSet flight2 = null;
    try {
      // Execute all SQL statements in this method in one transaction
      this.conn.setAutoCommit(false);

      // Verify user is logged in
      if (!this.loggedIn) return "Cannot pay, not logged in\n";

      // Get reservation with given ID from Reservations
      this.getReservationWithRID.setInt(1, reservationId);
      result = this.getReservationWithRID.executeQuery();

      // Verify there exists a reservation with given ID and user is the one who booked it
      if (!result.next() || !result.getString("username").equals(this.currentUser)) {
        this.conn.rollback();
        return "Cannot find unpaid reservation " + reservationId + " under user: " + this.currentUser + "\n";
      }

      // Verify the reservation is currently unpaid
      if (result.getInt("paid") == 1) {
        this.conn.rollback();
        return "Cannot find unpaid reservation " + reservationId + " under user: " + this.currentUser + "\n";
      }

      // Get user's current balance
      this.getUserWithUsername.clearParameters();
      this.getUserWithUsername.setString(1, this.currentUser);
      user = this.getUserWithUsername.executeQuery();
      user.next();
      int currentBalance = user.getInt("balance");

      // Get price of first flight
      this.getFlightWithFID.clearParameters();
      this.getFlightWithFID.setInt(1, result.getInt("fid1"));
      flight1 = this.getFlightWithFID.executeQuery();
      flight1.next();
      int price = flight1.getInt("price");

      // Add price of second flight
      this.getFlightWithFID.clearParameters();
      this.getFlightWithFID.setInt(1, result.getInt("fid2"));
      // Verify second flight isn't null
      if (result.getInt("fid2") != 0) {
        flight2 = this.getFlightWithFID.executeQuery();
        flight2.next();
        price += flight2.getInt("price");
      }

      // User is broke, exit
      if (currentBalance < price) {
        this.conn.rollback();
        return "User has only " + currentBalance + " in account but itinerary costs " + price + "\n";
      }

      // Update user's current balance
      int newBalance = currentBalance - price;
      this.updateBalance.clearParameters();
      this.updateBalance.setInt(1, newBalance);
      this.updateBalance.setString(2, this.currentUser);
      this.updateBalance.execute();

      // Update reservation payment status to paid
      this.updateReservationPayment.clearParameters();
      this.updateReservationPayment.setInt(1, 1);
      this.updateReservationPayment.setInt(2, reservationId);
      this.updateReservationPayment.execute();

      // Save changes in current transaction
      this.conn.commit();

      return "Paid reservation: " + reservationId + " remaining balance: " + newBalance + "\n";
    } catch (SQLException e) {
      // Undo changes in current transaction
      try {
        this.conn.rollback();
      } catch (SQLException e1) {}

      // Try again
      if (isDeadLock(e)) return this.transaction_pay(reservationId);

      return "Failed to pay for reservation " + reservationId + "\n";
    } finally {
      // Clean up resources
      if (result != null) {
        try {
          result.close();
        } catch (SQLException e) {}
      }
      if (user != null) {
        try {
          user.close();
        } catch (SQLException e) {}
      }
      if (flight1 != null) {
        try {
          flight1.close();
        } catch (SQLException e) {}
      }
      if (flight2 != null) {
        try {
          flight2.close();
        } catch (SQLException e) {}
      }

      // Future SQL statements will execute as individual transactions
      try {
        this.conn.setAutoCommit(true);
      } catch (SQLException e) {}

      checkDanglingTransaction();
    }
  }

  /**
   * Implements the reservations function.
   *
   * @return If no user has logged in, then return "Cannot view reservations, not logged in\n" If
   *         the user has no reservations, then return "No reservations found\n" For all other
   *         errors, return "Failed to retrieve reservations\n"
   *
   *         Otherwise return the reservations in the following format:
   *
   *         Reservation [reservation ID] paid: [true or false]:\n [flight 1 under the
   *         reservation]\n [flight 2 under the reservation]\n Reservation [reservation ID] paid:
   *         [true or false]:\n [flight 1 under the reservation]\n [flight 2 under the
   *         reservation]\n ...
   *
   *         Each flight should be printed using the same format as in the {@code Flight} class.
   *
   * @see Flight#toString()
   */
  public String transaction_reservations() {
    ResultSet result = null;
    ResultSet f1     = null;
    ResultSet f2     = null;
    try {
      // Execute all SQL statements in this method in one transaction
      this.conn.setAutoCommit(false);

      // Verify user is logged in
      if (!this.loggedIn) return "Cannot view reservations, not logged in\n";

      // Get all of current user's reservations
      this.getReservations.clearParameters();
      this.getReservations.setString(1, this.currentUser);
      result = this.getReservations.executeQuery();

      // Stores all of user's reservations in String format specified in method comment
      List<String> reservations = new ArrayList<>();
      while (result.next()) {
        // Get reservation details
        int rid      = result.getInt("rid");
        boolean paid = result.getInt("paid") == 1;

        // Append reservation header string
        StringBuffer reservation = new StringBuffer(
          String.format("Reservation %d paid: %b:\n", rid, paid)
        );

        // Get flight 1 details
        int fid1 = result.getInt("fid1");
        this.getFlightWithFID.clearParameters();
        this.getFlightWithFID.setInt(1, fid1);
        f1 = this.getFlightWithFID.executeQuery();
        f1.next();

        int fid        = f1.getInt("fid");
        int dayOfMonth = f1.getInt("day_of_month");
        int time       = f1.getInt("actual_time");
        String carrier = f1.getString("carrier_id");
        String num     = f1.getString("flight_num");
        String origin  = f1.getString("origin_city");
        String dest    = f1.getString("dest_city");
        int capacity   = f1.getInt("capacity");
        int price      = f1.getInt("price");

        // Construct itinerary string for flight 1
        Flight flight1 = new Flight(fid, dayOfMonth, carrier, num, origin, dest, time, capacity, price);

        // Append flight 1 itinerary string
        reservation.append(flight1.toString());

        int fid2 = result.getInt("fid2");
        if (fid2 != 0) {
          // Get flight 2 details
          this.getFlightWithFID.clearParameters();
          this.getFlightWithFID.setInt(1, fid2);
          f2 = this.getFlightWithFID.executeQuery();
          f2.next();

          fid        = f1.getInt("fid");
          dayOfMonth = f1.getInt("day_of_month");
          time       = f1.getInt("actual_time");
          carrier    = f1.getString("carrier_id");
          num        = f1.getString("flight_num");
          origin     = f1.getString("origin_city");
          dest       = f1.getString("dest_city");
          capacity   = f1.getInt("capacity");
          price      = f1.getInt("price");

          // Construct itinerary string for flight 2
          Flight flight2 = new Flight(fid, dayOfMonth, carrier, num, origin, dest, time, capacity, price);

          // Append flight 2 itinerary string
          reservation.append(flight2.toString());
        }
        // Append reservation string
        reservations.add(reservation.toString());
      }
      
      // User has no reservations, exit
      if (reservations.isEmpty()) {
        this.conn.rollback();
        return "No reservations found\n";
      }

      // Construct final result string
      StringBuffer finalResult = new StringBuffer();
      for (String reservation : reservations) {
        finalResult.append(reservation);
      }

      // Save changes in current transaction
      this.conn.commit();

      return finalResult.toString();
    } catch (SQLException e) {
      // Undo changes in current transaction
      try {
        this.conn.rollback();
      } catch (SQLException e1) {}

      // Try again
      if (isDeadLock(e)) return this.transaction_reservations();

      return "Failed to retrieve reservations\n";
    } finally {
      // Clean up resources
      if (result != null) {
        try {
          result.close();
        } catch (SQLException e) {}
      }
      if (f1 != null) {
        try {
          f1.close();
        } catch (SQLException e) {}
      }
      if (f2 != null) {
        try {
          f2.close();
        } catch (SQLException e) {}
      }

      // Future SQL statements will execute as individual transactions
      try {
        this.conn.setAutoCommit(true);
      } catch (SQLException e) {}

      checkDanglingTransaction();
    }
  }

  /**
   * Implements the cancel operation.
   *
   * @param reservationId the reservation ID to cancel
   *
   * @return If no user has logged in, then return "Cannot cancel reservations, not logged in\n" For
   *         all other errors, return "Failed to cancel reservation [reservationId]\n"
   *
   *         If successful, return "Canceled reservation [reservationId]\n"
   *
   *         Even though a reservation has been canceled, its ID should not be reused by the system.
   */
  public String transaction_cancel(int reservationId) {
    ResultSet reservation = null;
    ResultSet flight1     = null;
    ResultSet flight2     = null;
    ResultSet user        = null;
    try {
      // Execute all SQL statements in this method in one transaction
      this.conn.setAutoCommit(false);

      // Verify user is logged in
      if (!this.loggedIn) return "Cannot cancel reservations, not logged in\n";

      // Get reservation with given ID from Reservations
      this.getReservationWithRID.clearParameters();
      this.getReservationWithRID.setInt(1, reservationId);
      reservation = this.getReservationWithRID.executeQuery();

      // Verify there exists a reservation with given ID and user is the one who booked it.
      // This also makes sure the reservation ID is not for an already cancelled reservation,
      // since the query filters canceled reservations.
      if (!reservation.next() || !reservation.getString("username").equals(this.currentUser)) {
        this.conn.rollback();
        return "Failed to cancel reservation " + reservationId + "\n";
      }

      // Refund the user if they already paid for the reservation
      if (reservation.getInt("paid") == 1) {
        // Get price of first flight
        this.getFlightWithFID.clearParameters();
        this.getFlightWithFID.setInt(1, reservation.getInt("fid1"));
        flight1 = this.getFlightWithFID.executeQuery();
        flight1.next();
        int price = flight1.getInt("price");

        // Add price of second flight (if it exists)
        this.getFlightWithFID.clearParameters();
        this.getFlightWithFID.setInt(1, reservation.getInt("fid2"));
        // Verify second flight isn't null
        if (reservation.getInt("fid2") != 0) {
          flight2 = this.getFlightWithFID.executeQuery();
          flight2.next();
          price += flight2.getInt("price");
        }

        // Get user's current balance
        this.getUserWithUsername.clearParameters();
        this.getUserWithUsername.setString(1, this.currentUser);
        user = this.getUserWithUsername.executeQuery();
        user.next();

        // Calculate refunded balance for user
        int newBalance = user.getInt("balance") + price;

        // Update user's balance
        this.updateBalance.clearParameters();
        this.updateBalance.setInt(1, newBalance);
        this.updateBalance.setString(2, this.currentUser);
        this.updateBalance.execute();
      }

      this.updateReservationCanceled.clearParameters();
      // Set canceled to true
      this.updateReservationCanceled.setInt(1, 1);
      this.updateReservationCanceled.setInt(2, reservationId);
      // Cancel reservation
      this.updateReservationCanceled.execute();

      // Save changes in current transaction
      this.conn.commit();

      return "Canceled reservation " + reservationId + "\n";
    } catch (SQLException e) {
      // Undo changes in current transaction
      try {
        this.conn.rollback();
      } catch (SQLException e1) {}

      // Try again
      if (isDeadLock(e)) return this.transaction_pay(reservationId);

      return "Failed to cancel reservation " + reservationId + "\n";
    } finally {
      // Clean up resources
      if (reservation != null) {
        try {
          reservation.close();
        } catch (SQLException e) {}
      }
      if (flight1 != null) {
        try {
          flight1.close();
        } catch (SQLException e) {}
      }
      if (flight2 != null) {
        try {
          flight2.close();
        } catch (SQLException e) {}
      }
      if (user != null) {
        try {
          user.close();
        } catch (SQLException e) {}
      }

      // Future SQL statements will execute as individual transactions
      try {
        this.conn.setAutoCommit(true);
      } catch (SQLException e) {}

      checkDanglingTransaction();
    }
  }

  /**
   * Throw IllegalStateException if transaction not completely complete, rollback.
   * 
   */
  private void checkDanglingTransaction() {
    try {
      try (ResultSet rs = tranCountStatement.executeQuery()) {
        rs.next();
        int count = rs.getInt("tran_count");
        if (count > 0) {
          throw new IllegalStateException(
              "Transaction not fully commit/rollback. Number of transaction in process: " + count);
        }
      } finally {
        conn.setAutoCommit(true);
      }
    } catch (SQLException e) {
      throw new IllegalStateException("Database error", e);
    }
  }

  private static boolean isDeadLock(SQLException ex) {
    return ex.getErrorCode() == 1205;
  }

  /**
   * A class to store flight information.
   */
  class Flight {
    public int fid;
    public int dayOfMonth;
    public String carrierId;
    public String flightNum;
    public String originCity;
    public String destCity;
    public int time;
    public int capacity;
    public int price;

    public Flight(int fid, int dayOfMonth, String carrierId, String flightNum, String originCity, String destCity, int time, int capacity, int price) {
      this.fid = fid;
      this.dayOfMonth = dayOfMonth;
      this.carrierId = carrierId;
      this.flightNum = flightNum;
      this.originCity = originCity;
      this.destCity = destCity;
      this.time = time;
      this.capacity = capacity;
      this.price = price;
    }

    @Override
    public String toString() {
      return "ID: " + fid + " Day: " + dayOfMonth + " Carrier: " + carrierId + " Number: "
          + flightNum + " Origin: " + originCity + " Dest: " + destCity + " Duration: " + time
          + " Capacity: " + capacity + " Price: " + price + "\n";
    }
  }
}
