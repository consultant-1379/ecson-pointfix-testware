/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2021
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.eson.test.operators;

import com.ericsson.cifwk.taf.tools.http.HttpResponse;
import com.ericsson.oss.services.eson.test.handlers.EccdResthandler;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClientOperator {
    public static String clientSecret = "";
    private static final Logger LOGGER = LoggerFactory.getLogger(ClientOperator.class);
    private static final String REALM = (System.getenv("REALM") != null) ? System.getenv("REALM") : "master";
    private static final String CLIENT_NAME = "ecson-external-api-access";
    private static final String AUDIENCE_NAME = "ecson-external-api-access-audience";

    /**
     * Initializes the client's audience mapper.
     */
    public static void initClient() {
        EccdResthandler.isAuthEnabled = false;
        HttpResponse response =
                IamAccessRestOperator.kcGet("/auth/admin/realms/" + REALM + "/clients?clientId=" + CLIENT_NAME);
        JSONParser parser = new JSONParser();
        JSONArray jsonResponse = null;
        try {
            jsonResponse = (JSONArray) parser.parse(response.getBody());
        } catch (ParseException e) {
            e.printStackTrace();
        }
        if (jsonResponse.size() != 1) {
            LOGGER.error("Client '" + CLIENT_NAME + "' not found in KeyCloak.");
        } else {
            JSONObject jsonClient = (JSONObject) jsonResponse.get(0);
            checkAudienceMapper(jsonClient);
            clientSecret = getClientSecret((String) jsonClient.get("id"));
            if (!clientSecret.isEmpty()) {
                EccdResthandler.isAuthEnabled = true;
                LOGGER.info("Authorization is enabled.");
            }
        }
    }

    private static void checkAudienceMapper(JSONObject jsonClient) {
        boolean isAudienceMapperExist = false;
        JSONArray protocolMappers = (JSONArray) (jsonClient).get("protocolMappers");
        for (Object obj : protocolMappers) {
            JSONObject protocolMapper = (JSONObject) obj;
            if (protocolMapper.get("name").equals(AUDIENCE_NAME)) {
                isAudienceMapperExist = true;
                break;
            }
        }
        if (!isAudienceMapperExist) {
            createAudienceMapper((String) jsonClient.get("id"));
        }
    }

    private static void createAudienceMapper(String clientId) {
        String createAudienceUrl = "/auth/admin/realms/" + REALM + "/clients/" + clientId + "/protocol-mappers/models";
        String body = "{\n" +
                "  \"name\":\"" + AUDIENCE_NAME + "\",\n" +
                "  \"protocol\":\"openid-connect\",\n" +
                "  \"protocolMapper\":\"oidc-audience-mapper\",\n" +
                "  \"consentRequired\":\"false\",\n" +
                "  \"config\": {\n" +
                "       \"included.client.audience\":\"" + CLIENT_NAME + "\", \n" +
                "       \"id.token.claim\":\"false\", \n" +
                "       \"access.token.claim\":\"true\" \n" +
                "       }\n" +
                "}";
        HttpResponse response = IamAccessRestOperator.kcPost(createAudienceUrl, body);
        if (!response.getResponseCode().name().equals("CREATED")) {
            LOGGER.error("Creating Audience mapper for client '" + CLIENT_NAME + "' failed.");
        }
    }

    private static String getClientSecret(String clientId) {
        String clientSecretUrl = "/auth/admin/realms/" + REALM + "/clients/" + clientId + "/client-secret";
        HttpResponse response = IamAccessRestOperator.kcGet(clientSecretUrl);
        JSONParser parser = new JSONParser();
        JSONObject jsonResponse = null;
        try {
            jsonResponse = (JSONObject) parser.parse(response.getBody());
        } catch (ParseException e) {
            e.printStackTrace();
        }
        String clientSecret = (String) jsonResponse.get("value");
        if (clientSecret.isEmpty()) {
            LOGGER.error("There is no secret for client '" + CLIENT_NAME + "'.");
            return "";
        }
        return clientSecret;
    }
}