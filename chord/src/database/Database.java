package database;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import utils.Utils;

public class Database {

	// define the driver to use 
	private String driver = "org.apache.derby.jdbc.EmbeddedDriver";
	// the database name  
	private String dbName="localDB";
	// define the Derby connection URL to use 
	private String connectionURL = "jdbc:derby:" + dbName + ";create=true";

	private String initScript = "initDB.sql";

	private Connection conn = null;


	public Database(String string){
		dbName = "localDB"+string;
		connectionURL = "jdbc:derby:" + dbName + ";create=true";
		connect();
		if (!checkDBExisted()) {
			loadDB();
		}

	}
	private boolean checkDBExisted() {
		try {
			DatabaseMetaData metadata = conn.getMetaData();
			ResultSet tables = metadata.getTables(conn.getCatalog(), null, "FILESSTORED", null);
			boolean tableExists = tables.next();
			Utils.LOGGER.finest("DB existed: " + tableExists);
			return tableExists;
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return false;

	}

	public void connect() {
		try {
			conn = DriverManager.getConnection(connectionURL);
			Utils.LOGGER.finest("Connected to database " + dbName);

		} catch (SQLException e) {
			e.printStackTrace();
		}  	
	}

	public Connection getConnection() {
		return conn;
	}

	public void closeConnection() {
		if (driver.equals("org.apache.derby.jdbc.EmbeddedDriver")) {
			boolean gotSQLExc = false;
			try {
				DriverManager.getConnection("jdbc:derby:;shutdown=true");
			} catch (SQLException se)  {	
				if ( se.getSQLState().equals("XJ015") ) {		
					gotSQLExc = true;
				}
			}
			if (!gotSQLExc) {
				Utils.LOGGER.finest("Database did not shut down normally");
			}  else  {
				Utils.LOGGER.finest("Database shut down normally");	
			}  
		}
	}

	public void loadDB() {
		String sqlScript = Utils.readFile(initScript);
		runScript(sqlScript);
	}

	private void runScript(String sql) {
		try {
			Statement stmt = conn.createStatement();
			String[] stmtList = sql.split(";");
			for (int i = 0; i < stmtList.length; i++) {
				String currentStmt = stmtList[i].trim();
				if (!currentStmt.isEmpty()) {
					stmt.execute(stmtList[i].trim());	
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}	
	}
}
