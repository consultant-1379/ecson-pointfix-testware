/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2018 - 2021
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

import java.util.concurrent.TimeUnit;

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

public class RetAlgorithmRestOperator {

    public static final String ERROR_CHECKING_NUM_OPTIMIZATION_ELEMENT_GROUPS = "Error checking NumOptimizationElementGroups";
    private static final Logger LOGGER = LoggerFactory.getLogger(RetAlgorithmRestOperator.class);

    private static String message = "";
    private static String succeededExecutionId = "";
    private static String cronExpression = "";

    private RetAlgorithmRestOperator() {
    }

    public static boolean triggerRetExecution(final String eccdLbIp, final String ingressHost, final boolean openLoop
            , String user) {
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
        recordAndLogErrorMessage(ERROR_CHECKING_NUM_OPTIMIZATION_ELEMENT_GROUPS);
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
        recordAndLogErrorMessage(ERROR_CHECKING_NUM_OPTIMIZATION_ELEMENT_GROUPS);
        return false;
    }

    private static void recordAndLogErrorMessage(final String message) {
        RetAlgorithmRestOperator.message = String.format(message);
        LOGGER.error(RetAlgorithmRestOperator.message);
    }

    public static boolean checkRetExecutionSucceeded(final String eccdLbIp, final String ingressHost, String user) {
        final long start = System.currentTimeMillis();
        final long end = start + TimeUnit.MINUTES.toMillis(25);
        final int sleepTime = 60;
        final int limit = 1600;
        String executionId = "";
        int numOptimizationElementsReceived = 0;

        while (System.currentTimeMillis() < end) {
            try {
                executionId = executionId.length() == 0 ? checkExecutionStarted(eccdLbIp, ingressHost, user) : executionId;
                if (executionId.length() != 0) {
                    numOptimizationElementsReceived = numOptimizationElementsReceived <= limit ?
                            checkRetNumOptimizationElementsReceived(eccdLbIp, ingressHost, executionId, user) : numOptimizationElementsReceived;
                    if (numOptimizationElementsReceived > limit && checkExecutionSucceeded(eccdLbIp, ingressHost, executionId, user)) {
                        succeededExecutionId = executionId;
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

    private static String checkExecutionStarted(final String eccdLbIp, final String ingressHost, String user) {
        final String url = RetRestConstants.GET_EXECUTIONS;
        String executionId = "";

        LOGGER.info("Looking for a running execution.");
        if (EccdResthandler.executeGetCommand(url, ingressHost, eccdLbIp, HttpResponseStatus.OK.code(), user)) {
            final HttpResponse response = EccdResthandler.getHttpResponse();
            try {
                final JSONArray jsonArray = new JSONArray(response.getBody());
                if (jsonArray.length() > 0) {
                    final JSONObject collection = jsonArray.getJSONObject(0);
                    String state = collection.getString("state");
                    if ("STARTED".equals(state) || "KPI_PROCESSING".equals(state) || "OPTIMIZATION_PROCESSING".equals(state)) {
                        executionId = collection.getString("executionId");
                    }
                    message = String.format("Found execution '%s' with state: '%s'", executionId, state);
                    LOGGER.info(message);
                } else {
                    message = String.format("Found no executions, '%s' endpoint returned empty JSON response.", url);
                    LOGGER.info(message);
                }
            } catch (final JSONException e) {
                message = String.format("Error parsing JSON response:%n%s", response.getBody());
                LOGGER.warn(message, e);
            }
        }
        return executionId;
    }

    private static int checkRetNumOptimizationElementsReceived(final String eccdLbIp, final String ingressHost, final String executionId, String user) {
        final String url = RetRestConstants.GET_EXECUTIONS + "/" + executionId;
        int numOptimizationElementsReceived = 0;

        LOGGER.info("Checking numOptimizationElementsReceived for execution '{}'", executionId);
        if (EccdResthandler.executeGetCommand(url, ingressHost, eccdLbIp, HttpResponseStatus.OK.code(), user)) {
            final HttpResponse response = EccdResthandler.getHttpResponse();
            try {
                final JSONObject collection = new JSONObject(response.getBody());
                numOptimizationElementsReceived = collection.getInt("numOptimizationElementsReceived");
                message = String.format("Received '%d' numOptimizationElements.", numOptimizationElementsReceived);
                LOGGER.info(message);
            } catch (final JSONException e) {
                message = "Error checking numOptimizationElementsReceived";
                LOGGER.warn(message, e);
            }
        }
        return numOptimizationElementsReceived;
    }

    private static boolean checkExecutionSucceeded(final String eccdLbIp, final String ingressHost, final String executionId, String user) {
        final String url = RetRestConstants.GET_EXECUTIONS + "/" + executionId;
        boolean succeeded = false;

        LOGGER.info("Checking if execution with id '{}' has succeeded.", executionId);
        if (EccdResthandler.executeGetCommand(url, ingressHost, eccdLbIp, HttpResponseStatus.OK.code(), user)) {
            final HttpResponse response = EccdResthandler.getHttpResponse();
            try {
                final JSONObject collection = new JSONObject(response.getBody());
                String state = collection.getString("state");
                if ("SUCCEEDED".equals(state)) {
                    succeeded = true;
                }
                message = String.format("Execution's '%s' is in state '%s'.", executionId, state);
                LOGGER.info(message);
            } catch (final JSONException e) {
                message = String.format("Error parsing JSON response:%n%s", response.getBody());
                LOGGER.warn(message, e);
            }
        }
        return succeeded;
    }

    /**
     * Creates the httpPutData for EccdRestHander.executePutCommand().
     * @param openLoop
     * @return the generated httpPutData.
     */
    public static String getConfigurationScheduledTwoMinutesFromNow(final boolean openLoop) {
        cronExpression = CronExpressionUtility.generateCronExpression(SLEEP_TIME_TO_ALLOW_RET_TRIGGER);
        return "{\"configurationId\":1, \"configurationSettings\":{ \"cronExpression\": \"" + cronExpression
                + "\",\"openLoop\": " + openLoop + " },\"retSettings\":{ \"weekendDays\": \"\"}}";
    }

    public static String getMessage() {
        return message;
    }

    public static String getSucceededExecutionId() {
        return succeededExecutionId;
    }

    public static void clearSucceededExecutionId() {
        succeededExecutionId = "";
    }

    public static String getCronExpression() {
        return cronExpression;
    }

}
