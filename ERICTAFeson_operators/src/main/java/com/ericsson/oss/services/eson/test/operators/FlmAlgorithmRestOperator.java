/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2020 - 2021
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.eson.test.operators;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.ericsson.oss.services.eson.test.constants.common.DeploymentConstants;
import com.google.gson.Gson;
import org.hornetq.utils.json.JSONArray;
import org.hornetq.utils.json.JSONException;
import org.hornetq.utils.json.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.cifwk.taf.tools.http.HttpResponse;
import com.ericsson.oss.services.eson.test.constants.flm.FlmRestConstants;
import com.ericsson.oss.services.eson.test.handlers.EccdResthandler;
import com.ericsson.oss.services.eson.test.util.CronExpressionUtility;

import io.netty.handler.codec.http.HttpResponseStatus;

import static com.ericsson.oss.services.eson.test.util.Resources.getClasspathResourceAsString;

public class FlmAlgorithmRestOperator {

    public static final String ERROR_SCHEDULING_FLM = "Error scheduling FLM";
    public static final String CONFIG_ID_1 = "1";
    public static final int TWO_MINUTES_DELAY = 2;

    private static final Logger LOGGER = LoggerFactory.getLogger(FlmAlgorithmRestOperator.class);
    private static final int MAX_ON_DEMAND_DURATION_IN_MINUTES = 30;

    private static String message = "";
    private static String succeededExecutionId = "";
    private static String cronExpression = "";

    private FlmAlgorithmRestOperator() {
    }

    public static boolean triggerFlmExecution(final String eccdLbIp, final String ingressHost, final String user) {
        if (!EccdResthandler.executePutCommand(FlmRestConstants.PUT_CONFIGURATIONS + "/" + CONFIG_ID_1, ingressHost,
                getPAConfigAndSchedule(), eccdLbIp, HttpResponseStatus.OK.code(), user)) {
            final HttpResponse response = EccdResthandler.getHttpResponse();
            message = String.format("Scheduling of FLM failed with the following response, %s", response);
            recordAndLogErrorMessage(ERROR_SCHEDULING_FLM+ message);
            return false;
        }
        return true;
    }

    public static boolean checkFlmExecutionSucceeded(final String eccdLbIp, final String ingressHost, final String user) {
        final long start = System.currentTimeMillis();
        final long end = start + TimeUnit.MINUTES.toMillis(MAX_ON_DEMAND_DURATION_IN_MINUTES);
        final int sleepTime = 60;
        String executionId;

        while (System.currentTimeMillis() < end) {
            try {
                executionId = getSucceededExecutionId(eccdLbIp, ingressHost, user);
                if (!executionId.isEmpty()) {
                    succeededExecutionId = executionId;
                    return true;
                }
                LOGGER.info("Sleeping for {}s before retry.", sleepTime);
                TimeUnit.SECONDS.sleep(sleepTime);
            } catch (final InterruptedException e) {
                message = "Error checking checkFlmExecutionSucceeded.";
                LOGGER.error(message, e);
                Thread.currentThread().interrupt();
                return false;
            }
        }
        message = "Exceeded time allowed for checking checkFlmExecutionSucceeded.";
        LOGGER.error(message);
        return false;
    }
    public static boolean checkPAReversionElementSucceeded(final String eccdLbIp, final String ingressHost, final String user, final String sectorId) {

        String changeElementUrl= DeploymentConstants.GET_PUT_PROPOSED_CHANGES + "?executionId=" + succeededExecutionId + "&changeId=" +sectorId+ "&changeType=REVERSION" ;
        if (EccdResthandler.executeGetCommand(changeElementUrl, ingressHost, eccdLbIp, HttpResponseStatus.OK.code(), user)) {
            final HttpResponse response = EccdResthandler.getHttpResponse();
            try{
                final JSONArray jsonArray = new JSONArray(response.getBody());
                LOGGER.info("Reversion Change Element json :"+ jsonArray.getJSONObject(0).toString());
                for (int i = 0; i < jsonArray.length(); i++) {

                    if(jsonArray.getJSONObject(i).get("status").equals("SUCCEEDED")){
                        message = String.format("Found FLM execution '%s' with reversion change element state: %s", succeededExecutionId, jsonArray.getJSONObject(i).get("status"));
                        LOGGER.info(message);
                        return true;
                    }
                }
            } catch (final JSONException e) {
                message = String.format("Error processing reversion change element json '%s'.", e.getMessage());
                LOGGER.error(message);
                return false;
            }
        }
        message = String.format("Reversion Change Element for execution '%s' with state: SUCCEEDED not found", succeededExecutionId);
        return false;
    }
    /**
     * Creates the httpPutData for EccdRestHander.executePutCommand().
     *
     * @return the generated httpPutData.
     */
    public static String getConfigurationScheduledTwoMinutesFromNow() {
        cronExpression = CronExpressionUtility.generateCronExpression(TWO_MINUTES_DELAY);
        return "{\"id\":" + CONFIG_ID_1 + ", \"name\":\"default\", \"schedule\":\"" + cronExpression +
                "\", \"enabled\": true, \"openLoop\": true, \"weekendDays\": \"\", " +
                "\"customizedDefaultSettings\":{\"minRopsForAppCovReliability\":\"1\"}}";
    }

    public static String getPAConfigAndSchedule(){
        final JSONParser parser = new JSONParser();
        cronExpression = CronExpressionUtility.generateCronExpression(TWO_MINUTES_DELAY);
        try {
            final org.json.simple.JSONObject jsonobj = (org.json.simple.JSONObject) parser.parse(getClasspathResourceAsString("data/PASettings.json"));
            jsonobj.put("id",Integer.parseInt(CONFIG_ID_1)); //overriding the json id value
            jsonobj.put("schedule",cronExpression); //overriding the cron job value
            
            return jsonobj.toString();
        } catch (final IOException | ParseException | IllegalArgumentException | IllegalStateException e) {
            message = String.format("Error parsing JSON Object for PASettings %s.", e.getMessage());
            LOGGER.error(message);
            return "";
        }
    }

    public static String getMessage() {
        return message;
    }

    public static String getCronExpression() {
        return cronExpression;
    }

    public static String getSucceededExecutionId() {
        return succeededExecutionId;
    }

    private static String getSucceededExecutionId(final String eccdLbIp, final String ingressHost, final String user) {
        final String url = FlmRestConstants.GET_EXECUTIONS;
        String executionId = "";

        LOGGER.info("Looking for a succeeded execution.");
        if (EccdResthandler.executeGetCommand(url, ingressHost, eccdLbIp, HttpResponseStatus.OK.code(), user)) {
            final HttpResponse response = EccdResthandler.getHttpResponse();
            try {
                final JSONArray jsonArray = new JSONArray(response.getBody());
                if (jsonArray.length() > 0) {
                    final JSONObject collection = jsonArray.getJSONObject(0);
                    final String state = collection.getString("state");
                    if ("SUCCEEDED".equals(state)) {
                        executionId = collection.getString("id");
                    }
                    message = String.format("Found execution '%s' with state: '%s'", executionId, state);
                } else {
                    message = String.format("Found no executions, '%s' endpoint returned empty JSON response.", url);
                }
                LOGGER.info(message);
            } catch (final JSONException e) {
                message = String.format("Error parsing JSON response:%n%s", response.getBody());
                LOGGER.warn(message, e);
            }
        }
        return executionId;
    }

    private static void recordAndLogErrorMessage(final String message) {
        FlmAlgorithmRestOperator.message = message;
        LOGGER.error(FlmAlgorithmRestOperator.message);
    }

}
