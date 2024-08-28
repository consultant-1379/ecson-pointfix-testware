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
package com.ericsson.oss.services.eson.test.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public final class DateTimeUtility {

    private static final Logger LOGGER = LoggerFactory.getLogger(DateTimeUtility.class);


    private DateTimeUtility() { }

    /**
     * Converts the given date and time to given format.
     *
     * @param dateTimeInMilli
     *        Date and time in milliseconds
     * @param dateFormat
     *        Date and Time format. e.g., HH:SS or DD:MM:YY:HH:SS
     * @return
     *         Converted date in the given format
     */
    public static String convertDateTime(final long dateTimeInMilli, final String dateFormat) {
        final Date date = new Date(dateTimeInMilli);
        final DateFormat df = new SimpleDateFormat(dateFormat);
        return df.format(date);
    }

    /**
     * Returns a string for logging the start time of a test.
     * appends " start time (DD-MM-YY HH:MM) :" and then the time (in that format) to the given string
     *
     */
    public static String testStartMessage(final String testId) {
        final long time = System.currentTimeMillis();
        final String timeString = DateTimeUtility.convertDateTime(time, "dd-MM-YYYY HH:mm:ss");
        return String.format("%s start time : %s", testId, timeString);
    }

    /**
     * Returns a string for logging the start time of a test.
     * appends " end time (DD-MM-YY HH:MM) :" and then the time (in that format) to the given string
     *
     * @param testId
     * @return
     */
    public static String testEndMessage(final String testId) {
        final long time = System.currentTimeMillis();
        final String timeString = DateTimeUtility.convertDateTime(time, "dd-MM-YYYY HH:mm:ss");
        return String.format("%s end time : %s", testId, timeString);
    }

    /**
     * Returns a next days date as string in ISO format yyyy-MM-dd.
     *
     * @param date
     *        {@link String} date from which next date is required
     * @return
     *        {@link String} next days date
     */
    public static String getNextDaysDateFromDate(final String date) {
        final SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
        try {
            final Date nextDayDate = formatter.parse(date);
            final Calendar cal = Calendar.getInstance();
            cal.setTime(nextDayDate);
            cal.add(Calendar.DAY_OF_YEAR, 1);
            return formatter.format(cal.getTime());
        } catch (ParseException e) {
            LOGGER.error(String.format("Error parsing date for %s %s.", date, e.getMessage()));
        }
        return null;
    }
}
