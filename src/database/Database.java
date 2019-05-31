package database;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import util.Loggs;

public class Database {

	private String db = "database_";
	private String initsql = "launchdatabase.sql";
	private String driver = "org.apache.derby.jdbc.EmbeddedDriver";
	private String curl = "jdbc:derby:" + db + ";create=true";
	
	private Connection c = null;

	public Database(String s){
		db = "database_" + s;
		curl = "jdbc:derby:" + db + ";create=true";
		connect();
		if (!dbexists()) {
			dbload();
			System.out.println("database for " + s + " created");
		}
		else {
			System.out.println("database for " + s + " already exists");
		}
	}
	
	private boolean dbexists() {
		try {
			DatabaseMetaData meta = c.getMetaData();
			ResultSet tab = meta.getTables(c.getCatalog(), null, "FILESSTORED", null);
			boolean tabexists = tab.next();
			if(tabexists) {
				Loggs.LOG.finest("database exists");
			}
			else {
				Loggs.LOG.finest("database does not yet exist");
			}
			return tabexists;
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return false;
	}
	
	public void dbload() {
		String script = Loggs.read(initsql);
		runscript(script);
	}

	public void connect() {
		try {
			c = DriverManager.getConnection(curl);
			Loggs.LOG.finest("connected to " + this.db);
		} catch (SQLException e) {
			e.printStackTrace();
		}  	
	}

	public void endConnection() {
		if (driver.equals("org.apache.derby.jdbc.EmbeddedDriver")) {
			boolean gotSQLExc = false;
			try {
				DriverManager.getConnection("jdbc:derby:;shutdown=true");
			} catch (SQLException e)  {	
				if ( e.getSQLState().equals("XJ015") ) {		
					gotSQLExc = true;
				}
			}
			if (gotSQLExc) {
				Loggs.LOG.finest("connection ended");	
			}
			else  {
				Loggs.LOG.finest("problem ending connection");
			}  
		}
	}

	private void runscript(String script) {
		try {
			Statement s = c.createStatement();
			String[] list = script.split(";");
			for (int i = 0; i < list.length; i++) {
				String item = list[i].trim();
				if (!item.isEmpty()) {
					s.execute(list[i].trim());	
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}	
	}

	public Connection getConnection() {
		return c;
	}

}
