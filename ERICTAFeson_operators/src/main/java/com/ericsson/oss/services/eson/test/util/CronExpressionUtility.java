/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2019
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
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CronExpressionUtility {

    public static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";
    private final Date date;
    private final Calendar cal;
    private static final String SECONDS = "0";
    private static final String DAYS_OF_WEEK = "?";
    private String daysOfMonth;

    private String minutes;
    private String hours;

    private String months;
    private String years;
    private static final Logger logger = LoggerFactory.getLogger(CronExpressionUtility.class);

    public CronExpressionUtility(final Date date) {
        this.date = date;
        cal = Calendar.getInstance();
        generateCronExpression();
    }

    private void generateCronExpression() {
        cal.setTime(date);

        hours = String.valueOf(cal.get(Calendar.HOUR_OF_DAY));

        minutes = String.valueOf(cal.get(Calendar.MINUTE));

        daysOfMonth = String.valueOf(cal.get(Calendar.DAY_OF_MONTH));

        months = new java.text.SimpleDateFormat("MM").format(cal.getTime());

        years = String.valueOf(cal.get(Calendar.YEAR));
    }

    public Date getDate() {
        return date;
    }

    public String getSeconds() {
        return SECONDS;
    }

    public String getMinutes() {
        return minutes;
    }

    public String getDaysOfWeek() {
        return DAYS_OF_WEEK;
    }

    public String getHours() {
        return hours;
    }

    public String getDaysOfMonth() {
        return daysOfMonth;
    }

    public String getMonths() {
        return months;
    }

    public String getYears() {
        return years;
    }

    public static String generateCronExpression(final int delay) {

        final long ONE_MINUTE_IN_MILLIS = 60000;//millisecs

        final Calendar date = Calendar.getInstance();
        final long t = date.getTimeInMillis();
        final Date dateWithDelay = new Date(t + (delay * ONE_MINUTE_IN_MILLIS));
        try {
            final SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);
            dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
            final String cdate = dateFormat.format(dateWithDelay);

            final Date cronDate = new SimpleDateFormat(DATE_FORMAT).parse(cdate);

            final CronExpressionUtility cronExpressionUtility = new CronExpressionUtility(cronDate);
            final StringBuilder cronExpression = new StringBuilder();
            cronExpression.append(cronExpressionUtility.getSeconds())
                    .append(" ")
                    .append(cronExpressionUtility.getMinutes())
                    .append(" ")
                    .append(cronExpressionUtility.getHours())
                    .append(" ")
                    .append(cronExpressionUtility.getDaysOfMonth())
                    .append(" ")
                    .append(cronExpressionUtility.getMonths())
                    .append(" ")
                    .append(cronExpressionUtility.getDaysOfWeek())
                    .append(" ")
                    .append(cronExpressionUtility.getYears());
            logger.debug("Cron Expression {}", cronExpression);
            return cronExpression.toString();
        } catch (final ParseException e) {
            logger.debug("Error getting Cron Expression {}", e.getMessage());
        }
        return "";
    }

}