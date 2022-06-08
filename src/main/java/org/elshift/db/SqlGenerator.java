package org.elshift.db;

import org.elshift.db.annotations.SqlName;
import org.elshift.db.annotations.SqlNotNull;
import org.elshift.db.annotations.SqlPrimaryKey;
import org.elshift.db.annotations.SqlUnique;
import org.elshift.modules.impl.DownloadModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Generates SQL statement strings from an object or object type.
 * <br><i><b><u>    Absolutely unsafe!    </u></b></i>
 * Only use the generated statements in a trusted environment where no unwanted code can get loaded in any way.
 */
public class SqlGenerator {
    public enum SqlType {
        BIT,
        INT,
        BIGINT,
        CHAR,
        TEXT;

        public final String sqlSyntax;

        SqlType() {
            sqlSyntax = this.name().replace('_', ' ');
        }
    }

    public enum SqlConstraint {
        NOT_NULL,
        UNIQUE,
        PRIMARY_KEY,
        FOREIGN_KEY;

        public final String sqlSyntax;

        SqlConstraint() {
            sqlSyntax = this.name().replace('_', ' ');
        }
    }

    private static final HashMap<Class, SqlType> javaToSqlType = new HashMap<>() {{
        put(boolean.class, SqlType.BIT);
        put(Boolean.class, SqlType.BIT);
        put(int.class, SqlType.INT);
        put(Integer.class, SqlType.INT);
        put(long.class, SqlType.BIGINT);
        put(Long.class, SqlType.BIGINT);
        put(char.class, SqlType.CHAR);
        put(Character.class, SqlType.CHAR);
        put(String.class, SqlType.TEXT);
    }};

    private static final HashMap<Class, SqlConstraint> javaToSqlConstraint = new HashMap<>() {{
        put(SqlNotNull.class, SqlConstraint.NOT_NULL);
        put(SqlUnique.class, SqlConstraint.UNIQUE);
        put(SqlPrimaryKey.class, SqlConstraint.PRIMARY_KEY);
    }};

    private final Class type;
    private final String typeSqlName;
    private final HashMap<String, SqlField> fieldMap = new HashMap<>();

    private static final Logger logger = LoggerFactory.getLogger(DownloadModule.class);

    public SqlGenerator(Class type) {
        this.type = type;

        SqlName typeSqlNameAnn = (SqlName) type.getAnnotation(SqlName.class);
        if (typeSqlNameAnn != null)
            this.typeSqlName = typeSqlNameAnn.name();
        else
            this.typeSqlName = type.getSimpleName();

        // Find all class fields that fit an SQL type and add to the fields list
        // TODO: Error if multiple fields have the same SQL name
        boolean hasPrimaryKey = false;
        ArrayList<SqlConstraint> constraints = new ArrayList<>();
        for (Field f : type.getDeclaredFields()) {
            SqlType sqlType = javaToSqlType.get(f.getType());
            if (sqlType == null)
                continue;

            String sqlName = f.getName();
            SqlName sqlNameAnn = f.getAnnotation(SqlName.class);
            if (sqlNameAnn != null)
                sqlName = sqlNameAnn.name();

            for (Annotation ann : f.getAnnotations()) {
                String debug = ann.annotationType().getName();
                SqlConstraint c = javaToSqlConstraint.get(ann.annotationType());
                if (c != null) {
                    constraints.add(c);
                }
            }

            fieldMap.put(sqlName, new SqlField(sqlName, f, sqlType, constraints));
            constraints = new ArrayList<>();
        }
    }

    /**
     * Generates a statement for creating an SQL table
     *
     * @param tableName Name of SQL table to create
     * @return Statement that creates an SQL table
     */
    public String createTable(String tableName) {
        try {
            StringBuilder req = new StringBuilder(
                    "CREATE TABLE IF NOT EXISTS %s (%s".formatted(
                            tableName,
                            listToString(fieldMap.values(), SqlField::toDeclarationString)
                    )
            );

            // If any fields are primary keys, write the names again with different syntax (because SQL)
            List<SqlField> primaryKeys = getPrimaryKeys();
            if (!primaryKeys.isEmpty())
                req.append(", PRIMARY KEY(%s)".formatted(listToString(primaryKeys, SqlField::sqlName)));

            req.append(')');
            return req.toString();
        } catch (IllegalAccessException e) {
            logger.error("This exception cannot possibly happen", e);
            return null;
        }
    }

    /**
     * Generates a statement for creating an SQL table named after the type
     *
     * @return Statement that creates an SQL table
     */
    public String createTable() {
        return createTable(typeSqlName);
    }

    /**
     * Generates a statement for inserting an object's values in an SQL table
     *
     * @param tableName Destination
     * @param instance  An instance of the target class
     * @return Statement that inserts the object's values (at the time of call) in an SQL table
     */
    public String insertInto(String tableName, Object instance) {
        if (!type.isInstance(instance)) {
            logger.error("Invalid instance provided");
            return null;
        }

        try {
            return "INSERT INTO %s (%s) VALUES (%s)".formatted(
                    tableName,
                    listToString(fieldMap.values(), SqlField::sqlName),
                    listToString(fieldMap.values(), f -> f.toValueString(instance))
            );
        } catch (IllegalAccessException e) {
            logger.error("Couldn't access fields of valid instance. Perhaps the target class has mutated?", e);
            return null;
        }
    }

    /**
     * Generates a statement for inserting an object's values in an SQL table named after the type
     *
     * @param instance An instance of the target class
     * @return Statement that inserts the object's values (at the time of call) in an SQL table
     */
    public String insertInto(Object instance) {
        return insertInto(typeSqlName, instance);
    }

    /**
     * Generates a statement for updating SQL value(s) in one or more rows
     *
     * @param tableName     Destination
     * @param instance      An instance of the target class
     * @param sqlCondition  SQL condition that updates each applicable row
     * @param sqlFieldNames If set, only these fields will update. Otherwise, all fields will update.
     * @return Statement that updates existing SQL rows with the Java object's value(s)
     */
    public String updateWhere(String tableName, Object instance, String sqlCondition, String... sqlFieldNames) {
        Iterable<SqlField> toBeUpdated = getVarargFields(sqlFieldNames);
        if (toBeUpdated == null)
            return null;

        try {
            return "UPDATE %s SET %s WHERE %s".formatted(
                    tableName,
                    listToString(toBeUpdated, f -> f.sqlName() + " = " + f.toValueString(instance)),
                    sqlCondition
            );

        } catch (IllegalAccessException e) {
            logger.error("Couldn't access fields of valid instance. Perhaps the target class has mutated?", e);
            return null;
        }
    }

    /**
     * Generates a statement for updating the SQL value(s) for one unique row.
     * The target class must have a primary key.
     *
     * @param tableName     Destination
     * @param instance      An instance of the target class
     * @param sqlFieldNames If set, only these fields will update. Otherwise, all fields will update.
     * @return Statement that updates the corresponding SQL row with the Java object's values.
     * Returns null if target class has no primary key.
     */
    public String updateWherePrimaryKey(String tableName, Object instance, String... sqlFieldNames) {
        String sqlCondition = primaryKeyCondition(instance);
        if (sqlCondition == null)
            return null;
        return updateWhere(tableName, instance, sqlCondition, sqlFieldNames);
    }

    /**
     * Generates a statement for adding or updating a unique SQL row.
     * The target class must have a primary key.
     *
     * @param tableName Destination
     * @param instance  An instance of the target class
     * @return Statement that updates or inserts one Java object's values.
     */
    public String updateOrInsert(String tableName, Object instance) {
        String sqlCondition = primaryKeyCondition(instance);
        if (sqlCondition == null)
            return null;

        try {
            // Technically not a proper upsert, but all the values are provided so that nothing is lost
            return "INSERT OR REPLACE INTO %s (%s) VALUES (%s)".formatted(
                    tableName,
                    listToString(fieldMap.values(), SqlField::sqlName),
                    listToString(fieldMap.values(), f -> f.toValueString(instance))
            );
        } catch (IllegalAccessException e) {
            logger.error("Couldn't access fields of valid instance. Perhaps the target class has mutated?", e);
            return null;
        }
    }

    /**
     * Generates a statement for selecting field(s) from SQL rows
     *
     * @param tableName    Source
     * @param sqlCondition SQL condition to retrieve each applicable row
     * @param sqlFields    Fields to retrieve. Retrieves all fields if empty.
     * @return Statement that retrieves the specified fields from all the applicable rows
     */
    public String selectWhere(String tableName, String sqlCondition, String... sqlFields) {
        try {
            String selection = "*";
            if (sqlFields.length > 0) {
                Iterable<SqlField> toRetrieve = getVarargFields(sqlFields);
                if (toRetrieve == null)
                    return null;
                selection = "(%s)".formatted(listToString(toRetrieve, SqlField::sqlName));
            }
            return "SELECT %s FROM %s WHERE %s".formatted(selection, tableName, sqlCondition);
        } catch (IllegalAccessException e) {
            logger.error("Couldn't access fields of valid instance. Perhaps the target class has mutated?", e);
            return null;
        }
    }

    /**
     * Generates a statement for selecting field(s) from the SQL row corresponding to the Java object.
     * The target class must have a primary key.
     *
     * @param tableName Source
     * @param instance  An instance of the target class
     * @param sqlFields Fields to retrieve. Retrieves all fields if empty.
     * @return Statement that retrieves the specified fields corresponding to the Java object
     */
    public String selectWherePrimaryKey(String tableName, Object instance, String... sqlFields) {
        String sqlCondition = primaryKeyCondition(instance);
        if (sqlCondition == null)
            return null;
        return selectWhere(tableName, sqlCondition, sqlFields);
    }

    /**
     * SQL condition for rows with SQL field(s) that must equal Java fields
     *
     * @param instance  Acts as the input values.
     *                  Must have desired field values set.
     *                  Must be an instance of the target class.
     * @param sqlFields Fields to match. Matches all fields if empty.
     * @return SQL condition that is true when Java fields' values (at the time of call) are equal to SQL fields
     */
    public String matchingFieldsCondition(Object instance, String... sqlFields) {
        Iterable<SqlField> toMatch = getVarargFields(sqlFields);
        if (toMatch == null)
            return null;

        try {
            return "(%s) = (%s)".formatted(
                    listToString(toMatch, SqlField::sqlName),
                    listToString(toMatch, f -> f.toValueString(instance))
            );
        } catch (IllegalAccessException e) {
            logger.error("Couldn't access fields of valid instance. Perhaps the target class has mutated?", e);
            return null;
        }
    }

    /**
     * SQL condition for equal primary keys.
     * The target class must have a primary key.
     *
     * @return SQL condition that is true where primary key values are equal.
     * Return is null if no primary key exists.
     */
    public String primaryKeyCondition(Object instance) {
        List<SqlField> primaryKeys = getPrimaryKeys();
        if (primaryKeys.isEmpty())
            return null;

        try {
            return "(%s) = (%s)".formatted(
                    listToString(primaryKeys, SqlField::sqlName),
                    listToString(primaryKeys, pk -> pk.toValueString(instance))
            );
        } catch (IllegalAccessException e) {
            logger.error("Couldn't access fields of valid instance. Perhaps the target class has mutated?", e);
            return null;
        }
    }

    // TODO: The 'sqlValue' method should belong to the SqlField record,
    //  but SqlField is in the same file for convenience, so it remains private

    /**
     * Converts supported values to their SQL representations
     *
     * @param value Any value inheriting a supported type
     * @return SQL representation, or null if the type is not supported
     */
    public static String sqlValue(Object value) {
        if (value instanceof Character)
            value = value.toString();

        if (value instanceof Boolean bool) {
            return bool ? "1" : "0";
        } else if (value instanceof String str) {
            return '\'' + str.replace("'", "''") + '\'';
        } else if (value instanceof Integer || value instanceof Long) {
            return value.toString();
        }
        return null;
    }

    /**
     * @return The targeted Java class to generate statements for
     */
    public Class getTargetClass() {
        return type;
    }

    /**
     * @return The current name of the Java-SQL type
     */
    public String getSqlName() {
        return typeSqlName;
    }

    /**
     * @return All supported Java-to-SQL fields of the targeted Java Class
     */
    public Iterable<SqlField> getSqlFields() {
        return fieldMap.values();
    }

    /**
     * @param sqlName SQL name of the field to retrieve
     * @return The SqlField record, if found
     */
    public SqlField getSqlField(String sqlName) {
        return fieldMap.get(sqlName);
    }

    /**
     * Converts vararg field names into a list of SQL fields
     *
     * @param sqlFieldNames SqlFields to retrieve by name. Retrieves all fields if empty.
     * @return An immutable list of fields. Returns null if a field name wasn't found.
     */
    private Iterable<SqlField> getVarargFields(String... sqlFieldNames) {
        if (sqlFieldNames.length == 0)
            return fieldMap.values();

        ArrayList<SqlField> updateList = new ArrayList<>();
        for (String sqlFieldName : sqlFieldNames) {
            SqlField found = fieldMap.get(sqlFieldName);
            if (found == null)
                return null; // Field doesn't exist

            updateList.add(found);
        }
        return updateList;
    }

    /**
     * @return An immutable list of SQL fields that are primary keys
     */
    public List<SqlField> getPrimaryKeys() {
        return fieldMap.values().stream().filter(
                f -> f.constraints().contains(SqlConstraint.PRIMARY_KEY)
        ).toList();
    }

    private interface MakeString<T> {
        String string(T object) throws IllegalAccessException;
    }

    private static <T> String listToString(Iterable<T> list, MakeString<T> makeString) throws IllegalAccessException {
        StringBuilder s = new StringBuilder();
        boolean isFirstField = true;
        for (T item : list) {
            if (!isFirstField)
                s.append(", ");
            isFirstField = false;

            s.append(makeString.string(item));
        }
        return s.toString();
    }

    private static <T> String listToString(Iterable<T> list) throws IllegalAccessException {
        return listToString(list, Object::toString);
    }
}

record SqlField(
        String sqlName,
        Field javaField,
        SqlGenerator.SqlType sqlType,
        ArrayList<SqlGenerator.SqlConstraint> constraints
) {
    public String toDeclarationString() {
        StringBuilder s = new StringBuilder(sqlName);
        s.append(' ');
        s.append(sqlType.sqlSyntax);

        for (SqlGenerator.SqlConstraint c : constraints) {
            if (c != SqlGenerator.SqlConstraint.PRIMARY_KEY) {
                s.append(' ');
                s.append(c.sqlSyntax);
            }
        }

        return s.toString();
    }

    public String toValueString(Object instance) throws IllegalAccessException {
        return SqlGenerator.sqlValue(javaField.get(instance));
    }

    public void setValue(Object instance, Object value) throws IllegalAccessException {
        if (sqlType == SqlGenerator.SqlType.BIT) {
            value = (Integer)value == 1;
        }

        javaField.set(instance, value);
    }
}
