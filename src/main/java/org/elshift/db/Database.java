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

    /**
     * Initializes the necessary Java info and SQL tables for the given Java types, if not already done
     *
     * @param types A list of Java types to add to the database
     * @return False if errors occurred
     */
    public boolean seed(Class<?>... types) throws SQLException {
        for (Class<?> c : types) {
            SqlGenerator gen = getOrMakeGen(c);
            if (executeUpdate(gen.createTable(gen.getSqlName())) == -1)
                return false;
        }
        return true;
    }

    public PreparedStatement preparedStatement(String sql) throws SQLException {
        return con.prepareStatement(sql);
    }

    public boolean insert(Object o) throws SQLException {
        SqlGenerator gen = getTypeGen(o.getClass());
        return executeUpdate(gen.insertInto(gen.getSqlName(), o)) > 0;
    }

    public boolean updateWhere(Object o, String sqlCondition, String... sqlFields) throws SQLException {
        SqlGenerator gen = getTypeGen(o.getClass());
        return executeUpdate(gen.updateWhere(gen.getSqlName(), o, sqlCondition, sqlFields)) > 0;
    }

    public boolean updateWherePrimaryKey(Object o, String... sqlFields) throws SQLException {
        SqlGenerator gen = getTypeGen(o.getClass());
        return executeUpdate(gen.updateWherePrimaryKey(gen.getSqlName(), o, sqlFields)) > 0;
    }

    public boolean updateOrInsert(Object o) throws SQLException {
        SqlGenerator gen = getTypeGen(o.getClass());
        return executeUpdate(gen.updateOrInsert(gen.getSqlName(), o)) > 0;
    }

    public <T> boolean updateOrInsertMany(Class<T> klass, Iterable<T> o) throws SQLException {
        SqlGenerator gen = getTypeGen(klass);
        return executeUpdate(gen.updateOrInsertMany(gen.getSqlName(), (Iterable<Object>) o)) > 0;
    }

    /**
     * Executes an arbitrary SQL query, and expects the results to match the given class
     *
     * @param klass    Java class being queried
     * @param sqlQuery SQL query to execute
     * @return A reader utility that wraps {@link ResultSet}
     */
    public <T> SqlObjectReader<T> queryObjects(Class<T> klass, String sqlQuery) throws SQLException {
        ResultSet sqlResult = executeQuery(sqlQuery);
        if (sqlResult == null)
            return null;

        return new SqlObjectReader<>(sqlResult, getTypeGen(klass));
    }

    /**
     * Attempts to return the first simple value (int, char, bool, str)
     *
     * @param type      Java type to return
     * @param sqlResult SQL cursor pointing at the desired value
     * @param <T>       Java type to return
     * @return An instance of {@code type} upon success. {@code null} on failure.
     */
    public <T> T querySimple(Class<T> type, ResultSet sqlResult) throws SQLException {
        if (!sqlResult.next())
            return null;

        Object o = sqlResult.getObject(1);
        if (type.isInstance(o))
            return type.cast(o);
        return null;
    }

    /**
     * Executes an arbitrary SQL query, and attempts to return the first simple value (int, char, bool, str)
     *
     * @param type     Java type to return
     * @param sqlQuery SQL query to execute
     * @param <T>      Java type to return
     * @return An instance of {@code type} upon success. {@code null} on failure.
     */
    public <T> T querySimple(Class<T> type, String sqlQuery) throws SQLException {
        try (ResultSet sqlResult = stmt.executeQuery(sqlQuery)) {
            return querySimple(type, sqlResult);
        }
    }

    /**
     * Executes an arbitrary SQL query
     *
     * @param sqlQuery SQL query to execute
     * @return A {@link ResultSet} instance on success. {@code null} on failure.
     */
    public ResultSet executeQuery(String sqlQuery) throws SQLException {
        return stmt.executeQuery(sqlQuery);
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
    public <T> T getFirstObjectWhere(T output, String sqlCondition) throws SQLException {
        SqlGenerator gen = getTypeGen(output.getClass());
        ResultSet sqlResult = executeQuery(gen.selectWhere(gen.getSqlName(), sqlCondition));
        if (sqlResult == null)
            return null;

        T result = null;
        if (!sqlResult.next())
            return null;

        result = SqlObjectReader.readObject(sqlResult, gen, output);
        sqlResult.close();

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
    public <T> T getFirstObjectWhereFieldsMatch(T input, T output, String... sqlFields) throws SQLException {
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

    private int executeUpdate(String sql) throws SQLException {
        return stmt.executeUpdate(sql);
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
