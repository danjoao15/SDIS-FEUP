package database;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import util.Utils;

public class Database {


	private String driver = "org.apache.derby.jdbc.EmbeddedDriver";
	private String db="localDB";
	private String curl = "jdbc:derby:" + db + ";create=true";
	private String initsql = "initDB.sql";

	private Connection c = null;


	public Database(String s){
		db = "localDB"+s;
		curl = "jdbc:derby:" + db + ";create=true";
		connect();
		if (!dbexists()) {
			dbload();
		}

	}
	private boolean dbexists() {
		try {
			DatabaseMetaData meta = c.getMetaData();
			ResultSet tab = meta.getTables(c.getCatalog(), null, "FILESSTORED", null);
			boolean tabexists = tab.next();
			Utils.LOG.finest("database exists - " + tabexists);
			return tabexists;
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return false;

	}

	public void connect() {
		try {
			c = DriverManager.getConnection(curl);
			Utils.LOG.finest("onnected to " + db);

		} catch (SQLException e) {
			e.printStackTrace();
		}  	
	}

	public Connection getConnection() {
		return c;
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
			if (!gotSQLExc) {
				Utils.LOG.finest("problem ending connection");
			}  else  {
				Utils.LOG.finest("connection ended");	
			}  
		}
	}

	public void dbload() {
		String sqlscript = Utils.read(initsql);
		runscript(sqlscript);
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
}
