package org.elshift.db;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.HashMap;

public class Database {
    private Connection con = null;
    private Statement stmt = null;
    private final HashMap<Class<?>, SqlGenerator> sqlTypes = new HashMap<>();
    private static final Logger logger = LoggerFactory.getLogger(Database.class);

    public Database(String server, String user, String pass) throws SQLException {
        reconnect(server, user, pass);
    }

    public Database(Connection con) {
        this.con = con;
    }

    public Database() {
        this.con = null;
    }

    public void reconnect(String server, String user, String pass) throws SQLException {
        closeIfConnected();
        if (server != null) {
            if (user == null || pass == null)
                con = DriverManager.getConnection(server);
            else
                con = DriverManager.getConnection(server, user, pass);
        }

        if (con != null)
            stmt = con.createStatement();
    }

    public boolean seed(Class<?>... types) {
        for (Class<?> c : types) {
            if (executeUpdate(getOrMakeGen(c).createTable()) == -1)
                return false;
        }
        return true;
    }

    public boolean insert(Object o) {
        return executeUpdate(getTypeGen(o.getClass()).insertInto(o)) > 0;
    }

    public boolean updateWhere(Object o, String sqlCondition, String... sqlFields) {
        SqlGenerator gen = getTypeGen(o.getClass());
        return executeUpdate(gen.updateWhere(gen.getSqlName(), o, sqlCondition, sqlFields)) > 0;
    }

    public boolean updateWherePrimaryKey(Object o, String... sqlFields) {
        SqlGenerator gen = getTypeGen(o.getClass());
        return executeUpdate(gen.updateWherePrimaryKey(gen.getSqlName(), o, sqlFields)) > 0;
    }

    public boolean updateOrInsert(Object o) {
        SqlGenerator gen = getTypeGen(o.getClass());
        return executeUpdate(gen.updateOrInsert(gen.getSqlName(), o)) > 0;
    }

    /**
     * Runs an arbitrary SQL query, and expects the results to match the given class
     *
     * @param klass    Java class being queried
     * @param sqlQuery SQL query to execute
     * @return A reader utility that wraps {@link ResultSet}
     */
    public <T> SqlObjectReader<T> queryObjects(Class<T> klass, String sqlQuery) {
        ResultSet sqlResult = executeQuery(sqlQuery);
        if (sqlResult == null)
            return null;

        return new SqlObjectReader<>(sqlResult, getTypeGen(klass));
    }

    /**
     * Will retrieve the first matching object from SQL, given an input object
     *
     * @param output       Instance whose values will be assigned to the retrieved SQL values
     * @param sqlCondition SQL condition that returns true for the desired object
     * @param <T>          Type to retrieve
     * @return The output object with its values updated to reflect the retrieved SQL fields.
     * Returns null on failure.
     */
    public <T> T getFirstObjectWhere(T output, String sqlCondition) {
        SqlGenerator gen = getTypeGen(output.getClass());
        ResultSet sqlResult = executeQuery(gen.selectWhere(gen.getSqlName(), sqlCondition));
        if (sqlResult == null)
            return null;

        T result = null;
        try {
            if (!sqlResult.next())
                return null;

            result = SqlObjectReader.readObject(sqlResult, gen, output);
            sqlResult.close();
        } catch (SQLException e) {
            logger.error("Failed to read SQL ResultSet", e);
        }

        return result;
    }

    /**
     * Will retrieve the first matching object from SQL, given an input object and the SQL fields that should match
     *
     * @param input     Instance holding desired values to match
     * @param output    Instance whose values will be assigned to the retrieved SQL values
     * @param sqlFields The names of fields that must match
     * @param <T>       Type to retrieve
     * @return The output object with its values updated to reflect the retrieved SQL fields.
     * Returns null on failure.
     */
    public <T> T getFirstObjectWhereFieldsMatch(T input, T output, String... sqlFields) {
        SqlGenerator gen = getTypeGen(input.getClass());
        String sqlCondition = gen.matchingFieldsCondition(input, sqlFields);
        if (sqlCondition == null)
            return null;

        return getFirstObjectWhere(output, sqlCondition);
    }

    public <T> SqlGenerator getTypeGen(Class<T> c) {
        return sqlTypes.get(c);
    }

    public void closeIfConnected() throws SQLException {
        if (isConnected())
            con.close();
        con = null;
        stmt = null;
    }

    public boolean isConnected() {
        return con != null; // && con.isValid() // This requires extra wait time
    }

    private int executeUpdate(String sql) {
        try {
            return stmt.executeUpdate(sql);
        } catch (SQLException e) {
            logger.error("executeUpdate failure", e);
            return -1;
        }
    }

    private ResultSet executeQuery(String sql) {
        try {
            return stmt.executeQuery(sql);
        } catch (SQLException e) {
            logger.error("executeUpdate failure", e);
            return null;
        }
    }

    private <T> SqlGenerator getOrMakeGen(Class<T> c) {
        SqlGenerator gen = sqlTypes.get(c);
        if (gen != null)
            return gen;
        gen = new SqlGenerator(c);
        sqlTypes.put(c, gen);
        return gen;
    }
}
