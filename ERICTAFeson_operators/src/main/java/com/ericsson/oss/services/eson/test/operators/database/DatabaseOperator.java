/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2018 - 2022
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.eson.test.operators.database;

import static com.ericsson.oss.services.eson.test.operators.database.DatabaseUtils.getCount;
import static com.ericsson.oss.services.eson.test.operators.database.DatabaseUtils.resetAutoIncrementCounter;
import static java.lang.String.format;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Locale;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.services.eson.test.eccd.EccdHostGroup;
import com.ericsson.oss.services.eson.test.handlers.EccdCliHandler;
import com.ericsson.oss.services.eson.test.operators.DeploymentCliOperator;

public final class DatabaseOperator { // NOPMD

    private static final String KPI_CELL_GUID_60 = "kpi_cell_guid_60";

    private static final String CELL_GUID_60_KPIS = "cell_guid_60_kpis";

    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseOperator.class);

    private static final EccdCliHandler ECCD_CLI_HANDLER = new EccdCliHandler(EccdHostGroup.getEccdDrector());

    private static final StringBuilder UNSUCCESSFUL_ASSERTIONS = new StringBuilder();

    private static final String DB_ASSERTIONS_FILENAME = "DB_Assertions.txt";
    private static final String VALUE_DELIMITER = ";";
    private static final String COMMA = ",";
    private static final String EXT_NODE_IP = System.getProperty("ext_node_ip");
    private static final String RETURN_COLUMN_LABEL = "query_return";

    private static final int FROM_HOURS = 120;

    private static final int HOURS_24 = 24;

    private static final Long DB_POLL_INTERVAL = Long.valueOf(System.getProperty("db_poll_interval", "30"));

    private static String message = "";

    private static String localTimestampFromPmStats = "";

    private static int counterCsvEntries = 0;

    private DatabaseOperator() {
    }

    public static boolean resetDbTables(final String dbConnectionDetails, final String dbTables) throws TestException {
        final DbConnectionData dbConnectionData = new DbConnectionData(dbConnectionDetails, EXT_NODE_IP);
        try (final Connection connection = DriverManager.getConnection(dbConnectionData.getJdbcUrl(), dbConnectionData.getJdbcProperties())) {
            final DatabaseMetaData databaseMetaData = connection.getMetaData();
            try (final ResultSet existingTablesInDatabase = databaseMetaData.getTables(null, null, "%", new String[] { "TABLE" })) {
                final Collection<String> existingTables = new ArrayList<>();
                while (existingTablesInDatabase.next()) {
                    existingTables.add(existingTablesInDatabase.getString("table_name"));
                }
                final Collection<String> tables = new ArrayList<>(Arrays.asList(dbTables.split(VALUE_DELIMITER)));
                tables.retainAll(existingTables);
                final Statement statement = connection.createStatement();
                return truncateTables(statement, tables);
            }
        } catch (final SQLException e) {
            message = format("Failed to connect to '%s'", dbConnectionData.getDbName());
            LOGGER.error(message, e);
            return false;
        }
    }

    private static boolean truncateTables(final Statement statement, final Collection<String> tableNames) {
        try {
            final String sqlUpdate = format(
                    "TRUNCATE TABLE %s CASCADE", tableNames.toString().replace("[", "").replace("]", ""));
            LOGGER.info("Executing '{}'", sqlUpdate);
            statement.execute(sqlUpdate);
        } catch (final SQLException e) {
            message = format("Failed to truncate tables: '%s'", StringUtils.join(tableNames, ", "));
            LOGGER.error(message, e);
            return false;
        }
        return true;
    }

    public static boolean autoAlignTableDates(final String dbConnectionDetails, final String dbTables) throws TestException {
        final DbConnectionData dbConnectionData = new DbConnectionData(dbConnectionDetails, EXT_NODE_IP);
        final String[] tableNames = dbTables.split(VALUE_DELIMITER);

        try (final Connection connection = DriverManager.getConnection(dbConnectionData.getJdbcUrl(), dbConnectionData.getJdbcProperties());
                final Statement statement = connection.createStatement()) {
            return autoAlignTables(statement, tableNames);
        } catch (final SQLException e1) {
            message = format("Failed to auto align tables: '%s'", StringUtils.join(tableNames, ", "));
            LOGGER.error(e1.getMessage());
            return false;
        }
    }

    public static boolean autoAlignPmEventsTableDates(final String dbConnectionDetails, final String dbTables) throws TestException {
        if (isDaylightSavingTimeAtLA()) {
            return true;
        }
        final DbConnectionData dbConnectionData = new DbConnectionData(dbConnectionDetails, EXT_NODE_IP);
        final String[] tableNames = dbTables.split(VALUE_DELIMITER);

        try (final Connection connection = DriverManager.getConnection(dbConnectionData.getJdbcUrl(), dbConnectionData.getJdbcProperties());
                final Statement statement = connection.createStatement()) {
            return autoAlignPmEventsTables(statement, tableNames);
        } catch (final SQLException e1) {
            message = format("Failed to auto align tables: '%s'", StringUtils.join(tableNames, ", "));
            LOGGER.error(e1.getMessage());
            return false;
        }
    }

    private static boolean isDaylightSavingTimeAtLA() {
        final LocalDateTime yesterdaysDate = LocalDateTime.now().minusDays(1);
        final ZoneId zoneId = ZoneId.of("America/Los_Angeles");
        final ZonedDateTime zonedDateTime = ZonedDateTime.of(yesterdaysDate, zoneId);
        return zoneId.getRules().isDaylightSavings(zonedDateTime.toInstant());
    }

    private static boolean autoAlignTables(final Statement statement, final String[] tableNames) {
        for (final String tableName : tableNames) {
            try {
                final String sqlUpdate = format(
                        "WITH subquery AS (SELECT MAX(local_timestamp) AS maximum FROM %1$s) UPDATE %1$s SET local_timestamp = (date_trunc('day', current_timestamp at time zone 'utc')- interval '1 day') FROM subquery WHERE local_timestamp = subquery.maximum",
                        tableName);
                statement.execute(sqlUpdate);
            } catch (final SQLException e) {
                message = format("Failed to update timestamps in KPI table: %s", tableName);
                LOGGER.error(message, e);
                return false;
            }
        }
        return true;
    }

    private static boolean autoAlignPmEventsTables(final Statement statement, final String[] tableNames) {
        for (final String tableName : tableNames) {
            try {
                final String sqlUpdate = format(
                        "UPDATE %1$s SET local_timestamp = local_timestamp + '1 hour'::interval",
                        tableName);
                statement.execute(sqlUpdate);
            } catch (final SQLException e) {
                message = format("Failed to update timestamps in PmEvents table: %s", tableName);
                LOGGER.error(message, e);
                return false;
            }
        }
        return true;
    }

    public static boolean insertItems(final String dbConnectionDetails, final String dbTables, final String columns, final String values,
            final String kpiNames, final String kpiValues, final Instant UTC_TIME_STAMP, final int ossId, final String executionId)
            throws TestException {

        final DbConnectionData dbConnectionData = new DbConnectionData(dbConnectionDetails, EXT_NODE_IP);
        try {
            final Collection<String> tables = new ArrayList<>(Arrays.asList(dbTables.split(VALUE_DELIMITER)));
            final Collection<String> tableColumns = new ArrayList<>(Arrays.asList(columns.split(VALUE_DELIMITER)));
            final Collection<String> tableValues = new ArrayList<>(Arrays.asList(values.split(VALUE_DELIMITER)));
            final Collection<String> kpiNameList = new ArrayList<>(Arrays.asList(kpiNames.split(VALUE_DELIMITER)));
            final Collection<String> kpiValueList = new ArrayList<>(Arrays.asList(kpiValues.split(VALUE_DELIMITER)));

            if (tableColumns.size() != tableValues.size()) {
                throw new TestException(
                        format("Error parsing test data from %s, table columns has %d and table values has %d value, they should be equal",
                                dbConnectionData.getDbName(), tableColumns.size(), tableValues.size()));
            }

            if (kpiNameList.size() != kpiValueList.size()) {
                throw new TestException(
                        format("Error parsing test data, kpi names has %d and pki values has %d value, they should be equal",
                                kpiNameList.size(), kpiValueList.size()));
            }

            final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("'\'yyyy-MM-dd HH:mm:ss'\'").withZone(ZoneId.from(ZoneOffset.UTC));
            final String instantStr = formatter.format(UTC_TIME_STAMP);
            //Add dynamic table data here
            final int ossIdColumnIndex = ((ArrayList<String>) tableColumns).indexOf("oss_id");
            if (ossIdColumnIndex >= 0) {
                ((ArrayList<String>) tableValues).set(ossIdColumnIndex, String.valueOf(ossId));
            }
            final int executionIdIndex = ((ArrayList<String>) tableColumns).indexOf("execution_id");
            if (executionIdIndex >= 0) {
                ((ArrayList<String>) tableValues).set(executionIdIndex, "'" + executionId + "'");
            }
            tableColumns.add("local_timestamp");
            tableColumns.add("utc_timestamp");
            tableValues.add(instantStr);
            tableValues.add(instantStr);

            LOGGER.info("Inserting values into kpi db for utc_timestamp {}", instantStr);
            final boolean result = insertData(dbConnectionData, tables, tableColumns, tableValues, kpiNameList, kpiValueList);
            if (!result) {
                return false;
            }

            return true;
        } catch (final Exception e) {
            LOGGER.error(e.getMessage());
            return false;
        }
    }

    private static boolean insertData(final DbConnectionData dbConnectionData, final Collection<String> tables, final Collection<String> tableColumns,
            final Collection<String> tableValues, final Collection<String> kpiNameList, final Collection<String> kpiValueList) {
        final String jdbcUrl = dbConnectionData.getJdbcUrl();
        final Properties jdbcProperties = dbConnectionData.getJdbcProperties();
        try (final Connection connection = DriverManager.getConnection(jdbcUrl, jdbcProperties);
                final Statement statement = connection.createStatement()) {

            final String tColumns = String.join(COMMA, tableColumns);
            final String tValues = String.join(COMMA, tableValues);
            final String tKpiNames = String.join(COMMA, kpiNameList);
            final String tKpiValues = String.join(COMMA, kpiValueList);
            for (final String table : tables) {
                if (table.equals(KPI_CELL_GUID_60)) {
                    counterCsvEntries++;
                }

                final String sql = format(
                        "INSERT INTO %s (%s, %s) VALUES (%s, %s)", table, tColumns, tKpiNames, tValues, tKpiValues);
                final int result = statement.executeUpdate(sql);

                if (result != 1) {
                    return false;
                }
            }
            return true;
        } catch (final SQLException e) {
            LOGGER.error(e.getMessage());
            return false;
        }
    }

    public static boolean execute(final String dbConnectionDetails, final String dbTables,
            final String dbTableCountValues, final int pollingTimeout) throws TestException {
        return executeAndAssertDbCount(dbConnectionDetails, dbTables, dbTableCountValues, pollingTimeout, EXT_NODE_IP);
    }

    public static boolean executeCountOnView(final String dbConnectionDetails, final String dbTables,
            final String dbTableCountValues, final int pollingTimeout, final String externalViewServiceName)
            throws TestException {
        final String externalExporterLbIpAddress = DeploymentCliOperator.getExternalViewLoadBalancer(externalViewServiceName, ECCD_CLI_HANDLER);
        return executeAndAssertDbCount(dbConnectionDetails, dbTables, dbTableCountValues, pollingTimeout, externalExporterLbIpAddress);
    }

    public static void executeResetAutoIncrementCounter(final String dbConnectionDetails, final String dbTable, final String incrementColumnName)
            throws TestException {
        final DbConnectionData dbConnectionData = new DbConnectionData(dbConnectionDetails, EXT_NODE_IP);
        try (final Connection connection = DriverManager.getConnection(dbConnectionData.getJdbcUrl(), dbConnectionData.getJdbcProperties());
                final Statement statement = connection.createStatement()) {
            resetAutoIncrementCounter(statement, dbTable, incrementColumnName);
        } catch (final SQLException e) {
            LOGGER.error(e.getMessage());
        }
    }

    private static boolean executeAndAssertDbCount(final String dbConnectionDetails, final String dbTables,
            final String dbTableCountValues, final int pollingTimeout, final String ip) throws TestException {
        final DbConnectionData dbConnectionData = new DbConnectionData(dbConnectionDetails, ip);
        try {
            final String[] tableNames = dbTables.split(VALUE_DELIMITER);
            final String[] expectedCounts = dbTableCountValues.split(VALUE_DELIMITER);

            if ((tableNames.length != expectedCounts.length)) {
                throw new TestException(
                        format(
                                "Error parsing test data from %s, table names has %d values, expected counts has %d values, they should be equal",
                                dbConnectionData.getDbName(), tableNames.length, expectedCounts.length));
            }
            return getResults(pollingTimeout, dbConnectionData.getDbName(), dbConnectionData.getJdbcUrl(),
                    dbConnectionData.getJdbcProperties(), tableNames, expectedCounts);
        } catch (final Exception e) {
            LOGGER.error(e.getMessage());
        }
        return false;
    }

    private static Boolean getResults(final int pollingTimeout, final String dbName, final String jdbcUrl, final Properties jdbcProperties,
            final String[] tableNames, final String[] expectedCounts) throws TestException {
        try (final Connection connection = DriverManager.getConnection(jdbcUrl, jdbcProperties);
                final Statement statement = connection.createStatement()) {
            return assertTables(statement, dbName, tableNames, expectedCounts, pollingTimeout);
        } catch (final SQLException e) {
            LOGGER.error(e.getMessage());
        }
        return false;
    }

    private static boolean assertTables(final Statement statement, final String databaseName, final String[] tableNames,
            final String[] expectedCounts, final int pollingTimeoutValue) throws TestException {
        final TimeUnit timeoutUnit = TimeUnit.MINUTES;
        final long timeoutValueInNanoSeconds = timeoutUnit.toNanos(pollingTimeoutValue);

        for (int i = 0; i < tableNames.length; i++) {
            final String tableName = tableNames[i];
            final String sqlCountQuery = format("SELECT COUNT(*) FROM \"%s\";", tableName);

            int expectedCount = 0;
            if (tableName.equals(CELL_GUID_60_KPIS)) {
                expectedCount = Integer.parseInt(expectedCounts[i]) + counterCsvEntries;
            } else {
                expectedCount = Integer.parseInt(expectedCounts[i]);
            }

            if (!pollUntilMatchOrTimeout(statement, timeoutValueInNanoSeconds, sqlCountQuery, databaseName, tableName,
                    expectedCount)) {
                throw new TestException(
                        format("Database polling of '%s' timed out after %s %s", tableName, pollingTimeoutValue,
                                timeoutUnit.toString().toLowerCase(Locale.UK)));
            }
        }

        if (UNSUCCESSFUL_ASSERTIONS.length() != 0) {
            message = UNSUCCESSFUL_ASSERTIONS.toString();
            UNSUCCESSFUL_ASSERTIONS.setLength(UNSUCCESSFUL_ASSERTIONS.length() - 1);
            return false;
        }
        return true;
    }

    private static boolean pollUntilMatchOrTimeout(final Statement statement,
            final long timeoutValueInNanoSeconds, final String sqlCountQuery, final String dbName,
            final String tableName, final int expectedCount) {

        try (final PrintWriter printWriter = new PrintWriter(new FileWriter(DB_ASSERTIONS_FILENAME, true))) {
            final long startTime = System.nanoTime();
            int count = 0;
            final TimeUnit intervalUnit = TimeUnit.SECONDS;
            while (System.nanoTime() - startTime < timeoutValueInNanoSeconds) {
                count = getCount(statement, tableName, sqlCountQuery);
                final String intermediateCountMessage = format("'%s', ['%d'/'%d']", tableName, count, expectedCount);
                LOGGER.info(intermediateCountMessage);
                if (expectedCount == count) {
                    final String successfulAssertionsMessage = format(
                            "Database Assertion Passed: Expected count: ('%d') in '%s' database for table '%s', actual count: '%d'",
                            expectedCount, dbName, tableName, count);
                    LOGGER.info(successfulAssertionsMessage);
                    printWriter.println(successfulAssertionsMessage);
                    return true;

                }
                sleep(intervalUnit, tableName);
            }
            final String unsuccessfulAssertionsMessage = format(
                    "Database Assertion Failed: Did not find expected value ('%d') in '%s' database for table '%s' actual count: '%d'",
                    expectedCount, dbName, tableName, count);
            UNSUCCESSFUL_ASSERTIONS
                    .append(unsuccessfulAssertionsMessage)
                    .append('\n');
            LOGGER.error(unsuccessfulAssertionsMessage);
            printWriter.println(unsuccessfulAssertionsMessage);
        } catch (final IOException e) {
            LOGGER.info(e.getMessage());
        }
        return false;
    }

    private static void sleep(final TimeUnit timeUnit, final String tableName) {
        LOGGER.info("Sleeping for {} {} before checking {} table again", DB_POLL_INTERVAL, timeUnit, tableName);
        try {
            timeUnit.sleep(DB_POLL_INTERVAL);
        } catch (final InterruptedException e) {
            LOGGER.debug("Error sleeping", e);
            Thread.currentThread().interrupt();
        }
    }

    public static boolean executeAndAssertSelectOnView(final String dbConnectionDetails, final String queryDetails,
            final String whereClauseColumns, final String whereClauseValues, final String expectedValue, final int pollingTimeOut,
            final String externalViewServiceName) throws TestException {
        final String externalExporterLbIpAddress = DeploymentCliOperator.getExternalViewLoadBalancer(externalViewServiceName, ECCD_CLI_HANDLER);
        return executeAndAssertSelect(dbConnectionDetails, queryDetails, whereClauseColumns, whereClauseValues, expectedValue,
                pollingTimeOut, externalExporterLbIpAddress);
    }

    public static boolean executeAndAssertSelectOnTable(final String dbConnectionDetails, final String queryDetails,
            final String whereClauseColumns, final String whereClauseValues, final String expectedValue, final int pollingTimeOut)
            throws TestException {
        return executeAndAssertSelect(dbConnectionDetails, queryDetails, whereClauseColumns, whereClauseValues, expectedValue,
                pollingTimeOut, EXT_NODE_IP);
    }

    private static boolean executeAndAssertSelect(final String dbConnectionDetails, final String queryDetails,
            final String whereClauseColumns, final String whereClauseValues, final String expectedValue, final int pollingTimeOut, final String ip)
            throws TestException {
        final DbConnectionData dbConnectionData = new DbConnectionData(dbConnectionDetails, ip);
        final String query = generateSelectQuery(queryDetails, whereClauseColumns, whereClauseValues);
        final String[] queryInput = queryDetails.split(VALUE_DELIMITER);
        final String queryTable = queryInput[0];

        try (final Connection connection = DriverManager.getConnection(dbConnectionData.getJdbcUrl(), dbConnectionData.getJdbcProperties());
                final Statement statement = connection.createStatement()) {
            return executeQueryAndAssertTables(statement, queryTable, query, expectedValue, pollingTimeOut);
        } catch (final SQLException e) {
            LOGGER.error("Error creating db connection: ", e);
            throw new TestException("Test has failed. There was an error creating the database connection or statement");
        }
    }

    public static boolean executeQueryWithWhereClauseAndAssertTables(final String dbConnectionDetails, final String queryTable,
            final String expectedValue, final int pollingTimeOut, final String whereClause)
            throws TestException {
        final String query = format(
                "SELECT count(*) AS %s FROM %s WHERE %s", RETURN_COLUMN_LABEL, queryTable, whereClause);
        final DbConnectionData dbConnectionData = new DbConnectionData(dbConnectionDetails, EXT_NODE_IP);

        try (final Connection connection = DriverManager.getConnection(dbConnectionData.getJdbcUrl(), dbConnectionData.getJdbcProperties());
                final Statement statement = connection.createStatement()) {
            return executeQueryAndAssertTables(statement, queryTable, query, expectedValue, pollingTimeOut);
        } catch (final SQLException e) {
            LOGGER.error("Error creating db connection: ", e);
            throw new TestException("Test has failed. There was an error creating the database connection or statement");
        }
    }

    private static boolean executeQueryAndAssertTables(final Statement statement, final String table, final String query, final String expectedValue,
            final int pollingTimeOutValue) throws TestException {
        final TimeUnit timeoutUnit = TimeUnit.MINUTES;
        final long timeoutValueInNanoSeconds = timeoutUnit.toNanos(pollingTimeOutValue);

        if (!pollUntilValueMatchOrTimeout(statement, timeoutValueInNanoSeconds, query, table, expectedValue)) {
            throw new TestException(
                    format("Database polling of '%s' timed out after %s %s", table, pollingTimeOutValue,
                            timeoutUnit.toString().toLowerCase(Locale.UK)));
        }

        if (UNSUCCESSFUL_ASSERTIONS.length() != 0) {
            message = UNSUCCESSFUL_ASSERTIONS.toString();
            UNSUCCESSFUL_ASSERTIONS.setLength(UNSUCCESSFUL_ASSERTIONS.length() - 1);
            return false;
        }

        return true;
    }

    private static boolean pollUntilValueMatchOrTimeout(final Statement statement, final long timeoutValueInNanoSeconds, final String query,
            final String table, final String expectedValue) throws TestException {
        try (final PrintWriter printWriter = new PrintWriter(new FileWriter(DB_ASSERTIONS_FILENAME, true))) {
            final long startTime = System.nanoTime();
            String returnValue = "";
            final TimeUnit intervalUnit = TimeUnit.SECONDS;
            while (System.nanoTime() - startTime < timeoutValueInNanoSeconds) {
                returnValue = DatabaseUtils.getSelect(statement, table, query, RETURN_COLUMN_LABEL);
                if (expectedValue.equals(returnValue)) {
                    final String successfulAssertionsMessage = format(
                            "Database Assertion Passed: For query: '%s'. Expected Value: '%s'. Actual value: '%s'.",
                            query, expectedValue, returnValue);
                    LOGGER.info(successfulAssertionsMessage);
                    printWriter.println(successfulAssertionsMessage);
                    return true;
                }
                sleep(intervalUnit, table);
            }

            final String unsuccessfulAssertionsMessage = format(
                    "Database Assertion Failed. Did not find expected value for query: '%s'. Expected Value: '%s'. Actual value: '%s'.",
                    query, expectedValue, returnValue);
            UNSUCCESSFUL_ASSERTIONS
                    .append(unsuccessfulAssertionsMessage)
                    .append('\n');
            LOGGER.error(unsuccessfulAssertionsMessage);
            printWriter.println(unsuccessfulAssertionsMessage);
        } catch (final IOException e) {
            LOGGER.info(e.getMessage());
        }
        return false;
    }

    private static String generateSelectQuery(final String queryDetails, final String whereClauseColumns,
            final String whereClauseValues) throws TestException {

        final String[] queryInput = queryDetails.split(VALUE_DELIMITER);
        final String queryTable = queryInput[0];
        final String querySelect = queryInput[1];
        final String[] allWhereClauseColumns = whereClauseColumns.split(VALUE_DELIMITER);
        final String[] allWhereClauseValues = whereClauseValues.split(VALUE_DELIMITER);

        if ((allWhereClauseColumns.length != allWhereClauseValues.length)) {
            throw new TestException(
                    format(
                            "Error parsing test data: where_clause_columns has %d values, where_clause_values has %d values, they should be equal",
                            allWhereClauseColumns.length, allWhereClauseValues.length));
        }

        final StringBuilder whereClause = new StringBuilder();

        for (int i = 0; i < (allWhereClauseColumns.length - 1); i++) {
            final String allWhereClauseColumnsSql = allWhereClauseColumns[i] + " = " + allWhereClauseValues[i] + " and ";
            whereClause.append(allWhereClauseColumnsSql);
        }

        final String allWhereClauseColumnsSql = allWhereClauseColumns[allWhereClauseColumns.length - 1] + " = "
                + allWhereClauseValues[allWhereClauseColumns.length - 1];
        whereClause.append(allWhereClauseColumnsSql);
        return format("Select %s AS %s from %s where %s", querySelect, RETURN_COLUMN_LABEL, queryTable, whereClause);
    }

    public static String getMessage() {
        return message;
    }

    public static CharSequence getGuidFromFdnInCm(final String cm_db_connection_details, final String cmDetails) throws TestException {
        final DbConnectionData dbConnectionData = new DbConnectionData(cm_db_connection_details, EXT_NODE_IP);

        final String[] queryInput = cmDetails.split(VALUE_DELIMITER);
        final String table = queryInput[0];
        final String fdn = queryInput[1];

        final String query = format("Select id AS %s from %s where fdn = %s", RETURN_COLUMN_LABEL, table, fdn);

        try (final Connection connection = DriverManager.getConnection(dbConnectionData.getJdbcUrl(), dbConnectionData.getJdbcProperties());
                final Statement statement = connection.createStatement()) {
            return DatabaseUtils.getSelect(statement, table, query, RETURN_COLUMN_LABEL);
        } catch (final SQLException e) {
            LOGGER.error("Error creating db connection: ", e);
            throw new TestException("Test has failed. There was an error creating the database connection or statement");
        }
    }

    public static void getColumnValueFromTable(final String connection_details,
            final String table, final String column) throws TestException {
        final DbConnectionData dbConnectionData = new DbConnectionData(connection_details, EXT_NODE_IP);

        final String query = format("Select %s AS %s from %s LIMIT 1;", column, RETURN_COLUMN_LABEL, table);
        try (final Connection connection = DriverManager.getConnection(dbConnectionData.getJdbcUrl(), dbConnectionData.getJdbcProperties());
                final Statement statement = connection.createStatement()) {
            localTimestampFromPmStats = DatabaseUtils.getSelect(statement, table, query, RETURN_COLUMN_LABEL);
        } catch (final SQLException e) {
            LOGGER.error("Error creating db connection: ", e);
            throw new TestException("Test has failed. There was an error creating the database connection or statement");
        }
    }

    public static String getLocalTimestampFromPmStats() {
        return localTimestampFromPmStats;
    }

    public static void executeQuery(final String dbConnectionDetails, final String query)
            throws TestException {
        final DbConnectionData dbConnectionData = new DbConnectionData(dbConnectionDetails, EXT_NODE_IP);
        try (final Connection connection = DriverManager.getConnection(dbConnectionData.getJdbcUrl(), dbConnectionData.getJdbcProperties());
                final Statement statement = connection.createStatement()) {
            statement.execute(query);
        } catch (final SQLException e) {
            LOGGER.error("Error executing the query {}: ", query, e);
            throw new TestException("Test has failed. There was an error executing the query");
        }
    }

    public static void executeUpdate(final String dbConnectionDetails, final String sql)
            throws TestException {
        final DbConnectionData dbConnectionData = new DbConnectionData(dbConnectionDetails, EXT_NODE_IP);
        try (final Connection connection = DriverManager.getConnection(dbConnectionData.getJdbcUrl(), dbConnectionData.getJdbcProperties());
                final Statement statement = connection.createStatement()) {
            final int rowCount = statement.executeUpdate(sql);
            LOGGER.info("Update '{}' rows", rowCount);
        } catch (final SQLException e) {
            LOGGER.error("Error executing the sql {}: ", sql, e);
        }
    }

    public static void executeSelectOnPmEventsDb(final String pmDbConnectionDetails, final String pmQueryDetails,
            final String whereClauseColumns, final String whereClauseValues,
            final int timesToPollToVerify) throws TestException {

        final String[] values = whereClauseValues.split(VALUE_DELIMITER);
        final String sqlQuery = String.format("SELECT COUNT(*) AS %s FROM %s WHERE %s >= '%s' AND %s <= '%s';",
                RETURN_COLUMN_LABEL, pmQueryDetails, whereClauseColumns, values[0], whereClauseColumns, values[1]);

        String rowCount;
        int currentRowCount;
        int previousRowCount = 0;
        int counter = 0;

        LOGGER.info("PMEvents DB polling query: {}", sqlQuery);

        final DbConnectionData dbConnectionData = new DbConnectionData(pmDbConnectionDetails, EXT_NODE_IP);
        try (final Connection connection = DriverManager.getConnection(dbConnectionData.getJdbcUrl(), dbConnectionData.getJdbcProperties());
                final Statement statement = connection.createStatement()) {

            while (counter != timesToPollToVerify) {
                rowCount = DatabaseUtils.getSelect(statement, pmQueryDetails, sqlQuery, RETURN_COLUMN_LABEL);
                currentRowCount = Integer.parseInt(rowCount);
                if (currentRowCount != 0 && currentRowCount == previousRowCount) {
                    counter++;
                    TimeUnit.MINUTES.sleep(1);
                    LOGGER.info("PMEvents DB polled for {} time(s) to verify.", counter);
                } else {
                    previousRowCount = currentRowCount;
                    LOGGER.info("PMEvents DB will be polled after 3 minutes.");
                    TimeUnit.MINUTES.sleep(3);
                    counter = 0;
                }
            }

            LOGGER.info("PM Events DB loaded with {} rows.", previousRowCount);

        } catch (final SQLException | InterruptedException e) {
            LOGGER.error("Error executing the query {}: ", sqlQuery, e);
            throw new TestException("Test has failed. There was an error executing the query");
        }
    }

    public static void selectNullTimeStampsFromPMEventsDb(final String pmDbConnectionDetails, final String pmQueryDetails) throws TestException {
        final String sqlQuery = String.format("SELECT COUNT(*) AS %s FROM %s WHERE local_timestamp IS NULL;", RETURN_COLUMN_LABEL, pmQueryDetails);
        int rowCount;
        int counter = 0;
        final int maxTimesToPollDb = 5;

        LOGGER.info("PMEvents DB polling query: {}", sqlQuery);

        final DbConnectionData dbConnectionData = new DbConnectionData(pmDbConnectionDetails, EXT_NODE_IP);

        try (final Connection connection = DriverManager.getConnection(dbConnectionData.getJdbcUrl(), dbConnectionData.getJdbcProperties());
                final Statement statement = connection.createStatement()) {

            while (counter != maxTimesToPollDb) {
                rowCount = Integer.parseInt(DatabaseUtils.getSelect(statement, pmQueryDetails, sqlQuery, RETURN_COLUMN_LABEL));
                if (rowCount == 0) {
                    counter++;
                    TimeUnit.MINUTES.sleep(1);
                    LOGGER.info("No NULL local timestamps detected, PMEvents DB polled for {} time(s) to verify.", counter);
                } else {
                    LOGGER.info("PMEvents DB will be polled for NULL local timestamps after 1 minute.");
                    TimeUnit.MINUTES.sleep(1);
                    counter = 0;
                }
            }

        } catch (final SQLException | InterruptedException e) {
            LOGGER.error("Error executing the query {}: ", sqlQuery, e);
            throw new TestException("Test has failed. There was an error executing the query");
        }
    }

    public static void setLocalTimesStamps(final String db_connection_details, final String dbTables) throws TestException {
        final String[] tableNames = dbTables.split(VALUE_DELIMITER);
        LOGGER.info("Note: This will need to be changed when Daylight Saving Time occurs");
        for (final String tableName : tableNames) {
            LOGGER.info("Setting the local time to the UTC timestamp for table {}", tableName);
            final String setLocalTimeToUtc = "UPDATE " + tableName + " SET local_timestamp = utc_timestamp + INTERVAL '1' hour";
            executeQuery(db_connection_details, setLocalTimeToUtc);
        }
    }
}