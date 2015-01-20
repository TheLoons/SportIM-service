package org.sportim.service.util;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Interface for SQL sconnection providers.
 */
public interface ConnectionProvider {
    public Connection getConnection() throws SQLException;
}
