package com.matteo.AppAPIserver.Database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Questa classe contiene i metodi per connettersi al database
 * 
 * @author Matteo Basso
 */
public class Database {
	/**
	 * Questo metodo instaura una connessione con il database
	 * 
	 * @return l'oggetto Connection
	 * @throws SQLException in caso che si sia verificato un errore di connessione
	 */
	public static Connection connect() throws SQLException {
		return DriverManager.getConnection("jdbc:mysql://127.0.0.1:3306/CompravenditaAzioni", "root", "");
	}

	public static void closeConnection(Connection conn, Statement stmt, ResultSet rs) {
		if (rs != null) {
			try {
				rs.close();
			} catch (SQLException e) {}
		}

		if (stmt != null) {
			try {
				stmt.close();
			} catch (SQLException e) {}
		}

		if (conn != null) {
			try {
				conn.close();
			} catch (SQLException e) {}
		}
	}

	public static void closeConnection(Connection conn, Statement stmt) {
		closeConnection(conn, stmt, null);
	}
}
