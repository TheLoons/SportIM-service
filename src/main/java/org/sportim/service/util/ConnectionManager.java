package org.sportim.service.util;

import javax.naming.InitialContext;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Connection management for the SportIM APIs.
 *
 * Created by hannah on 11/22/14.
 */
public class ConnectionManager {
    private static ConnectionManager instance = null;
    private DataSource ds;

    private ConnectionManager() {
        try {
            InitialContext ctx = new InitialContext();
            ds = (DataSource) ctx.lookup("java:comp/env/jdbc/SportIMDB");
        } catch (Exception e) {
            // TODO remove this - use log4j2
            e.printStackTrace();
        }
    }

    public Connection getConnection() throws SQLException {
        // Set the timezone on the connection
        if (ds == null)
            return null;

        Connection conn = ds.getConnection();
        Statement stmt = conn.createStatement();
        stmt.execute("SET time_zone='+00:00'");
        return conn;
    }

    public static ConnectionManager getInstance() {
        if (instance == null) {
            instance = new ConnectionManager();
        }
        return instance;
    }
}
