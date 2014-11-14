package ab.database;

import java.sql.*;

/**
 * Created by Mayank.
 */
public class BaseClassDB {

    public String sDriver = "org.sqlite.JDBC";
    public String sUrl = "jdbc:sqlite:dataset.db";
    public int iTimeout = 30;
    public Connection conn = null;
    public Statement statement = null;
    public PreparedStatement prep = null;

    // Stub constructor for quick instantiation o/t fly for using some of the ancillary stuff
    //quick and dirty constructor to test the database passing the DriverManager name and the fully loaded url to handle
    // NB this will typically be available if you make this class concrete and not abstract


    public void init(String sDriverVar, String sUrlVar) throws Exception {
        setDriver(sDriverVar);
        setUrl(sUrlVar);
        setConnection();
        //setStatement();
        createTable();
    }

    private void setDriver(String sDriverVar) {
        sDriver = sDriverVar;
    }

    private void setUrl(String sUrlVar) {
        sUrl = sUrlVar;
    }

    public void setConnection() throws Exception {
        Class.forName(sDriver);
        conn = DriverManager.getConnection(sUrl);
    }


    public Connection getConnection() {
        return conn;
    }

    public void setStatement() throws Exception {
        if (conn == null) {
            setConnection();
        }
        statement = conn.createStatement();
        statement.setQueryTimeout(iTimeout);  // set timeout to 30 sec.
    }

    public void setPreparedStatement() throws Exception {
        if (conn == null) {
            setConnection();
        }
        prep = conn.prepareStatement("insert into datapoints values (?, ?, ?, ?, ?, ?, ?, ?);");
        prep.setQueryTimeout(iTimeout);  // set timeout to 30 sec.
    }

    public Statement getStatement() {
        return statement;
    }

    public void executeStmt(String instruction) throws SQLException {
        statement.executeUpdate(instruction);
    }

    // processes an array of instructions e.g. a set of SQL command strings passed from a file
    //NB you should ensure you either handle empty lines in files by either removing them or parsing them out
    // since they will generate spurious SQLExceptions when they are encountered during the iteration....
    public void executeStmt(String[] instructionSet) throws SQLException {
        for (int i = 0; i < instructionSet.length; i++) {
            executeStmt(instructionSet[i]);
        }
    }

    public ResultSet executeQry(String instruction) throws SQLException {
        return statement.executeQuery(instruction);
    }

    public void closeConnection() {
        try {
            conn.close();
        } catch (Exception ignore) {
        }
    }

    public void createTable() {
        try {
            setStatement();
            statement.execute("CREATE TABLE IF NOT EXISTS datapoints(\n" +
                    "   TYPE TEXT,\n" +
                    "   ANGLE REAL,\n" +
                    " REACHABLE INT,\n" +
                    "   PWEIGHT REAL,\n" +
                    "   AWEIGHT REAL,\n" +
                    "   DISTANCE REAL,\n" +
                    " WEAKNESS REAL,\n" +
                    " SCORE INT\n" +
                    ");");
            System.out.println("Table created.");
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void inset(String type, double angle, int reachable, double pweight, double aweight, double distance, double weakness, int score) {
        try {
            setPreparedStatement();


            prep.setString(1, type);
            prep.setDouble(2, angle);
            prep.setDouble(3, reachable);
            prep.setDouble(4, pweight);
            prep.setDouble(5, aweight);
            prep.setDouble(6, distance);
            prep.setDouble(7, weakness);
            prep.setInt(8, score);

            conn.setAutoCommit(false);
            prep.executeUpdate();
            conn.setAutoCommit(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}


