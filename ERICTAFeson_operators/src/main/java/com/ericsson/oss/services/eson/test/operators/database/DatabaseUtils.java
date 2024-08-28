/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2018 - 2020
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/

package com.ericsson.oss.services.eson.test.operators.database;

import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class DatabaseUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseUtils.class);

    private DatabaseUtils() {

    }

    static double getTolerance(final Properties properties, final String propertyName) {
        if (!properties.containsKey(propertyName)) {
            LOGGER.warn("Properties {} does not include '{}', returning 0", properties, propertyName);
            return 0.0D;
        }

        try {
            return Double.parseDouble(properties.getProperty(propertyName));
        } catch (final Exception e) {
            LOGGER.warn("Error getting tolerance value for property '{}' from {}, returning 0", propertyName, properties, e);
            return 0.0D;
        }
    }

    static int getCount(final Statement statement, final String tableName, final String sqlCountQuery) {
        if (!tableExists(statement, tableName)) {
            LOGGER.warn("Table '{}' does not exist", tableName);
            return -1;
        }

        try (final ResultSet resultSet = statement.executeQuery(sqlCountQuery)) {
            resultSet.next();
            return resultSet.getInt("count");
        } catch (final Exception e) {
            LOGGER.warn("Error getting count for SQL query '{}'", sqlCountQuery, e);
            return -2;
        }
    }

    private static boolean tableExists(final Statement statement, final String tableName) {
        final String tableExistsQuery = String.format("SELECT COUNT(*) FROM information_schema.tables WHERE table_name = '%s'", tableName);

        try (final ResultSet resultSet = statement.executeQuery(tableExistsQuery)) {
            resultSet.next();
            final int count = resultSet.getInt("count");

            if (count != 1) {
                LOGGER.warn("Found {} instances of table '{}'", count, tableName);
                return false;
            }

            return true;
        } catch (final Exception e) {
            LOGGER.warn("Unable to check if table '{}' exists", tableName, e);
            return false;
        }
    }

    public static String getSelect(final Statement statement, final String tableName, final String sqlQuery, final String columnLabel)
            throws TestException {
        if (!tableExists(statement, tableName)) {
            LOGGER.warn("Table '{}' does not exist", tableName);
            throw new TestException(String.format(
                    "Table %s not found", tableName));
        }

        try (final ResultSet resultSet = statement.executeQuery(sqlQuery)) {
            resultSet.next();
            return resultSet.getString(columnLabel);
        } catch (final Exception e) {
            final String exception = String.format("Error executing query: %s on %s", sqlQuery, tableName);
            LOGGER.warn((exception), e);
            throw new TestException(exception);
        }
    }

    public static void resetAutoIncrementCounter(final Statement statement, final String tableName, final String incrementColumnName) {
        final String query = String.format("ALTER SEQUENCE %s_%s_seq RESTART WITH 1", tableName, incrementColumnName);
        try {
            statement.execute(query);
        } catch (final Exception e) {
            LOGGER.warn("Failed to reset Auto Increment Counter '{}' for table '{}' ", incrementColumnName, tableName, e);
        }
    }
}
