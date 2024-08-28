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
package com.ericsson.oss.services.eson.test.operators;

import static com.ericsson.oss.services.eson.test.util.Resources.getClasspathResourceAsString;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
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
import com.ericsson.oss.services.eson.test.operators.database.TestException;
import com.ericsson.oss.services.eson.test.util.Resources;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;

import io.netty.handler.codec.http.HttpResponseStatus;

public class CmServiceRestOperator implements CmServiceOperator {

    private static final Logger LOGGER = LoggerFactory.getLogger(CmServiceRestOperator.class);
    private static final Gson GSON = new Gson();
    private static final int MAX_EMF_PER_HTTP_REQUEST = 1000;
    private static final int MAX_PTF_PER_HTTP_REQUEST = 1000;
    private static final int LIMIT = 1000; // 1000 cells per page

    private static String message = "";

    public boolean postRequiredCmElements(final String ingressHost, final String eccdLbIp, final String cmDefinitionFile, final String user) {
        return postRequiredCmData(DeploymentConstants.REQUIRED_CM_ELEMENTS, ingressHost, eccdLbIp,
                cmDefinitionFile, HttpResponseStatus.ACCEPTED.code(), user);
    }

    public boolean postCmCollection(final String ingressHost, final String eccdLbIp, final String cmCollectionDefinitionFile, final String user) {
        return postRequiredCmData(DeploymentConstants.CM_COLLECTIONS_V2, ingressHost, eccdLbIp,
                cmCollectionDefinitionFile, HttpResponseStatus.CREATED.code(), user);
    }

    public boolean getCmCollection(final String ingressHost, final String eccdLbIp, final String getOperationPath, final String expectedOutput,
            final String user) {
        return getRequiredCmCollection(DeploymentConstants.CM_COLLECTIONS_V2 + getOperationPath, ingressHost, eccdLbIp, HttpResponseStatus.OK.code(),
                expectedOutput, user);
    }

    public boolean getAllCmCollections(final String ingressHost, final String eccdLbIp, final String expectedCollectionOne,
            final String expectedCollectionTwo, final String user) {
        return getAllRequiredCmCollections(DeploymentConstants.CM_COLLECTIONS_V2, ingressHost, eccdLbIp, HttpResponseStatus.OK.code(),
                expectedCollectionOne, expectedCollectionTwo, user);

    }

    public boolean evaluateCmCollection(final String ingressHost, final String eccdLbIp, final String getOperationPath, final String user) {
        return evaluateRequiredCmCollection(DeploymentConstants.CM_COLLECTIONS_V2 + getOperationPath, ingressHost, eccdLbIp,
                HttpResponseStatus.OK.code(), user);
    }

    public boolean updateCmCollection(final String ingressHost, final String eccdLbIp, final String updateOperationPath,
            final String cmCollectionDefinitionFile, final String user) {
        return updateCmData(DeploymentConstants.CM_COLLECTIONS_V2 + updateOperationPath, ingressHost, eccdLbIp,
                cmCollectionDefinitionFile, HttpResponseStatus.OK.code(), user);
    }

    public boolean deleteCmCollection(final String ingressHost, final String eccdLbIp, final String getOperationPath, final String user) {
        return deleteCmCollection(DeploymentConstants.CM_COLLECTIONS_V2 + getOperationPath, ingressHost, eccdLbIp,
                HttpResponseStatus.OK.code(), user);
    }

    @Override
    public boolean uploadPtfData(final String eccdLbIp, final String ingressHost, final String ptfData, final String user) {
        return uploadPhysicalTopologyData(eccdLbIp, ingressHost, ptfData, user);
    }

    @Override
    public boolean uploadEmfData(final String eccdLbIp, final String ingressHost, final String emfData, final String user) {
        return uploadExternalMappingFileData(eccdLbIp, ingressHost, emfData, user);
    }

    public static boolean checkNumberOfObjectsReturned(final String eccdLbIp, final String ingressHost, final String topologyList,
            final String expectedCountList, final String user) {

        try {
            final String[] topologyObjectNames = topologyList.split(";");
            final String[] expectedTopologyObjectCounts = expectedCountList.split(";");

            LOGGER.info("topologyObjectNames : {} , expectedTopologyObjectCounts : {} ", topologyObjectNames, expectedTopologyObjectCounts);

            if ((topologyObjectNames.length != expectedTopologyObjectCounts.length)) {
                throw new TestException(
                        String.format(
                                "Error parsing test data,topologyObjectNames has %d values, expectedTopologyObjectCounts has %d values, they should be equal",
                                topologyObjectNames.length, expectedTopologyObjectCounts.length));
            }

            for (int cnt = 0; cnt < topologyObjectNames.length; cnt++) {
                LOGGER.info("topologyObjectName : {} , expectedTopologyObjectCount : {} ", topologyObjectNames[cnt],
                        expectedTopologyObjectCounts[cnt]);
                if (!checkCountForObjects(eccdLbIp, ingressHost, topologyObjectNames[cnt], Integer.parseInt(expectedTopologyObjectCounts[cnt]),
                        user)) {
                    return false;
                }
            }
        } catch (final Exception e) {
            LOGGER.error(e.getMessage());
        }
        return true;
    }

    public static String getMessage() {
        return message;
    }

    private static boolean uploadPhysicalTopologyData(final String eccdLbIp, final String ingressHost, final String ptfData, final String user) {
        final List<JSONObject> ptfFileCollection = new ArrayList<>(MAX_PTF_PER_HTTP_REQUEST);
        final JSONParser parser = new JSONParser();
        try {
            final JSONObject jsonobj = (JSONObject) parser.parse(getClasspathResourceAsString("data/" + ptfData));
            final String jsonOperation = (String) jsonobj.get("operation");
            final JSONArray jsons = (JSONArray) jsonobj.get("physicalTopologies");
            for (final Iterator<Object> iterator = jsons.iterator(); iterator.hasNext();) {
                final JSONObject o = (JSONObject) iterator.next();
                if (!iterator.hasNext() || ptfFileCollection.size() < MAX_PTF_PER_HTTP_REQUEST) {
                    ptfFileCollection.add(o);
                }

                if (!iterator.hasNext() || ptfFileCollection.size() >= MAX_PTF_PER_HTTP_REQUEST) {
                    final String httpPutData = "{\"operation\":\"" + jsonOperation + "\", \"physicalTopologies\": "
                            + GSON.toJson(ptfFileCollection) + " }";
                    if (!EccdResthandler.executePutCommand(DeploymentConstants.PHYSICAL_TOPOLOGY, ingressHost, httpPutData, eccdLbIp,
                            HttpResponseStatus.ACCEPTED.code(), user)) {
                        message = String.format("Error importing PTF Data %s.", httpPutData);
                        LOGGER.error(message);
                        return false;
                    }
                    ptfFileCollection.clear();
                }
            }
        } catch (final IOException | ParseException | IllegalArgumentException | IllegalStateException e) {
            message = String.format("Error parsing JSON Object containing PTF Data %s.", e.getMessage());
            LOGGER.error(message);
            return false;
        }
        return true;
    }

    private static boolean uploadExternalMappingFileData(final String eccdLbIp, final String ingressHost, final String emfData, final String user) {
        final List<JSONObject> emfFileCollection = new ArrayList<>(MAX_EMF_PER_HTTP_REQUEST);
        final JSONParser parser = new JSONParser();
        try {
            final int ossId = OssRepositoryRestOperator.getIdOfConfiguredOssByName(eccdLbIp, ingressHost, "stubbed-enm", user);
            final JSONObject jsonobj = (JSONObject) parser.parse(getClasspathResourceAsString("data/" + emfData));
            final JSONArray jsons = (JSONArray) jsonobj.get("externalMappings");
            for (final Iterator<Object> iterator = jsons.iterator(); iterator.hasNext();) {
                final JSONObject o = (JSONObject) iterator.next();
                o.put("cellOssId", ossId);
                o.put("retOssId", ossId);
                if (!iterator.hasNext() || emfFileCollection.size() < MAX_EMF_PER_HTTP_REQUEST) {
                    emfFileCollection.add(o);
                }
                if (!iterator.hasNext() || emfFileCollection.size() >= MAX_EMF_PER_HTTP_REQUEST) {
                    final String httpPutData = "{\"operation\":\"UPDATE\", \"externalMappings\": "
                            + GSON.toJson(emfFileCollection) + " }";
                    if (!EccdResthandler.executePutCommand(DeploymentConstants.EXTERNAL_MAPPING, ingressHost, httpPutData, eccdLbIp,
                            HttpResponseStatus.ACCEPTED.code(), user)) {
                        message = String.format("Error importing EMF Data %s.", httpPutData);
                        LOGGER.error(message);
                        return false;
                    }
                    emfFileCollection.clear();
                }
            }
        } catch (final IOException | ParseException | IllegalArgumentException | IllegalStateException e) {
            message = String.format("Error parsing JSON Object containing EMF Data %s.", e.getMessage());
            LOGGER.error(message);
            return false;
        }
        return true;
    }

    private static boolean checkCountForObjects(final String eccdLbIp, final String ingressHost, final String object, final int expectedCount,
            final String user) {
        final long start = System.currentTimeMillis();
        final long end = start + TimeUnit.MINUTES.toMillis(25);
        final int totalPages = (int) Math.ceil((double) expectedCount / LIMIT);
        final int remainingCellCountInFinalPage = (LIMIT - ((totalPages * LIMIT) - expectedCount));
        while (true) {
            if (System.currentTimeMillis() > end) {
                message = String.format("Upload of CM Topology exceeded time allowed for %s %s.", object, expectedCount);
                LOGGER.error(message);
                return false;
            }
            final JSONArray objectList = getObjectsfromTopology(eccdLbIp, ingressHost, object, LIMIT, totalPages, user);
            if (objectList != null) {
                if (objectList.size() > remainingCellCountInFinalPage) {
                    message = String.format("%s loaded in CM Topology Database exceed expected %s count %s.", object, object, expectedCount);
                    LOGGER.error(message);
                    return false;
                } else if (objectList.size() == remainingCellCountInFinalPage) {
                    return true;
                }
            } else {
                message = String.format("Error parsing JSON Object containing all %s ", object);
                LOGGER.error(message);
                return false;
            }
            try {
                Thread.sleep(100000);
            } catch (final InterruptedException e) {
                message = String.format("Error occurred during timeout of %s count check. %s", object, e.getMessage());
                LOGGER.error(message);
                Thread.currentThread().interrupt();
                return false;
            }
        }
    }

    private static JSONArray getObjectsfromTopology(final String eccdLbIp, final String ingressHost, final String object, final int limit,
            final int page, final String user) {
        JSONArray allObjects = null;
        final String query = String.format("?limit=%d&page=%d", limit, page);
        final String url = String.format(DeploymentConstants.GET_TOPOLOGY_OBJECT, object) + query;
        if (EccdResthandler.executeGetCommand(url, ingressHost, eccdLbIp, HttpResponseStatus.OK.code(), user)) {
            final HttpResponse response = EccdResthandler.getHttpResponse();
            final JSONParser parser = new JSONParser();
            try {
                final JSONObject jsonobj = (JSONObject) parser.parse(response.getBody());
                allObjects = (JSONArray) jsonobj.get("topology-objects");
            } catch (final ParseException e) {
                message = String.format("Error parsing JSON Object containing all %s %s.", object, e.getMessage());
                LOGGER.error(message);
                return null;
            }
        }
        return allObjects;
    }

    private static boolean postRequiredCmData(final String path, final String ingressHost, final String eccdLbIp,
            final String cmFile, final int expectedResponse, final String user) {
        try {
            final String requiredCm = getRequiredJsonFromFile(cmFile);
            return EccdResthandler.executePostCommand(path, ingressHost, requiredCm, eccdLbIp, expectedResponse, user);
        } catch (final IOException | ParseException e) {
            message = String.format("Error parsing required CM %s", e.getMessage());
            LOGGER.error(message);
            return false;
        }
    }

    private static boolean updateCmData(final String path, final String ingressHost, final String eccdLbIp,
            final String cmFile, final int expectedResponse, final String user) {
        final boolean isUpdateSuccess;
        try {
            final ObjectMapper mapper = new ObjectMapper();
            final String requiredCm = getRequiredJsonFromFile(cmFile);
            final JsonNode expectedJson = mapper.readTree(requiredCm);
            isUpdateSuccess = EccdResthandler.executePutCommand(path, ingressHost, requiredCm, eccdLbIp, expectedResponse, user);
            final HttpResponse response = EccdResthandler.getHttpResponse();
            final JsonNode responseJson = mapper.readTree(response.getBody());
            return (isUpdateSuccess && expectedJson.equals(responseJson));
        } catch (final IOException | ParseException e) {
            message = String.format("Error updating CM data %s", e.getMessage());
            LOGGER.error(message);
            return false;
        }
    }

    private static boolean getRequiredCmCollection(final String path, final String ingressHost, final String eccdLbIp,
            final int expectedResponse, final String expectedOutput, final String user) {
        final HttpResponse response = EccdResthandler.executeGetCommandAndReturnHttpResponse(path, ingressHost, eccdLbIp, expectedResponse, user);
        return response.getBody().contains(expectedOutput);
    }

    private static boolean getAllRequiredCmCollections(final String path, final String ingressHost, final String eccdLbIp,
            final int expectedResponse, final String expectedCollectionOne, final String expectedCollectionTwo, final String user) {
        final HttpResponse response = EccdResthandler.executeGetCommandAndReturnHttpResponse(path, ingressHost, eccdLbIp, expectedResponse, user);
        return (response.getBody().contains(expectedCollectionOne) && response.getBody().contains(expectedCollectionTwo));
    }

    private static boolean evaluateRequiredCmCollection(final String path, final String ingressHost, final String eccdLbIp,
            final int expectedResponse, final String user) {
        final HttpResponse response = EccdResthandler.executeGetCommandAndReturnHttpResponse(path, ingressHost, eccdLbIp, expectedResponse, user);
        return (!response.getBody().isEmpty() && expectedResponse == response.getResponseCode().getCode());
    }

    private static boolean deleteCmCollection(final String path, final String ingressHost, final String eccdLbIp,
            final int expectedResponse, final String user) {
        return EccdResthandler.executeDeleteCommand(path, ingressHost, eccdLbIp, expectedResponse, user);
    }

    private static String getRequiredJsonFromFile(final String file) throws IOException, ParseException {
        final JSONParser parser = new JSONParser();
        final JSONObject requiredJsonData = (JSONObject) parser.parse(Resources.getClasspathResourceAsString("algorithm_config/" + file));
        return requiredJsonData.toJSONString();
    }

}
