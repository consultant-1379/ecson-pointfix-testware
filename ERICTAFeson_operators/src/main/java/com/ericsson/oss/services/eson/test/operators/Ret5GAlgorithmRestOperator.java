/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2022
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.eson.test.operators;

import static com.ericsson.oss.services.eson.test.constants.ret.RetRestConstants.SLEEP_TIME_TO_ALLOW_RET_TRIGGER;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.StringUtils;
import org.hornetq.utils.json.JSONArray;
import org.hornetq.utils.json.JSONException;
import org.hornetq.utils.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.cifwk.taf.tools.http.HttpResponse;
import com.ericsson.oss.services.eson.test.constants.ret.RetRestConstants;
import com.ericsson.oss.services.eson.test.handlers.EccdResthandler;
import com.ericsson.oss.services.eson.test.util.CronExpressionUtility;

import io.netty.handler.codec.http.HttpResponseStatus;

public class Ret5GAlgorithmRestOperator {

    private static final Logger LOGGER = LoggerFactory.getLogger(Ret5GAlgorithmRestOperator.class);

    private static final String STATE = "state";
    private static final String SUCCEEDED = "SUCCEEDED";
    private static final List<String> KPI_PROGRESS_EXECUTION_STATES = new ArrayList<>(
            Arrays.asList("STARTED", "KPI_PROCESSING", "OPTIMIZATION_PROCESSING", SUCCEEDED));

    private static String message = "";
    private static String cronExpression = "";

    private Ret5GAlgorithmRestOperator() {
    }

    public static boolean triggerRetExecution(final String eccdLbIp, final String ingressHost, final boolean openLoop, final String user) {
        try {
            if (EccdResthandler.executePutCommand(RetRestConstants.PUT_CONFIGURATIONS, ingressHost,
                    getConfigurationScheduledTwoMinutesFromNow(openLoop),
                    eccdLbIp,
                    HttpResponseStatus.OK.code(), user)) {
                final HttpResponse response = EccdResthandler.getHttpResponse();
                return checkStatusReturned(response);
            }
        } catch (final Exception e) {
            message = String.format("Error during execution %s", e.getMessage());
            LOGGER.error(message, e);
            Thread.currentThread().interrupt();
            return false;
        }
        return false;
    }

    private static Boolean checkStatusReturned(final HttpResponse response) {
        try {
            final JSONObject jsonObj = new JSONObject(response.getBody());
            if (jsonObj.getString("Status").equals("Success") && jsonObj.getString("Message").equals("Successfully updated configuration settings")) {
                LOGGER.info(jsonObj.getString("Message"));
                return true;
            }
        } catch (final JSONException e) {
            message = String.format("Error parsing JSON Object containing RET execution %s", e.getMessage());
            LOGGER.error(message, e);
            return false;
        }
        return false;
    }

    public static boolean checkRetExecutionSucceeded(final String eccdLbIp, final String ingressHost, final String user) {
        final long start = System.currentTimeMillis();
        final long end = start + TimeUnit.MINUTES.toMillis(25);
        final int sleepTime = 60;
        String executionId = "";

        while (System.currentTimeMillis() < end) {
            try {
                executionId = executionId.length() == 0 ? getExecutionId(eccdLbIp, ingressHost, user) : executionId;
                if (executionId.length() != 0) {
                    if (checkExecutionSucceeded(eccdLbIp, ingressHost, executionId, user)) {
                        return true;
                    }
                }
                LOGGER.info("Sleeping for {}s before retry.", sleepTime);
                TimeUnit.SECONDS.sleep(sleepTime);
            } catch (final InterruptedException e) {
                message = "Error checking checkRetExecutionSucceeded.";
                LOGGER.error(message, e);
                Thread.currentThread().interrupt();
                return false;
            }
        }
        message = "Exceeded time allowed for checking checkRetExecutionSucceeded.";
        LOGGER.error(message);
        return false;
    }

    private static String getExecutionId(final String eccdLbIp, final String ingressHost, final String user) {
        LOGGER.info("Looking for a running execution.");
        if (EccdResthandler.executeGetCommand(RetRestConstants.GET_EXECUTIONS, ingressHost, eccdLbIp, HttpResponseStatus.OK.code(), user)) {
            final HttpResponse response = EccdResthandler.getHttpResponse();
            try {
                final JSONArray jsonArray = new JSONArray(response.getBody());
                if (jsonArray.length() > 0) {
                    final JSONObject collection = jsonArray.getJSONObject(0);
                    final String state = collection.getString(STATE);
                    final String executionId = collection.getString("executionId");

                    if (!KPI_PROGRESS_EXECUTION_STATES.contains(state)) {
                        LOGGER.warn("The HttpResponse state '{}' was not one of '{}'", state, KPI_PROGRESS_EXECUTION_STATES);
                    }

                    message = String.format("Found execution '%s' with state: '%s'", executionId, state);
                    LOGGER.info(message);
                    return executionId;
                } else {
                    message = String.format("Found no executions, '%s' endpoint returned empty JSON response.", RetRestConstants.GET_EXECUTIONS);
                    LOGGER.info(message);
                }
            } catch (final JSONException e) {
                message = String.format("Error parsing JSON response: %n%s", response.getBody());
                LOGGER.warn(message, e);
            }
        }
        return StringUtils.EMPTY;
    }

    private static boolean checkExecutionSucceeded(final String eccdLbIp, final String ingressHost, final String executionId, final String user) {
        final String url = RetRestConstants.GET_EXECUTIONS + "/" + executionId;

        LOGGER.info("Checking if execution with id '{}' has succeeded.", executionId);
        if (EccdResthandler.executeGetCommand(url, ingressHost, eccdLbIp, HttpResponseStatus.OK.code(), user)) {
            final HttpResponse response = EccdResthandler.getHttpResponse();
            try {
                final JSONObject collection = new JSONObject(response.getBody());
                final String state = collection.getString(STATE);

                message = String.format("Execution's '%s' is in state '%s'.", executionId, state);
                LOGGER.info(message);

                if (SUCCEEDED.equals(state)) {
                    return true;
                }
            } catch (final JSONException e) {
                message = String.format("Error parsing JSON response: %n%s", response.getBody());
                LOGGER.warn(message, e);
            }
        }
        return false;
    }

    /**
     * Creates the httpPutData for EccdRestHander.executePutCommand().
     *
     * @param openLoop
     *            whether or not to run in openLoop mode
     * @return the generated httpPutData.
     */
    private static String getConfigurationScheduledTwoMinutesFromNow(final boolean openLoop) {
        cronExpression = CronExpressionUtility.generateCronExpression(SLEEP_TIME_TO_ALLOW_RET_TRIGGER);
        return "{\"configurationId\":1, \"configurationSettings\":{ \"cronExpression\": \"" + cronExpression
                + "\",\"openLoop\": " + openLoop + " },\"retSettings\":{ \"weekendDays\": \"\"}}";
    }

    public static String getMessage() {
        return message;
    }

    public static String getCronExpression() {
        return cronExpression;
    }

}
