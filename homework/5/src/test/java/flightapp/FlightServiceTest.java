package flightapp;

import org.junit.*;
import org.junit.runner.*;
import org.junit.runners.*;

import java.lang.reflect.InvocationTargetException;
import java.nio.file.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.*;
import java.sql.*;

import static org.junit.Assert.assertTrue;

/**
 * Autograder for the transaction assignment
 */
@RunWith(Parameterized.class)
public class FlightServiceTest {
  /**
   * Maximum number of concurrent users we will be testing
   */
  private static final int MAX_USERS = 5;
  /**
   * Max time in seconds to wait for a response for a user
   */
  private static final int RESPONSE_TIME = 60;
  /**
   * Thread pool used to run different users
   */
  private static ExecutorService pool;

  /**
   * Denotes a comment
   */
  static final String COMMENTS = "#";
  /**
   * Denotes information mode change
   */
  static final String DELIMITER = "*";
  /**
   * Denotes alternate result
   */
  static final String SEPARATOR = "|";

  private static String serverURL = null;
  private static String dbName = null;
  private static String adminName = null;
  private static String password = null;

  public static void initCredential() {
    String credentialServerPort = System.getProperty("credentialServer");
    if (credentialServerPort != null) {
      String[] tokens = credentialServerPort.split(":");
      try {
        System.out.println(String.format("Trying to connec to '%s' to receive database credential......", credentialServerPort));
        Class clazz = Class.forName("flightapp.distributed.Client");
        clazz.getMethod("getDBCredential", String.class, int.class).invoke(null, tokens[0], Integer.parseInt(tokens[1]));
      } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
        e.printStackTrace();
      } catch (IndexOutOfBoundsException | NumberFormatException e) {
        System.err.println("credentialServer must have format 'host:port'");
      }
    }
    String credential = System.getProperty("credential");
    if (credential != null) {
      String[] token = credential.split(";");
      if (token.length == 4) {
        serverURL = token[0].trim();
        dbName = token[1].trim();
        adminName = token[2].trim();
        password = token[3];
      } else {
        System.out.println("Credential need to be 'serverURL;dbName;username;password'");
        System.out.println("Using dbconn.properties for test.");
      }
    }
  }

  /**
   * Models a single user. Callable from a thread.
   */
  static class User implements Callable<String> {
    private Query q;
    private List<String> cmds; // commands that this user will execute
    private List<String> results; // the expected results from those commands

    public User(List<String> cmds, List<String> results) throws IOException, SQLException {
      this.q = new Query(serverURL, dbName, adminName, password);
      this.cmds = cmds;
      this.results = results;
    }

    public List<String> results() {
      return results;
    }

    @Override
    public String call() {
      StringBuffer sb = new StringBuffer();
      for (String cmd : cmds) {
        sb.append(FlightService.execute(q, cmd));
      }

      return sb.toString();
    }

    public void shutdown() throws Exception {
      this.q.closeConnection();
    }
  }

  /**
   * Parse the input test case. Format expected is
   *
   * @param filename test case's path and file name
   * @return new User objects with commands to run and expected results
   * @throws Exception
   */
  static List<User> parse(String filename) throws IOException, SQLException {
    List<User> users = new ArrayList<>();
    List<String> cmds = new ArrayList<>();
    List<String> results = new ArrayList<>();
    String r = "";
    boolean isCmd = true;
    BufferedReader reader = new BufferedReader(new FileReader(filename));
    String l;
    int lineNumber = 0;
    while ((l = reader.readLine()) != null) {
      lineNumber++;

      // Skip comment lines
      if (l.startsWith(COMMENTS)) {
        continue;

        // Switch between recording commands and recording results
      } else if (l.startsWith(DELIMITER)) {
        if (isCmd) {
          isCmd = false;
        } else {
          // Result recordings finished for a user so user is fully specified
          results.add(r);
          users.add(new User(cmds, results));
          cmds = new ArrayList<>();
          results = new ArrayList<>();
          r = "";
          isCmd = true;
        }

        // Record an alternate outcome result
      } else if (l.startsWith(SEPARATOR)) {
        if (isCmd) {
          reader.close();
          throw new IllegalArgumentException(
                  "ERROR: input file is malformatted on line: " + lineNumber);
        } else {
          results.add(r);
          r = "";
        }

        // Build command list or result string
      } else {
        // Ignore trailing comments
        l = l.split(COMMENTS, 2)[0];
        // Add new command or build current result
        if (isCmd) {
          cmds.add(l);
        } else {
          r = r + l + "\n";
        }
      }
    }
    reader.close();

    // Everything should be parsed by now and put into user objects
    if (cmds.size() > 0 || r.length() > 0 || results.size() > 0) {
      throw new IllegalArgumentException(String.format(
              "ERROR: input file is malformatted, extra information found #commands=%s, len(result)=%s, #results=%s",
              cmds.size(), r.length(), results.size()));
    }

    // check that all users have the same number of possible scenarios
    int n = users.get(0).results().size();
    for (int i = 1; i < users.size(); ++i) {
      int m = users.get(i).results().size();
      if (m != n) {
        throw new IllegalArgumentException(String.format(
                "ERROR: input file is malformatted, user %s should have %s possible results rather than %s",
                i, n, m));
      }
    }

    return users;
  }

  /**
   * Creates the thread pool to execute test cases with multiple users.
   */
  @BeforeClass
  public static void setup() {
    System.out.println("running setup");
    pool = Executors.newFixedThreadPool(MAX_USERS);
    initCredential();
    try {
      Connection conn = serverURL == null ? Query.openConnectionFromDbConn()
              : Query.openConnectionFromCredential(serverURL, dbName, adminName, password);
      TestUtils.dropTables(conn);
      TestUtils.runCreateTables(conn);
      TestUtils.checkTable(conn);
      conn.close();
    } catch (Exception e) {
      System.err.println("Failed to drop tables and run createTables.sql");
      e.printStackTrace();
      System.exit(1);
    }
  }

  /**
   * A file that will be parsed as a test case scenario
   */
  protected String file;

  /**
   * Initialize a test case with a file name
   */
  public FlightServiceTest(String file) {
    this.file = file;
  }

  /**
   * Gets test case scenario files from the specified folder.
   */
  @Parameterized.Parameters
  public static Collection<String> files() throws IOException {
    String pathString = System.getProperty("test.cases");
    return Arrays.stream(pathString.split(":", -1)).map(Paths::get).flatMap(path -> {
      try {
        if (Files.isDirectory(path)) {
          try (Stream<Path> paths = Files.walk(path, 5, FileVisitOption.FOLLOW_LINKS)) {
            return paths.filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().endsWith(".txt")).map(p -> {
                      try {
                        return p.toFile().getCanonicalPath().toString();
                      } catch (IOException e) {
                        return null;
                      }
                    }).filter(p -> p != null).collect(Collectors.toList()).stream();
          }
        } else if (Files.isRegularFile(path)) {
          return Stream.of(path.toFile().getCanonicalPath().toString());
        } else {
          System.err.println(path + " does not exists.");
        }
      } catch (Exception e) {
        return Stream.empty();
      }
      return Stream.empty();
    }).collect(Collectors.toCollection(LinkedHashSet::new));
  }


  @Before
  public void clearDB() throws SQLException, IOException {
    Query query =
            serverURL == null ? new Query() : new Query(serverURL, dbName, adminName, password);
    query.clearTables();
    query.closeConnection();
  }

  /**
   * Runs the test case scenario
   */
  @Test
  public void runTest() throws Exception {
    System.out.println("running test scenario: " + this.file);

    // Loads the scenario and initializes users
    List<User> users = parse(this.file);
    List<Future<String>> futures = new ArrayList<>();
    for (User user : users) {
      futures.add(pool.submit(user));
    }

    try {
      // Waits for an output for each user
      List<String> outputs = new ArrayList<>();
      long waitTime = RESPONSE_TIME * futures.size();
      long endTime = System.currentTimeMillis() + waitTime * 1000;
      for (Future<String> f : futures) {
        try {
          outputs.add(f.get(waitTime, TimeUnit.SECONDS));
        } catch (TimeoutException e) {
          System.out.println("Timed out!");
        } finally {
          waitTime = (endTime - System.currentTimeMillis()) / 1000;
          waitTime = waitTime <= 0 ? 1 : waitTime;
        }
      }

      // For each possible outcome, check if each user matches the respective output
      // for the given outcome
      boolean passed = false;
      Map<Integer, List<String>> outcomes = new HashMap<Integer, List<String>>();
      int n = users.get(0).results().size(); // number of possible outcomes
      for (int i = 0; i < n; ++i) {
        boolean isSame = true;
        for (int j = 0; j < users.size(); ++j) {
          isSame = isSame && outputs.get(j).equals(users.get(j).results().get(i));
          if (!outcomes.containsKey(i)) {
            outcomes.put(i, new ArrayList<String>());
          }
          outcomes.get(i).add(users.get(j).results().get(i));
        }
        passed = passed || isSame;
      }

      // Print the result and debugging info if applicable under the assertion
      System.out.println(passed ? "passed" : "failed");
      String outcomesFormatted = "";
      if (!passed) {
        for (Map.Entry<Integer, List<String>> outcome : outcomes.entrySet()) {
          outcomesFormatted += "===== Outcome " + outcome.getKey() + " =====\n";
          outcomesFormatted += formatOutput(outcome.getValue()) + "\n";
        }
      }
      assertTrue(
              String.format("Failed: actual outputs for %s were: \n%s\n\nPossible outcomes were: \n%s",
                      this.file, formatOutput(outputs), outcomesFormatted),
              passed);
    } catch (Exception e) {
      System.out.println("failed");
      e.printStackTrace();
      throw e;
    } finally {
      // Cleanup
      for (User u : users) {
        u.shutdown();
      }
    }
  }

  public static String formatOutput(List<String> output) {
    StringBuilder sb = new StringBuilder();
    int i = 0;
    for (String s : output) {
      sb.append("---Terminal " + i + " begin\n");
      sb.append(s);
      sb.append("---Terminal " + i + " end\n");
      ++i;
    }

    return sb.toString();
  }
}
