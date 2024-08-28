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

import static com.ericsson.oss.services.eson.test.util.Resources.getClasspathResourceAsString;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.StringUtils;
import org.hornetq.utils.json.JSONArray;
import org.hornetq.utils.json.JSONException;
import org.hornetq.utils.json.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.cifwk.taf.tools.http.HttpResponse;
import com.ericsson.oss.services.eson.test.constants.common.DeploymentConstants;
import com.ericsson.oss.services.eson.test.handlers.EccdResthandler;

import io.netty.handler.codec.http.HttpResponseStatus;

public class CasRestOperator {
    private static final Logger LOGGER = LoggerFactory.getLogger(CasRestOperator.class);

    private static String message = "";
    private static final int SLEEP_TIME_MINUTES = 15;
    private static final int TIMEOUT_TIME_MINUTES = 10;

    private CasRestOperator() {
    }

    public static boolean checkChangePendingApproval(final String eccdLbIp, final String ingressHost, final String executionId, final String user,
            final int numChangesExpectedLowerLimit) {
        return checkChangeStatus(eccdLbIp, ingressHost, executionId, "PENDING_APPROVAL", user, numChangesExpectedLowerLimit);
    }

    public static boolean checkChangePendingApproval(final String eccdLbIp, final String ingressHost, final String executionId, final String user,
                                                     final int numChangesExpectedLowerLimit, final String changeType) {
        return checkChangeStatus(eccdLbIp, ingressHost, executionId, "PENDING_APPROVAL", user, numChangesExpectedLowerLimit, changeType);
    }

    public static boolean checkChangeSucceeded(final String eccdLbIp, final String ingressHost, final String executionId, final String user,
            final int numChangesExpectedLowerLimit) {
        return checkChangeStatus(eccdLbIp, ingressHost, executionId, "SUCCEEDED", user, numChangesExpectedLowerLimit);
    }

    public static boolean checkChangeSucceeded(final String eccdLbIp, final String ingressHost, final String executionId, final String user,
                                               final int numChangesExpectedLowerLimit, final String changeType) {
        return checkChangeStatus(eccdLbIp, ingressHost, executionId, "SUCCEEDED", user, numChangesExpectedLowerLimit, changeType);
    }

    private static boolean checkChangeStatus(final String eccdLbIp, final String ingressHost, final String executionId, final String status,
            final String user, final int numChangesExpectedLowerLimit) {
        final long start = System.currentTimeMillis();
        final long end = start + TimeUnit.MINUTES.toMillis(TIMEOUT_TIME_MINUTES);
        final String url = DeploymentConstants.GET_PUT_PROPOSED_CHANGES + "?executionId=" + executionId;
        LOGGER.info("Checking status of changes");

        while (true) {
            if (System.currentTimeMillis() > end) {
                message = String.format("Error checking if changes %s", status);
                LOGGER.error(message);
                return false;
            }

            if (EccdResthandler.executeGetCommand(url, ingressHost, eccdLbIp,
                    HttpResponseStatus.OK.code(), user)) {
                final HttpResponse response = EccdResthandler.getHttpResponse();
                try {
                    final JSONArray jsonArray = new JSONArray(response.getBody());
                    for (int i = 0; i < jsonArray.length(); i++) {
                        final JSONObject collection = jsonArray.getJSONObject(i);
                        if (status.equals(collection.getString("status"))) {
                            final int countOfChanges = StringUtils.countMatches(response.getBody(), "id");
                            LOGGER.info("Number of change elements '{}' = '{}'", status, countOfChanges);
                            if (countOfChanges > numChangesExpectedLowerLimit) {
                                return true;
                            }
                        }
                    }
                    TimeUnit.SECONDS.sleep(SLEEP_TIME_MINUTES);
                } catch (final JSONException | InterruptedException e) {
                    message = String.format("Error checking if changes '%s' '%s'.", status, e.getMessage());
                    LOGGER.error(message);
                    Thread.currentThread().interrupt();
                    return false;
                }
            }
        }
    }

    private static boolean checkChangeStatus(final String eccdLbIp, final String ingressHost, final String executionId, final String status,
            final String user, final int numChangesExpectedLowerLimit, final String changeType) {
        final long start = System.currentTimeMillis();
        final long end = start + TimeUnit.MINUTES.toMillis(TIMEOUT_TIME_MINUTES);

        final String url = DeploymentConstants.GET_PUT_PROPOSED_CHANGES +
                "?executionId=" + executionId + "&status=" + status + "&changeType=" + changeType;

        LOGGER.info("Checking status of changes");
        while (System.currentTimeMillis() < end) {
            if (EccdResthandler.executeGetCommand(url, ingressHost, eccdLbIp,
                    HttpResponseStatus.OK.code(), user)) {
                final HttpResponse response = EccdResthandler.getHttpResponse();
                try {
                    final JSONArray changeElements = new JSONArray(response.getBody());
                    final int countOfChanges = changeElements.length();
                    LOGGER.info("Number of change elements of changeType:'{}', status:'{}' = '{}'", changeType, status, countOfChanges);
                    if (countOfChanges >= numChangesExpectedLowerLimit) {
                        return true;
                    }

                    TimeUnit.SECONDS.sleep(SLEEP_TIME_MINUTES);
                } catch (final JSONException | InterruptedException e) {
                    message = String.format("Error checking if changes '%s' '%s'.", status, e.getMessage());
                    LOGGER.error(message);
                    Thread.currentThread().interrupt();
                    return false;
                }
            }
        }

        message = String.format("Timeout checking if changes %s", status);
        LOGGER.error(message);
        return false;
    }

    public static boolean updateChangeToProposed(final String eccdLbIp, final String ingressHost, final String executionId, final String user) {
        final String url = DeploymentConstants.GET_PUT_PROPOSED_CHANGES + "?executionId=" + executionId;
        return updateChanges(eccdLbIp, ingressHost, url, user);
    }

    public static boolean updateChangeToProposed(final String eccdLbIp, final String ingressHost,
                                                 final String executionId, final String user, final String changeType) {
        final String url = DeploymentConstants.GET_PUT_PROPOSED_CHANGES + "?executionId=" + executionId + "&changeType=" + changeType;
        return updateChanges(eccdLbIp, ingressHost, url, user);
    }

    private static boolean updateChanges(final String eccdLbIp, final String ingressHost, final String url, final String user) {
        if (EccdResthandler.executeGetCommand(url, ingressHost, eccdLbIp, HttpResponseStatus.OK.code(), user)) {
            final HttpResponse response = EccdResthandler.getHttpResponse();

            try {
                final JSONArray jsonArray = new JSONArray(response.getBody());
                for (int i = 0; i < jsonArray.length(); i++) {
                    jsonArray.getJSONObject(i).put("status", "PROPOSED");
                }
                EccdResthandler.executePutCommand(DeploymentConstants.GET_PUT_PROPOSED_CHANGES, ingressHost, jsonArray.toString(), eccdLbIp,
                        HttpResponseStatus.OK.code(), user);
                return true;

            } catch (final JSONException e) {
                message = String.format("Error changing status to proposed '%s'.", e.getMessage());
                LOGGER.error(message);
                return false;
            }
        }
        message = "Error changing status to proposed....";
        LOGGER.error(message);
        return false;
    }

    public static boolean postChanges(final String ingressHost, final String eccdLbIp, final String changesFile, final String user) {
        try {
            final String changes = getRequiredJsonFromChangesFileAndModify(ingressHost, eccdLbIp, changesFile, user);
            return EccdResthandler.executePostCommand(DeploymentConstants.GET_PUT_PROPOSED_CHANGES, ingressHost, changes, eccdLbIp,
                    HttpResponseStatus.CREATED.code(), user);
        } catch (final IOException | ParseException e) {
            message = String.format("Error parsing changes %s", e.getMessage());
            LOGGER.error(message);
            return false;
        }
    }

    /**
     * Returns the httpPutData.
     * 
     * @param ingressHost
     *            - Cluster host name.
     * @param eccdLbIp
     *            - Load balancer IP of the cluster.
     * @param file
     *            - CmChanges file to create the json.
     * @param user
     *            - Name of the KeyCloak user.
     * @return - the generated json.
     * @throws IOException
     *             Thrown if there is an error in getClasspathResourceAsString().
     * @throws ParseException
     *             Thrown if there is an error during parsing.
     */
    public static String getRequiredJsonFromChangesFileAndModify(final String ingressHost, final String eccdLbIp, final String file,
            final String user)
            throws IOException, ParseException {
        final int ossId = OssRepositoryRestOperator.getIdOfConfiguredOssByName(eccdLbIp, ingressHost, "stubbed-enm", user);
        final JSONParser parser = new JSONParser();
        final String json = parser.parse(getClasspathResourceAsString("data/" + file)).toString();
        return StringUtils.replace(json, "\"ossId\":1", "\"ossId\":" + ossId);
    }

    public static String getMessage() {
        return message;
    }
}
