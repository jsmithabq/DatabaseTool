package jdbc;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.io.*;
import java.sql.*;

/**
* <p><code>DatabaseTool</code> is a simple tool for managing JDBC
* connections to a relational database.</p>
* @author Jerry Smith
* @version $Id: DatabaseTool.java 257 2006-01-18 17:48:51Z jsmith $
*/

public class DatabaseTool extends Frame implements ActionListener {
  //
  // Constants:
  //
  private static final String TITLE = "Database Tool";
  private static final String PROPERTIES_FILE = "databasetool.properties";
//  private static final String DRIVER =
//    "sun.jdbc.odbc.JdbcOdbcDriver";
//  private static final String DRIVER =
//    "com.pointbase.jdbc.jdbcDriver";
  private static final String DRIVER =
    "COM.cloudscape.core.JDBCDriver";
  private static final String PROTOCOL = "jdbc";
//  private static final String SUBPROTOCOL = "odbc";
//  private static final String SUBPROTOCOL = "pointbase";
  private static final String SUBPROTOCOL = "cloudscape";
  private static final String SUB_DATA = "datasource";
  private static final int DRIVER_WIDTH = 25;
  private static final int URL_WIDTH = 25;
  private static final int USER_WIDTH = 15;
  private static final int PW_WIDTH = 15;
  private static final int MESSAGE_WIDTH = 70;
  private static final int SQL_WIDTH = 80;
  private static final int RESULT_WIDTH = 80;
  private static final int STARTS_WITH_LEN = 40;
  private static final String LINEFEED = "\n";
  private static final String DOUBLE_LINEFEED = "\n\n";
  private static final String NULL_SYMBOL = "(null)";
  //
  // Instance variables:
  //
  private String appName;
  private Button connect, previous, next, submit, clear;
  private TextField urlField, driverField;
  private TextField userField, passwordField;
  private Checkbox debugBox;
  private TextArea sqlArea;
  private TextArea resultArea;
  private TextField messageField;
  private DatabaseMetaData dmd;
  private Vector submissions;
  private int submitIndex = -1;
  private Connection con = null;
  private Statement stmt = null;
  private static String defaultDriver = "";
  private static String defaultURL = "";
  private static String defaultUser = "";
  private static String defaultPassword = "";
  private static String defaultDebug = "";
  private boolean debug;

  {
    appName = this.getClass().getName();
  }

  /**
  * <p>Not for public consumption.</p>
  * <p><code>DatabaseTool</code> is never instantiated directly.</p>
  * @param title The title displayed in the title bar.
  * @param subprotocol The JDBC subprotocol.
  * @param data The JDBC subprotocol data.
  * @param driver The driver specification.
  */

  public DatabaseTool(String title, String subprotocol,
      String data, String driver) {
    super(title);
    submissions = new Vector(50);
    getDatabaseToolProperties();
    setLayout(new BorderLayout(5, 5));
    InsetsPanel outerPanel = new InsetsPanel();
    outerPanel.setLayout(new BorderLayout(5, 5));
    add(outerPanel, BorderLayout.CENTER);
    Panel urlPanel = new Panel();
    outerPanel.add(urlPanel, BorderLayout.NORTH);
    Panel middlePanel = new Panel();
    middlePanel.setLayout(new BorderLayout(5, 5));
    outerPanel.add(middlePanel, BorderLayout.CENTER);
    Panel securityPanel = new Panel();
    middlePanel.add(securityPanel, BorderLayout.NORTH);

    urlPanel.add(new Label("Driver:"));
    driver = checkValue(defaultDriver, driver);
    driverField = new TextField(driver, DRIVER_WIDTH);
    driverField.addActionListener(this);
    urlPanel.add(driverField);
    urlPanel.add(new Label("    URL:"));
    String url = PROTOCOL + ":";
    url += checkValue(SUBPROTOCOL, subprotocol);
    url += ":";
    url += (data == null || data.length() == 0) ?
      SUB_DATA : data;
    urlField = new TextField(url, URL_WIDTH);
    urlField.addActionListener(this);
    urlPanel.add(urlField);
    connect = new Button(" Connect ");
    connect.addActionListener(this);
    urlPanel.add(connect);

    securityPanel.add(debugBox = new Checkbox("Debug:", debug));
    debugBox.addItemListener(new ItemListener() {
      public void itemStateChanged(ItemEvent ie) {
        debug = debugBox.getState();
      }
    });
    securityPanel.add(new Label("User:"));
    userField = new TextField(USER_WIDTH);
    userField.addActionListener(this);
    securityPanel.add(userField);
    securityPanel.add(new Label("    Password:"));
    passwordField = new TextField(PW_WIDTH);
    passwordField.addActionListener(this);
    securityPanel.add(passwordField);

    Panel innerPanel = new Panel();
    innerPanel.setLayout(new BorderLayout(5, 5));
    middlePanel.add(innerPanel, BorderLayout.CENTER);
    Panel sqlPanel = new Panel();
    sqlPanel.setLayout(new BorderLayout(5, 5));
    innerPanel.add(sqlPanel, BorderLayout.NORTH);

    sqlPanel.add(new Label("SQL:", Label.CENTER),
      BorderLayout.NORTH);
    sqlArea = new TextArea(10, SQL_WIDTH);
    sqlPanel.add(sqlArea, BorderLayout.CENTER);
    Panel buttonPanel = new Panel();
    clear = new Button(" Clear ");
    clear.addActionListener(this);
    clear.setEnabled(false);
    buttonPanel.add(clear);
    previous = new Button(" Previous ");
    previous.addActionListener(this);
    previous.setEnabled(false);
    buttonPanel.add(previous);
    next = new Button(" Next ");
    next.addActionListener(this);
    next.setEnabled(false);
    buttonPanel.add(next);
    submit = new Button(" Submit ");
    submit.addActionListener(this);
    submit.setEnabled(false);
    buttonPanel.add(submit);
    sqlPanel.add(buttonPanel, BorderLayout.SOUTH);

    Panel resultPanel = new Panel();
    resultPanel.setLayout(new BorderLayout(5, 5));
    innerPanel.add(resultPanel, BorderLayout.CENTER);
    resultPanel.add(new Label("Result:", Label.CENTER),
      BorderLayout.NORTH);
    resultArea = new TextArea(15, RESULT_WIDTH);
    resultArea.setEditable(false);
    resultPanel.add(resultArea, BorderLayout.CENTER);

    Panel messagePanel = new Panel();
    outerPanel.add(messagePanel, BorderLayout.SOUTH);
    messagePanel.add(new Label("Status:"));
    messageField = new TextField(MESSAGE_WIDTH);
    messageField.setEditable(false);
    messagePanel.add(messageField);
    pack();
    setSize(getPreferredSize());
    setVisible(true);
    addWindowListener(new ApplicationCloser());
  }

  /**
  * <p>Not for public consumption.</p>
  * <p><code>DatabaseTool</code> is never instantiated directly.</p>
  * @param title The title displayed in the title bar.
  * @param subprotocol The JDBC subprotocol.
  * @param data The JDBC subprotocol data.
  */

  public DatabaseTool(String title, String subprotocol,
      String data) {
    this(title, subprotocol, data, DRIVER);
  }

  /**
  * <p>Not for public consumption.</p>
  * <p><code>DatabaseTool</code> is never instantiated directly.</p>
  * @param title The title displayed in the title bar.
  * @param subprotocol The JDBC subprotocol.
  */

  public DatabaseTool(String title, String subprotocol) {
    this(title, subprotocol, SUB_DATA, DRIVER);
  }

  /**
  * <p>Not for public consumption.</p>
  * <p><code>DatabaseTool</code> is never instantiated directly.</p>
  * @param title The title displayed in the title bar.
  */

  public DatabaseTool(String title) {
    this(title, SUBPROTOCOL, SUB_DATA, DRIVER);
    useDefaultProperties();
  }

  /**
  * <p>Not for public consumption.</p>
  * <p><code>DatabaseTool</code> is never instantiated directly.</p>
  */

  public DatabaseTool() {
    this(TITLE, SUBPROTOCOL, SUB_DATA, DRIVER);
    useDefaultProperties();
  }

  /**
  * <p>Runs the database tool. In this form it accepts the following
  * arguments:</p>
  * <ul>
  * <li><code>DatabaseTool [[no args]]</code>
  * <li><code>DatabaseTool [[subprotocol]]</code>
  * <li><code>DatabaseTool [[subprotocol]] [[data]]</code>
  * <li><code>DatabaseTool [[subprotocol]] [[data]] [[driver]]</code>
  * </ul>
  * @param args The command-line arguments.
  */

  public static void main(String[] args) {
    if (args.length > 3 ||
        (args.length > 0 &&
          (args[0].equalsIgnoreCase("usage") ||
          args[0].equalsIgnoreCase("-usage") ||
          args[0].equalsIgnoreCase("help") ||
          args[0].equalsIgnoreCase("-help")))) {
      System.out.println("Usage: java " + DatabaseTool.class.getName() +
        " [<subprotocol> <data> <driver>]");
      return;
    }
    if (args.length == 1) {
      new DatabaseTool(TITLE, args[0]);
    }
    else if (args.length == 2) {
      new DatabaseTool(TITLE, args[0], args[1]);
    }
    else if (args.length == 3) {
      new DatabaseTool(TITLE, args[0], args[1], args[2]);
    }
    else {
      new DatabaseTool();
    }
  }

  private String checkValue(String alternateValue, String str) {
    return (str == null || str.length() == 0) ?
      alternateValue : str;
  }

  private void debugMessage(String msg) {
    if (debug) {
      System.out.println(appName + ": " + msg);
    }
  }

  private void handleMessage(String msg) {
    messageField.setText(msg);
  }

  private void useDefaultProperties() {
    if (!getDatabaseToolProperties())
      return;
    driverField.setText(defaultDriver);
    urlField.setText(defaultURL);
    userField.setText(defaultUser);
    passwordField.setText(defaultPassword);
  }

  private boolean getDatabaseToolProperties() {
    try {
      InputStream is =
        ClassLoader.getSystemResourceAsStream(PROPERTIES_FILE);
      if (is == null) {
        //System.out.println("Cannot get properties from: '" +
        //  PROPERTIES_FILE + "'.");
        return false;
      }
      Properties p = new Properties();
      p.load(is);
      defaultDriver = p.getProperty("databasetool.driver", DRIVER);
      defaultURL = p.getProperty("databasetool.url",
        PROTOCOL + ":" + SUBPROTOCOL + ":" + SUB_DATA);
      defaultUser = p.getProperty("databasetool.user", "");
      defaultPassword = p.getProperty("databasetool.password", "");
      defaultDebug = p.getProperty("databasetool.debug", "false");
      debug = defaultDebug.equalsIgnoreCase("true");
      if (debug) {
        System.out.println(appName + ": " + 
          "using properties from file '" +
          ClassLoader.getSystemResource(PROPERTIES_FILE) + "'.");
      }
      return true;
    }
    catch (Exception e) {
      System.out.println("Error getting database properties: " + e);
    return false;
    }
  }

  private boolean connectToDB() {
    closeDB();
    submit.setEnabled(false);
    try {
      String driver = driverField.getText();
      if (driver.length() == 0)
        driver = DRIVER;
      Class.forName(driver).newInstance();
      con = DriverManager.getConnection(urlField.getText(),
        userField.getText(), passwordField.getText());
      stmt = con.createStatement();
      dmd = con.getMetaData();
      String result = "Connected to: " + dmd.getURL() + LINEFEED +
        "Driver: " + dmd.getDriverName() + LINEFEED +
        "Version: " + dmd.getDriverVersion();
      resultArea.setText(result);
      handleMessage("Successfully connected to database.");
      debugMessage("Successfully connected to database.");
      submit.setEnabled(true);
      return true;
    }
    catch (Exception e) {
      handleMessage("Error connecting to database: " +
        urlField.getText());
      debugMessage("Error connecting to database: " +
        urlField.getText());
      debugMessage("Error connecting to database: " + e);
      return false;
    }
  }

  private void closeDB() {
    try {
      if (con != null)
        con.close();
    }
    catch (Exception e) {
      System.out.println(
        "Error closing the database connection: " +
        urlField.getText());
    }
  }

  private void closeStatement() {
    try {
      if (stmt != null)
        stmt.close();
    }
    catch (Exception e) {
      System.out.println("Error closing the current statement.");
    }
  }

  private void displayResultSet(ResultSet rs) {
    String result = "";
    try {
      ResultSetMetaData rsmd = rs.getMetaData();
      int cols = rsmd.getColumnCount();
      for (int i = 1; i <= cols; i++) {
        if (i > 1)
          result += ", ";
        result += rsmd.getColumnLabel(i);
      }
      result += DOUBLE_LINEFEED;
      boolean emptyResultSet = true;
      while (rs.next()) {
        emptyResultSet = false;
        for (int i = 1; i <= cols; i++) {
          if (i > 1) {
            result += ", ";
          }
          String dataType = rsmd.getColumnTypeName(i);
          String temp = rs.getString(i);
          if (temp == null) {
            temp = NULL_SYMBOL;
            result += temp;
          }
          else {
            if (dataType.equalsIgnoreCase("BLOB")) {
              temp = temp.substring(0, 10) + "....." +
                temp.substring(temp.length() - 10);
              result += temp;
            }
            else {
              result += rs.getString(i);
            }
          }
        }
        result += LINEFEED;
      }
      if (emptyResultSet) {
        result += " -- no rows -- " + LINEFEED;
      }
      resultArea.setText(result);
      handleMessage("");
    }
    catch (Exception e) {
      handleMessage("Error displaying the current statement.");
      debugMessage("Error displaying the current statement: " + e);
    }
  }

  private void executeSQL() {
    if (con == null) {
      handleMessage("Please connect to a database.");
      return;
    }
    String cmd = sqlArea.getText();
    int len = cmd.length();
    if (len == 0) {
      handleMessage("SQL statement area is empty.");
      return;
    }
    if (len < 6) {
      handleMessage("SQL statement is invalid.");
      return;
    }
    submissions.addElement(cmd);
    submitIndex =
      submissions.lastIndexOf(cmd, submissions.size() - 1);
    updateButtons();
    try {
      String keyword = cmd.substring(0, 6);
      int startLength =
        (len < STARTS_WITH_LEN) ? len : STARTS_WITH_LEN;
      String cmdStart = cmd.substring(0, startLength);
      if (keyword.equalsIgnoreCase("select")) {
        ResultSet rs = stmt.executeQuery(cmd);
        displayResultSet(rs);
        rs.close();
        handleMessage("Executed query: " + cmdStart + "...");
      }
      else if (keyword.equalsIgnoreCase("update") ||
          keyword.equalsIgnoreCase("insert") ||
          keyword.equalsIgnoreCase("delete")) {
        int result = stmt.executeUpdate(cmd);
        handleMessage("Executed update: " +
          cmdStart + "... with result: " + result);
      }
      else {
        stmt.execute(cmd);
        handleMessage("Executed: " + cmdStart + "...");
      }
    }
    catch (Exception e) {
      handleMessage("Error executing the current statement.");
      debugMessage("Error executing the current statement:" + e);
    }
  }

  private void getPreviousSubmission() {
    if (submitIndex <= 0)
      return;
    submitIndex--;
    sqlArea.setText(
      (String) submissions.elementAt(submitIndex));
    updateButtons();
  }

  private void getNextSubmission() {
    if (submitIndex >= submissions.size() - 1)
      return;
    submitIndex++;
    sqlArea.setText(
      (String) submissions.elementAt(submitIndex));
    updateButtons();
  }

  private void updateButtons() {
    previous.setEnabled(submitIndex > 0);
    next.setEnabled(submitIndex < submissions.size() - 1);
    clear.setEnabled(sqlArea.getText().length() > 0);
  }

  /**
  * <p>Not for public consumption.</p>
  * <p>Processes graphically triggered operations.</p>
  * @param event The GUI's button events.
  */

  public void actionPerformed(ActionEvent event) {
    handleMessage("");
    Object source = event.getSource();
    if (source == connect ||
        source == driverField || source == urlField ||
        source == userField || source == passwordField) {
      connectToDB();
    }
    else if (source == previous) {
      getPreviousSubmission();
    }
    else if (source == next) {
      getNextSubmission();
    }
    else if (source == submit) {
      executeSQL();
    }
    else if (source == clear) {
      sqlArea.setText("");
      resultArea.setText("");
//      handleMessage("");
    }
  }

  class InsetsPanel extends Panel {
    public Insets getInsets() {
      return new Insets(5, 5, 5, 5);
    }
  }

  class ApplicationCloser extends WindowAdapter {
    public void windowClosing(WindowEvent event) {
      if (event.getID() == WindowEvent.WINDOW_CLOSING) {
        closeStatement();
        closeDB();
        System.exit(0);
      }
    }
  }
}
