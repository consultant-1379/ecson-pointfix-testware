/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2020
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.eson.test.operators.database;

import java.io.IOException;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.services.eson.test.eccd.EccdHostGroup;
import com.ericsson.oss.services.eson.test.handlers.EccdCliHandler;
import com.ericsson.oss.services.eson.test.operators.DeploymentCliOperator;

class DbConnectionData {

    private static final int REQUIRED_DB_CONNECTION_ARGS = 4;
    private static final int DB_NAME_INDEX = 0;
    private static final int DB_USER_INDEX = 1;
    private static final int DB_PORT_INDEX = 2;
    private static final int DB_SECRET_NAME_INDEX = 3;
    private static final int POSTGRES_PORT_LENGTH = 4;
    private static final Logger LOGGER = LoggerFactory.getLogger(DbConnectionData.class);
    private static final String useAlternativeConfig = System.getProperty("useAlternativeConfig", "false");

    private static final String CONNECTION_DELIMITER = ";";
    private static final String POSTGRESQL_DRIVER = "org.postgresql.Driver";
    private static final String DB_NAME = "dbName";
    private static final String JDBC_URL = "jdbcUrl";

    private static final EccdCliHandler ECCD_CLI_HANDLER = new EccdCliHandler(EccdHostGroup.getEccdDrector());

    private final Properties jdbcProperties = new Properties();
    private final Properties connectionProperties = new Properties();

    DbConnectionData(final String dbConnectionDetails, final String ip) throws TestException {
        extractConnectionData(dbConnectionDetails, ip);
    }

    private void extractConnectionData(final String dbConnectionDetails, final String ip) throws TestException {
        final String[] dbConnection = dbConnectionDetails.split(CONNECTION_DELIMITER);

        if (REQUIRED_DB_CONNECTION_ARGS != dbConnection.length) {
            throw new TestException(String.format(
                    "Error parsing database connection details '%s', connection must have '%d' arguments in the db_connection_details column in relevant csv file comprising of 'dbName;dbUser;dbPort;dbSecretName'",
                    dbConnectionDetails, REQUIRED_DB_CONNECTION_ARGS));
        }

        final String dbName = dbConnection[DB_NAME_INDEX];
        final String dbUser = dbConnection[DB_USER_INDEX];
        String dbPort = dbConnection[DB_PORT_INDEX];
        if (dbPort.length() > POSTGRES_PORT_LENGTH && useAlternativeConfig.equals("true")) {
            try {
                final Properties props = new Properties();
                props.load(getClass().getClassLoader().getResourceAsStream("taf_properties/dbNodePortConfigAlternative.properties"));
                dbPort = props.getProperty(dbName);
            } catch (final IOException e) {
                LOGGER.warn("Failed to load node port config file dbNodePortConfigAlternative.properties");
            }
        }
        final String dbSecretName = dbConnection[DB_SECRET_NAME_INDEX];
        final String jdbcUrl = String.format("jdbc:postgresql://%s:%s/%s", ip, dbPort, dbName);
        final String dbPassword = DeploymentCliOperator.decodeSecret(dbSecretName, ECCD_CLI_HANDLER);

        jdbcProperties.setProperty("user", dbUser);
        jdbcProperties.setProperty("password", dbPassword);
        jdbcProperties.setProperty("driver", POSTGRESQL_DRIVER);

        connectionProperties.setProperty(DB_NAME, dbName);
        connectionProperties.setProperty(JDBC_URL, jdbcUrl);
    }

    String getJdbcUrl() {
        return connectionProperties.getProperty(JDBC_URL);
    }

    String getDbName() {
        return connectionProperties.getProperty(DB_NAME);
    }

    Properties getJdbcProperties() {
        return jdbcProperties;
    }
}
