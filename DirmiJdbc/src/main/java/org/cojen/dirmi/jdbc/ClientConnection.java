/*
 *  Copyright 2007-2009 Brian S O'Neill
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.cojen.dirmi.jdbc;

import java.rmi.RemoteException;

import java.sql.Array;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.NClob;
import java.sql.PreparedStatement;
import java.sql.Savepoint;
import java.sql.SQLClientInfoException;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Statement;
import java.sql.Struct;

import java.util.Properties;

import org.cojen.dirmi.util.Wrapper;

/**
 * 
 *
 * @author Brian S O'Neill
 */
public abstract class ClientConnection implements Connection {
    private static final Wrapper<ClientConnection, RemoteConnection> wrapper =
        Wrapper.from(ClientConnection.class, RemoteConnection.class);

    public static ClientConnection from(RemoteConnection con) {
        return wrapper.wrap(con);
    }

    private final RemoteConnection mConnection;

    protected ClientConnection(RemoteConnection con) {
        mConnection = con;
    }

    public Statement createStatement() throws SQLException {
        return ClientStatement.from(mConnection.createStatement());
    }

    public PreparedStatement prepareStatement(String sql) throws SQLException {
        return ClientPreparedStatement.from(mConnection.prepareStatement(sql));
    }

    public CallableStatement prepareCall(String sql) throws SQLException {
        throw new SQLException("FIXME (prepareCall)");
    }

    public DatabaseMetaData getMetaData() throws SQLException {
        return ClientDatabaseMetaData.from(mConnection.getMetaData());
    }

    public Statement createStatement(int resultSetType, int resultSetConcurrency) 
        throws SQLException
    {
        return ClientStatement.from
            (mConnection.createStatement(resultSetType, resultSetConcurrency));
    }

    public PreparedStatement prepareStatement(String sql, int resultSetType, 
                                              int resultSetConcurrency)
        throws SQLException
    {
        return ClientPreparedStatement.from
            (mConnection.prepareStatement(sql, resultSetType, resultSetConcurrency));
    }

    public CallableStatement prepareCall(String sql, int resultSetType, 
                                         int resultSetConcurrency)
        throws SQLException
    {
        throw new SQLException("FIXME (prepareCall)");
    }

    public Savepoint setSavepoint() throws SQLException {
        throw new SQLException("FIXME (setSavepoint)");
    }

    public Savepoint setSavepoint(String name) throws SQLException {
        throw new SQLException("FIXME (setSavepoint)");
    }

    public void rollback(Savepoint savepoint) throws SQLException {
        throw new SQLException("FIXME (rollback)");
    }

    public void releaseSavepoint(Savepoint savepoint) throws SQLException {
        throw new SQLException("FIXME (releaseSavepoint)");
    }

    public Statement createStatement(int resultSetType, int resultSetConcurrency, 
                                     int resultSetHoldability)
        throws SQLException
    {
        return ClientStatement.from
            (mConnection.createStatement(resultSetType, resultSetConcurrency,
                                         resultSetHoldability));
    }

    public PreparedStatement prepareStatement(String sql, int resultSetType, 
                                              int resultSetConcurrency, int resultSetHoldability)
        throws SQLException
    {
        return ClientPreparedStatement.from
            (mConnection.prepareStatement
             (sql, resultSetType, resultSetConcurrency, resultSetHoldability));
    }

    public CallableStatement prepareCall(String sql, int resultSetType, 
                                         int resultSetConcurrency, 
                                         int resultSetHoldability)
        throws SQLException
    {
        throw new SQLException("FIXME (prepareCall)");
    }

    public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys)
        throws SQLException
    {
        return ClientPreparedStatement.from
            (mConnection.prepareStatement(sql, autoGeneratedKeys));
    }

    public PreparedStatement prepareStatement(String sql, int columnIndexes[])
        throws SQLException
    {
        return ClientPreparedStatement.from(mConnection.prepareStatement(sql, columnIndexes));
    }

    public PreparedStatement prepareStatement(String sql, String columnNames[])
        throws SQLException
    {
        return ClientPreparedStatement.from(mConnection.prepareStatement(sql, columnNames));
    }

    public Clob createClob() throws SQLException {
        throw new SQLException("FIXME (createClob)");
    }

    public Blob createBlob() throws SQLException {
        throw new SQLException("FIXME (createBlob)");
    }
    
    public NClob createNClob() throws SQLException {
        throw unsupported();
    }

    public SQLXML createSQLXML() throws SQLException {
        throw unsupported();
    }

    public void setClientInfo(String name, String value) throws SQLClientInfoException {
        try {
            mConnection.setClientInfo(name, value);
        } catch (RemoteException e) {
            SQLClientInfoException ex = new SQLClientInfoException();
            ex.initCause(e);
            throw ex;
        }
    }
        
    public void setClientInfo(Properties properties) throws SQLClientInfoException {
        try {
            mConnection.setClientInfo(properties);
        } catch (RemoteException e) {
            SQLClientInfoException ex = new SQLClientInfoException();
            ex.initCause(e);
            throw ex;
        }
    }
        
    public Array createArrayOf(String typeName, Object[] elements) throws SQLException {
        throw unsupported();
    }

    public Struct createStruct(String typeName, Object[] attributes) throws SQLException {
        throw unsupported();
    }

    public <T> T unwrap(Class<T> iface) throws SQLException {
        throw unsupported();
    }

    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return false;
    }

    private static SQLException unsupported() throws SQLException {
        return ClientDriver.unsupported();
    }
}
