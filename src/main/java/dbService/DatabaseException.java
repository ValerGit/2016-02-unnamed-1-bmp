package dbservice;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DatabaseException extends Exception {

    public DatabaseException( String error, RuntimeException e ) {
        super(error, e);
    }

}
