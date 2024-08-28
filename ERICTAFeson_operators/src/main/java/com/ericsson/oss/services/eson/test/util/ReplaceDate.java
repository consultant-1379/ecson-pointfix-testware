/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2020 - 2022
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.eson.test.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.services.eson.test.operators.database.TestException;

public final class ReplaceDate {

    private static final String VALUE_DELIMITER = ";";
    private static final String LOCAL_TIMESTAMP = "local_timestamp";
    private static String DATE_PATTERN = "\\d{4}-\\d{2}-\\d{2}";

    private static final Logger LOGGER = LoggerFactory.getLogger(ReplaceDate.class);

    private ReplaceDate() {
    }

    /**
     * Method to replace date in where clause using the previous day's date based on the cron expression.
     * 
     * @param whereClauseColumns
     *            {@link String} Table columns for where clause
     * @param whereClauseValues
     *            {@link String} Table values for where clause
     * @param cronExpression
     *            {@link String} cron expression from which date is extracted
     * @return {@link String} Converted where clause values
     * @throws TestException
     *             throws exception if columns and values do not match
     */
    public static String replaceDateInWhereClauseValues(final String whereClauseColumns, final String whereClauseValues,
            final String cronExpression) throws TestException {
        final String[] allWhereClauseColumns = whereClauseColumns.split(VALUE_DELIMITER);
        final String[] allWhereClauseValues = whereClauseValues.split(VALUE_DELIMITER);

        validateWhereClauseAndValues(allWhereClauseColumns, allWhereClauseValues);
        return substitutedData(whereClauseValues, getPreviousDaysDateFromCronExpression(cronExpression), allWhereClauseColumns, allWhereClauseValues);
    }

    /**
     * Method to replace date in where clause values with the given date
     * 
     * @param whereClauseColumns
     *            {@link String} Table columns for where clause
     * @param whereClauseValues
     *            {@link String} Table values for where clause
     * @param date
     *            {@link String} the date to substituted in
     * @return {@link String} Converted where clause values
     * @throws TestException
     *             throws exception if columns and values do not match
     */
    public static String replaceDateInWhereClauseValuesWithGivenDate(final String whereClauseColumns, final String whereClauseValues,
            final String date) throws TestException {
        final String[] allWhereClauseColumns = whereClauseColumns.split(VALUE_DELIMITER);
        final String[] allWhereClauseValues = whereClauseValues.split(VALUE_DELIMITER);

        validateWhereClauseAndValues(allWhereClauseColumns, allWhereClauseValues);
        return substitutedDateOnly(whereClauseValues, date, allWhereClauseColumns, allWhereClauseValues);

    }

    /**
     * Returns previous days date from cron expression.
     *
     * @param cronExpression
     *            {@link String} cron expression from whcich date is extracted
     * @return {@link String} previous days date in (YYYY-MM-MM) format
     */
    public static String getPreviousDaysDateFromCronExpression(final String cronExpression) throws TestException {
        final String[] cronSplitted = cronExpression.split(" ");
        final String cronDate = cronSplitted[6] + "-" + cronSplitted[4] + "-" + cronSplitted[3];
        final SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
        try {
            final Date date = formatter.parse(cronDate);
            final Calendar cal = Calendar.getInstance();
            cal.setTime(date);
            cal.add(Calendar.DAY_OF_YEAR, -1);
            final Date previousDate = cal.getTime();
            return "'" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(previousDate) + "'";

        } catch (final ParseException e) {
            LOGGER.error(String.format("Error parsing date for cron expression %s %s.", cronExpression, e.getMessage()));
            throw new TestException(
                    String.format("Error parsing date for cron expression %s %s.", cronExpression, e.getMessage()));
        }
    }

    private static void validateWhereClauseAndValues(final String[] allWhereClauseColumns, final String[] allWhereClauseValues) throws TestException {
        if ((allWhereClauseColumns.length != allWhereClauseValues.length)) {
            throw new TestException(
                    String.format(
                            "Error parsing test data: where_clause_columns has %d values, where_clause_values has %d values, they should be equal",
                            allWhereClauseColumns.length, allWhereClauseValues.length));
        }
    }

    private static String substitutedData(final String whereClauseValues, final String date, final String[] allWhereClauseColumns,
            final String[] allWhereClauseValues) throws TestException {
        for (int i = 0; i < allWhereClauseColumns.length; i++) {
            if (allWhereClauseColumns[i].equals(LOCAL_TIMESTAMP)) {
                return whereClauseValues.replaceAll(allWhereClauseValues[i], date);
            }
        }
        return null;
    }

    private static String substitutedDateOnly(final String whereClauseValues, final String date, final String[] allWhereClauseColumns,
                                              final String[] allWhereClauseValues) {
        for (int i = 0; i < allWhereClauseColumns.length; i++) {
            if (allWhereClauseColumns[i].equals(LOCAL_TIMESTAMP)) {
                return whereClauseValues.replaceAll(DATE_PATTERN, date);
            }
        }
        return null;
    }
}
