/*
 *------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2019 - 2021
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *------------------------------------------------------------------------------
 */

package com.ericsson.oss.services.eson.test.operators;

import java.io.IOException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.cifwk.taf.tools.http.HttpResponse;
import com.ericsson.oss.services.eson.test.constants.common.DeploymentConstants;
import com.ericsson.oss.services.eson.test.handlers.EccdResthandler;
import com.ericsson.oss.services.eson.test.util.Resources;

import io.netty.handler.codec.http.HttpResponseStatus;

public class KpiServiceRestOperator {

    private static final Logger LOGGER = LoggerFactory.getLogger(KpiServiceRestOperator.class);
    private static final String CALCULATION_FREQUENCY = "calculation_frequency";
    private static final String KPI_DEFINITIONS = "kpi_definitions";
    private static final String FILE_PATH = "algorithm_config/";

    private KpiServiceRestOperator() {
    }

    public static boolean removeCalculationFrequencyAndPutKpiDefinitions(final String ingressHost, final String eccdLbIp, final String file, final String user) {
        final JSONParser parser = new JSONParser();
        try {
            final JSONObject requiredKpis = (JSONObject) parser.parse(Resources.getClasspathResourceAsString(FILE_PATH + file));

            removeCalculationFrequency(requiredKpis);
            return putKpiDefinitions(ingressHost, eccdLbIp, requiredKpis.toJSONString(), user);
        } catch (final IOException | ParseException e) { //NOSONAR Exception suitably logged
            LOGGER.error("Error parsing JSON Object containing KPI definitions {}", e.getMessage());
        }
        return false;
    }

    public static Map<String, String> calculateKpisAndPollState(final String ingressHost, final String eccdLbIp, final String file, final String user) {
        final Map<String, String> calculationIdsBySchedule = calculateKpis(ingressHost, eccdLbIp, file, user);

        if (calculationIdsBySchedule == null || calculationIdsBySchedule.isEmpty()) {
            LOGGER.error("KPI calculation request(s) not accepted for file {}", file);
            return Collections.emptyMap();
        }

        final Map<String, String> calculationStateByCalculationFrequency = new HashMap<>();
        for (final Map.Entry<String, String> calculationIdForSchedule : calculationIdsBySchedule.entrySet()) {
            final String currentState = pollKpiCalculationState(ingressHost, eccdLbIp, user, calculationIdForSchedule.getValue());
            if ("FAILED".equals(currentState)) {
                LOGGER.error("KPI calculation for frequency '{}' failed with state - {}", calculationIdForSchedule.getKey(), currentState);
            }
            LOGGER.info("KPI calculation for frequency '{}' completed", calculationIdForSchedule.getKey());
            calculationStateByCalculationFrequency.put(calculationIdForSchedule.getKey(), currentState);
        }
        return calculationStateByCalculationFrequency;
    }

    public static String pollKpiCalculationState(final String ingressHost, final String eccdLbIp, final String user,
                                                 final String calculationId) {
        final long pollingTimeoutInSeconds = 5400;
        long waitTime = 0;
        String currentState = getCalculationState(ingressHost, eccdLbIp, calculationId, user);
        while (!"FINISHED".equals(currentState) && waitTime < pollingTimeoutInSeconds) {
            waitTime += 20;
            try {
                TimeUnit.SECONDS.sleep(20);
            } catch (final InterruptedException e) {
                LOGGER.error(String.format("Error occurred during timeout  %s", e.getMessage()));
                Thread.currentThread().interrupt();
            }
            currentState = getCalculationState(ingressHost, eccdLbIp, calculationId, user);
        }
        return currentState;
    }

    public static String calculatePreviousDayKpis(final String ingressHost, final String eccdLbIp, final String kpiCalculationRequest, final String user) {
        try {
            final String paramToken = "${param.value}";
            final String parameterizedCalculationRequest = parameterizeCalculationRequest(kpiCalculationRequest, paramToken);
            final String calculationId = postKpiCalculationRequest(ingressHost, eccdLbIp, parameterizedCalculationRequest, user);
            LOGGER.info("Calculation ID: {}", calculationId);
            return pollKpiCalculationState(ingressHost, eccdLbIp, user, calculationId);
        } catch (final IOException | ParseException e) { //NOSONAR Exception suitably logged
            LOGGER.error(String.format("Error parsing JSON Object containing KPI Data %s.", e.getMessage()));
        }
        return null;
    }

    private static void removeCalculationFrequency(final JSONObject requiredKpis) {
        final JSONArray kpiDefinitions = (JSONArray) requiredKpis.get(KPI_DEFINITIONS);
        final Iterator<JSONObject> iterator = kpiDefinitions.iterator();

        while (iterator.hasNext()) {
            final JSONObject kpiDefinition = iterator.next();
            kpiDefinition.remove(CALCULATION_FREQUENCY);
        }
    }

    private static boolean putKpiDefinitions(final String ingressHost, final String eccdLbIp, final String request, final String user) {
        return EccdResthandler.executePutCommand(DeploymentConstants.POST_PUT_KPI_DEFINITIONS, ingressHost, request, eccdLbIp,
                HttpResponseStatus.ACCEPTED.code(), user);
    }

    public static boolean postCountersDefinitions(final String ingressHost, final String eccdLbIp, final String file, final String user) {
        try {
            final String requiredCounters = getRequiredJsonFromFile(file);
            return EccdResthandler.executePostCommand(DeploymentConstants.POST_COUNTER_DEFINITIONS, ingressHost, requiredCounters, eccdLbIp,
                    HttpResponseStatus.ACCEPTED.code(), user);
        } catch (final IOException | ParseException e) { //NOSONAR Exception suitably logged
            LOGGER.error("Error parsing JSON Object containing counter definitions {}", e.getMessage());
        }
        return false;
    }

    private static String getRequiredJsonFromFile(final String file) throws IOException, ParseException {
        final JSONParser parser = new JSONParser();
        final JSONObject requiredCounters = (JSONObject) parser.parse(Resources.getClasspathResourceAsString(FILE_PATH + file));
        return requiredCounters.toJSONString();
    }

    private static Map<String, String> calculateKpis(final String ingressHost, final String eccdLbIp, final String file, final String user) {
        try {
            final Map<String, String> calculationRequests = createKpiCalculationRequests(file);
            final Map<String, String> calculationIds = new HashMap<>();
            for (final Map.Entry<String, String> calculationRequest : calculationRequests.entrySet()) {
                final String calculationId = postKpiCalculationRequest(ingressHost, eccdLbIp, calculationRequest.getValue(), user);
                if (calculationId != null) {
                    calculationIds.put(calculationRequest.getKey(), calculationId);
                }
            }
            return calculationIds;
        } catch (final IOException | ParseException e) { //NOSONAR Exception suitably logged
            LOGGER.error(String.format("Error parsing JSON Object containing KPI Data %s.", e.getMessage()));
        }
        return null;
    }

    private static String postKpiCalculationRequest(final String ingressHost, final String eccdLbIp, final String calculationRequest,
                                                    final String user) throws ParseException {

        EccdResthandler.executePostCommand(DeploymentConstants.POST_KPI_CALCULATION, ingressHost, calculationRequest, eccdLbIp,
                HttpResponseStatus.CREATED.code(), user);
        final HttpResponse response = EccdResthandler.getHttpResponse();

        LOGGER.info("KPI calculation request received HTTP status code '{}'", response.getResponseCode().getCode());
        LOGGER.info("KPI calculation request received response body '{}'", response.getBody());
        final JSONParser parser = new JSONParser();
        final JSONObject responseObj = (JSONObject) parser.parse(response.getBody());
        return (String) responseObj.get("calculationId");
    }

    private static String getCalculationState(final String ingressHost, final String eccdLbIp, final String calculationId, final String user) {
        final String url = String.format(DeploymentConstants.GET_KPI_CALCULATION_STATE, calculationId);
        if (EccdResthandler.executeGetCommand(url, ingressHost, eccdLbIp, HttpResponseStatus.ACCEPTED.code(), user)) {
            final HttpResponse response = EccdResthandler.getHttpResponse();
            final JSONParser parser = new JSONParser();
            try {
                final JSONObject responseObj = (JSONObject) parser.parse(response.getBody());
                return (String) responseObj.get("status");
            } catch (final ParseException e) { //NOSONAR Exception suitably logged
                LOGGER.error(String.format("Error parsing response JSON Object while getting the state for %s %s.", calculationId, e.getMessage()));
            }
        }
        return null;
    }

    private static Map<String, String> createKpiCalculationRequests(final String file) throws IOException, ParseException {
        final Set<String> calculationFrequencies = new HashSet<>();

        final JSONParser parser = new JSONParser();
        final JSONObject requiredKpis = (JSONObject) parser.parse(Resources.getClasspathResourceAsString(
                FILE_PATH + file));
        final JSONArray jsonArray = (JSONArray) requiredKpis.get(KPI_DEFINITIONS);
        for (final Object aJsonArray : jsonArray) {
            final JSONObject jsonObject = (JSONObject) aJsonArray;
            if (jsonObject.containsKey(CALCULATION_FREQUENCY)) {
                calculationFrequencies.add(jsonObject.get(CALCULATION_FREQUENCY).toString());
            }
        }

        final Map<String, String> calculationRequests = new HashMap<>();
        final LocalDate todayMinusOne = LocalDate.now().minusDays(1);

        for (final String calculationFrequency : calculationFrequencies) {
            final StringBuilder calculationRequest = new StringBuilder("{\"source\":\"TEST\",\"kpi_names\": [");
            for (final Object aJsonArray : jsonArray) {
                final JSONObject jsonObject = (JSONObject) aJsonArray;
                if (jsonObject.containsKey(CALCULATION_FREQUENCY) && jsonObject.get(CALCULATION_FREQUENCY).toString().equals(calculationFrequency)) {
                    calculationRequest.append("\"").append(jsonObject.get("name")).append("\",");
                }
            }
            calculationRequest.deleteCharAt(calculationRequest.length() - 1).append("],");
            calculationRequest.append("\"parameters\":{")
                    .append("\"param.start_date_time\":\"").append(Timestamp.valueOf(todayMinusOne.atTime(0, 0)).toString()).append("\",")
                    .append("\"param.end_date_time\":\"").append(Timestamp.valueOf(todayMinusOne.atTime(1, 0)).toString()).append('"')
                    .append("}}");
            calculationRequests.put(calculationFrequency, calculationRequest.toString());
        }
        return calculationRequests;
    }

    private static String parameterizeCalculationRequest(final String kpiCalculationRequest, final String paramToken)
            throws IOException, ParseException {
        final JSONParser parser = new JSONParser();
        final JSONObject calculationRequests = (JSONObject) parser.parse(Resources.getClasspathResourceAsString(FILE_PATH + kpiCalculationRequest));
        final String calculationRequestString = calculationRequests.toString();
        final LocalDate todayMinusOne = LocalDate.now().minusDays(1);

        final String calculationRequestStringWithDateTime = calculationRequestString.replace(paramToken, todayMinusOne.toString());
        LOGGER.info("Calculation request with parameter applied: {}", calculationRequestStringWithDateTime);
        return calculationRequestStringWithDateTime;
    }
}