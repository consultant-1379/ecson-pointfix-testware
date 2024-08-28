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

import org.hornetq.utils.json.JSONArray;
import org.hornetq.utils.json.JSONException;
import org.hornetq.utils.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.cifwk.taf.tools.http.HttpResponse;
import com.ericsson.oss.services.eson.test.constants.common.DeploymentConstants;
import com.ericsson.oss.services.eson.test.handlers.EccdResthandler;

import io.netty.handler.codec.http.HttpResponseStatus;

public class OssRepositoryRestOperator {

    private OssRepositoryRestOperator() {
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(OssRepositoryRestOperator.class);

    public static boolean configureOSS(final String ossPayload, final String eccdLbIp, final String ingressHost, final String user) {
        return EccdResthandler.executePostCommand(DeploymentConstants.GET_POST_DELETE_OSS, ingressHost, ossPayload, eccdLbIp,
                HttpResponseStatus.CREATED.code(), user);
    }

    public static boolean updateOSS(final String ossPayload, final String ossName, final String eccdLbIp, final String ingressHost,
            final String user) {
        int ossCreateOrUpdate = getIdOfConfiguredOssByName(eccdLbIp, ingressHost, ossName, user);
        if (ossCreateOrUpdate == -1) {
            ossCreateOrUpdate = 1;
        }
        final String ossId = "/" + ossCreateOrUpdate;
        return EccdResthandler.executePutCommand(DeploymentConstants.GET_POST_DELETE_OSS + ossId, ingressHost, ossPayload, eccdLbIp,
                HttpResponseStatus.OK.code(), user);
    }

    public static boolean checkConfiguredOSS(final String eccdLbIp, final String ingressHost, final String user) {
        if (EccdResthandler.executeGetCommand(DeploymentConstants.GET_POST_DELETE_OSS, ingressHost, eccdLbIp,
                HttpResponseStatus.OK.code(), user)) {
            final HttpResponse response = EccdResthandler.getHttpResponse();
            try {
                final JSONObject jsonObj = new JSONObject(response.getBody());
                final JSONObject collection = jsonObj.getJSONArray("collection").getJSONObject(0);
                if (!collection.getString("name").equals("stubbed-enm")) {
                    return false;
                }
            } catch (final JSONException e) {
                LOGGER.error("Error parsing JSON Object containing OSS Configuration {}", e);
                return false;
            }
            return true;
        } else {
            return false;
        }
    }

    public static boolean deleteConfiguredOSS(final String eccdLbIp, final String ingressHost, final String user) {
        final String url = DeploymentConstants.GET_POST_DELETE_OSS + "/" + getIdOfConfiguredOssByName(eccdLbIp,
                ingressHost, "stubbed-enm", user);
        return EccdResthandler.executeDeleteCommand(url, ingressHost, eccdLbIp, HttpResponseStatus.OK.code(), user);
    }

    public static int getIdOfConfiguredOssByName(final String eccdLbIp, final String ingressHost,
            final String ossName, final String user) {
        if (EccdResthandler.executeGetCommand(DeploymentConstants.GET_POST_DELETE_OSS, ingressHost, eccdLbIp,
                HttpResponseStatus.OK.code(), user)) {
            final HttpResponse response = EccdResthandler.getHttpResponse();
            try {
                final JSONObject jsonObj = new JSONObject(response.getBody());
                final JSONArray collection = jsonObj.getJSONArray("collection");
                for (int i = 0; i < collection.length(); i++) {
                    final JSONObject oss = collection.getJSONObject(i);
                    if (oss.getString("name").equals(ossName)) {
                        return oss.getInt("id");
                    }
                }
            } catch (final JSONException e) {
                LOGGER.error("Error parsing JSON Object containing OSS Configuration {}", e);
            }
        }
        return -1;
    }
}