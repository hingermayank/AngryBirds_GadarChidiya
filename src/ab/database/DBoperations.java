package ab.database;

/**
 * Created by mayank.
 */
public class DBoperations extends BaseClassDB {

    String sqldriver = "org.sqlite.JDBC";
    String sqlurl = "jdbc:sqlite:dataset.db";
    public DBoperations() throws Exception {
        try {
            init(sqldriver, sqlurl);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
