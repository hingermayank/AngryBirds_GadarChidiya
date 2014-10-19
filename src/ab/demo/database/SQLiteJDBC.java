package ab.demo.database;

/**
 * Created by mayank on 12/11/14.
 */
import java.sql.*;

public class SQLiteJDBC
{
    public SQLiteJDBC()

    {
        Connection c = null;
        try {
            Class.forName("org.sqlite.JDBC");
            c = DriverManager.getConnection("jdbc:sqlite:test.db");
        } catch (Exception e) {
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
            System.exit(0);
        }
        System.out.println("Opened database successfully");
    }
}