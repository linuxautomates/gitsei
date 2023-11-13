package io.levelops.commons.databases.utils;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ConnectionCallback;

import java.sql.Connection;
import java.sql.SQLException;

public class TransactionCallback<T> implements ConnectionCallback<T> {

    private final ConnectionCallback<T> delegate;

    private TransactionCallback(ConnectionCallback<T> delegate) {
        this.delegate = delegate;
    }

    public static <T> TransactionCallback<T> of(ConnectionCallback<T> delegate) {
        return new TransactionCallback<>(delegate);
    }

    @Override
    public T doInConnection(Connection conn) throws SQLException, DataAccessException {
        boolean prevAutoCommit = conn.getAutoCommit();
        conn.setAutoCommit(false);
        try {
            T returnValue = delegate.doInConnection(conn);
            conn.commit();
            return returnValue;
        } catch (SQLException | DataAccessException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.rollback();
            conn.setAutoCommit(prevAutoCommit);
        }
    }
}
