package org.elshift.db;

import org.elshift.modules.impl.DownloadModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.ResultSet;
import java.sql.SQLException;

public record SqlObjectReader<T>(ResultSet sqlResult, SqlGenerator typeGen) {
    private static final Logger logger = LoggerFactory.getLogger(DownloadModule.class);

    /**
     * Reads values from {@code sqlResult} and outputs them to the Java object {@code output}
     *
     * @param output Instance whose values will be assigned to the retrieved SQL values
     * @return The output object with its values updated to reflect the retrieved SQL values.
     * Returns null on failure to access expected SQL results or Java object fields
     */
    public T readObject(T output) {
        return readObject(sqlResult, typeGen, output);
    }

    /**
     * Reads values from {@code sqlResult} and outputs them to the Java object {@code output}
     *
     * @param sqlResult SQL result, positioned over the desired row
     * @param typeGen   SQL generator for class {@code T}
     * @param output    Instance whose values will be assigned to the retrieved SQL values
     * @return The output object with its values updated to reflect the retrieved SQL values.
     * Returns null on failure to access expected SQL results or Java object fields
     */
    public static <T> T readObject(ResultSet sqlResult, SqlGenerator typeGen, T output) {
        try {
            for (SqlField f : typeGen.getSqlFields())
                f.setValue(output, sqlResult.getObject(f.sqlName()));
        } catch (IllegalAccessException | SQLException | NullPointerException e) {
            logger.error("Failed to read SQL ResultSet values into a Java object", e);
            return null;
        }
        return output;
    }
}
