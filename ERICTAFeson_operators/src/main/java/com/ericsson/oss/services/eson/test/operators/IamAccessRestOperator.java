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

import java.util.Arrays;
import java.util.List;

import com.ericsson.oss.services.eson.test.handlers.EccdResthandler;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.ericsson.cifwk.taf.tools.http.HttpResponse;
import com.ericsson.de.tools.cli.CliCommandResult;
import com.ericsson.de.tools.http.BasicHttpToolBuilder;
import com.ericsson.de.tools.http.impl.HttpToolImpl;
import com.ericsson.oss.services.eson.test.eccd.EccdHostGroup;
import com.ericsson.oss.services.eson.test.handlers.EccdCliHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IamAccessRestOperator implements IamAccessOperator {

    public static final String TAF_SUPER_USER = "tafsuperuser";
    public static final String TAF_READ_ONLY_USER = "tafreadonlyuser";
    private static final String REALM = (System.getenv("REALM") != null) ? System.getenv("REALM") : "master";
    private static final String OIDC_TOKEN_URL = "/auth/realms/" + REALM + "/protocol/openid-connect/token";
    private static boolean isProtocolHttps = false;
    private static final String KC_ADMIN_ID = getKcadminId();
    private static final String KC_ADMIN_PW = getKcadminPw();
    private static final String CLIENT_NAME = "ecson-external-api-access";
    private static final Logger LOGGER = LoggerFactory.getLogger(IamAccessRestOperator.class);


    private static final List<String> superUserRoles = Arrays.asList("nm-administrator",
            "flm-administrator",
            "cm-topology-administrator",
            "cm-change-administrator",
            "ret-administrator");

    private static final List<String> readOnlyUserRoles = Arrays.asList("flm-readonly",
            "cm-topology-readonly",
            "cm-change-readonly",
            "ret-readonly");
    private static String eccdLbIp = "";
    private static String hostname = "";

    /**
     * Returns the client access token.
     * @param hostname - KeyCloak host name.
     * @param eccdLbIp - Load balancer IP of cluster.
     * @param user - KeyCloak user.
     * @return the access token as a String or an empty String if token is not found.
     */
    @Override
    public String getClientAccessToken(String hostname, String eccdLbIp, String user) {
        IamAccessRestOperator.hostname = hostname;
        IamAccessRestOperator.eccdLbIp = eccdLbIp;
        HttpToolImpl tool = getHttpTool();
        HttpResponse httpResponseFromIam = tool.request()
                .header("Host", IamAccessRestOperator.hostname)
                .body("username", user)
                .body("password", user)
                .body("grant_type", "password")
                .body("client_id", CLIENT_NAME)
                .body("client_secret", ClientOperator.clientSecret)
                .body("scope", "roles profile-adp-auth")
                .post(OIDC_TOKEN_URL);
        tool.close();
        JSONParser parser = new JSONParser();
        JSONObject jsonResponse = null;
        try {
            jsonResponse = (JSONObject) parser.parse(httpResponseFromIam.getBody());
        } catch (ParseException e) {
            e.printStackTrace();
        }
        String accessToken = (String) jsonResponse.get("access_token");
        if (null == accessToken) {
            return "";
        } else {
            return (String) jsonResponse.get("access_token");
        }
    }

    /**
     * Setup TAF users in each run
     * @param eccdLbIp - Load balancer IP of cluster.
     * @param ingressHost - KeyCloak host name.
     */
    public static void setupTAFUsers(String eccdLbIp, String ingressHost) {
        IamAccessRestOperator.hostname = ingressHost;
        IamAccessRestOperator.eccdLbIp = eccdLbIp;
        decideProtocol();

        if (isUserExistsInKeyCloak(TAF_SUPER_USER)) {
            deleteExistingUserFromKeyCloak(TAF_SUPER_USER);
        }

        if(isUserExistsInKeyCloak(TAF_READ_ONLY_USER)) {
            deleteExistingUserFromKeyCloak(TAF_READ_ONLY_USER);
        }

        if (isUserExistsInKeyCloakOnEcsonRealm("kpi_exporter")) {
            deleteExistingUserFromKeyCloakOnEcsonRealm("kpi_exporter");
        }

        createUserInKeyCloak(TAF_SUPER_USER, TAF_SUPER_USER);
        assignRolesToUserInKeyCloak(TAF_SUPER_USER, superUserRoles);
        createUserInKeyCloak(TAF_READ_ONLY_USER, TAF_READ_ONLY_USER);
        assignRolesToUserInKeyCloak(TAF_READ_ONLY_USER, readOnlyUserRoles);

        createUserInKeyCloakOnEcsonRealm("kpi_exporter", getKpiExporterPassword());
    }

    private static void deleteExistingUserFromKeyCloak(String userName){
        String userId = getUserIdInKeyCloak(userName);
        String url = "/auth/admin/realms/" + REALM + "/users/" + userId;
        String token = getAdminToken();
        HttpToolImpl tool = getHttpTool();
        HttpResponse response = tool.request()
                .header("Host", hostname)
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + token)
                .delete(url);
        tool.close();
        LOGGER.info(userName + " deleted from KeyCloak.");
    }

    private static void deleteExistingUserFromKeyCloakOnEcsonRealm(String userName){
        String userId = getUserIdInKeyCloak(userName);
        String url = "/auth/admin/realms/ecson/users/" + userId;
        String token = getAdminToken();
        HttpToolImpl tool = getHttpTool();
        HttpResponse response = tool.request()
                .header("Host", hostname)
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + token)
                .delete(url);
        tool.close();
        LOGGER.info(userName + " deleted from KeyCloak.");
    }

    /**
     * Checks whether the given user exists in KeyCloak.
     * @param username - KeyCloak user's name.
     * @return true if user exists and false if user doesn't exists
     */
    public static boolean isUserExistsInKeyCloak(String username) {
        return !getUserIdInKeyCloak(username).isEmpty();
    }

    /**
     * Checks whether the given user exists in KeyCloak.
     * @param username - KeyCloak user's name.
     * @return true if user exists and false if user doesn't exists
     */
    public static boolean isUserExistsInKeyCloakOnEcsonRealm(String username) {
        return !getUserIdInKeyCloakOnEcsonRealm(username).isEmpty();
    }

    private static String getUserIdInKeyCloak(String username) {
        HttpResponse response = kcGet("/auth/admin/realms/" + REALM + "/users?username=" + username);

        JSONParser parser = new JSONParser();
        JSONArray jsonResponse = null;
        try {
            jsonResponse = (JSONArray) parser.parse(response.getBody());
        } catch (ParseException e) {
            e.printStackTrace();
        }
        if (jsonResponse.size() == 1) {
            return (String) ((JSONObject) jsonResponse.get(0)).get("id");
        } else {
            return "";
        }
    }

    private static String getUserIdInKeyCloakOnEcsonRealm(String username) {
        HttpResponse response = kcGet("/auth/admin/realms/ecson/users?username=" + username);

        JSONParser parser = new JSONParser();
        JSONArray jsonResponse = null;
        try {
            jsonResponse = (JSONArray) parser.parse(response.getBody());
        } catch (ParseException e) {
            e.printStackTrace();
        }
        if (jsonResponse.size() == 1) {
            return (String) ((JSONObject) jsonResponse.get(0)).get("id");
        } else {
            return "";
        }
    }

    private static void assignRolesToUserInKeyCloak(String username, List<String> roles) {
        HttpResponse response = kcGet("/auth/admin/realms/" + REALM + "/roles");
        JSONParser parser = new JSONParser();
        JSONArray jsonResponse = null;
        try {
            jsonResponse = (JSONArray) parser.parse(response.getBody());
        } catch (ParseException e) {
            e.printStackTrace();
        }
        String userId = getUserIdInKeyCloak(username);
        for (Object obj : jsonResponse) {
            JSONObject role = (JSONObject) obj;
            if (roles.contains(role.get("name"))) {
                String body = "[\n" +
                        "      {\n" +
                        "        \"id\": \"" + role.get("id") + "\",\n" +
                        "        \"name\":\"" + role.get("name") + "\"\n" +
                        "      }\n" +
                        "]";
                kcPost("/auth/admin/realms/" + REALM + "/users/" + userId + "/role-mappings/realm", body);
            }
        }
    }

    private static void createUserInKeyCloak(String userName, String userPw) {
        String body = "{\n" +
                "    \"username\": \"" + userName + "\",\n" +
                "    \"enabled\": true,\n" +
                "    \"credentials\": [\n" +
                "        {\n" +
                "            \"type\": \"password\",\n" +
                "            \"value\": \"" + userPw + "\"\n" +
                "        }\n" +
                "    ]\n" +
                "}";
        kcPost("/auth/admin/realms/" + REALM + "/users", body);
    }

    private static void createUserInKeyCloakOnEcsonRealm(String userName, String userPw) {
        String body = "{\n" +
                "    \"username\": \"" + userName + "\",\n" +
                "    \"enabled\": true,\n" +
                "    \"credentials\": [\n" +
                "        {\n" +
                "            \"type\": \"password\",\n" +
                "            \"value\": \"" + userPw + "\"\n" +
                "        }\n" +
                "    ]\n" +
                "}";
        kcPost("/auth/admin/realms/ecson/users", body);
    }

    private static String getKcadminId() {
        String namespace = System.getProperty("namespace", "sonom");
        String cmd = "kubectl --kubeconfig=" + DeploymentCliOperator.getKubeConfig() +
                " --namespace " + namespace +
                " get secret eric-oss-ec-son-common-access-mgmt-secret" +
                " -o jsonpath=\"{.data.kcadminid}\"" +
                " | base64 --decode";
        EccdCliHandler eccdCliHandler = new EccdCliHandler(EccdHostGroup.getEccdDrector());
        CliCommandResult helmResult = eccdCliHandler.execute(cmd);
        return helmResult.getOutput();
    }

    private static String getKcadminPw() {
        String namespace = System.getProperty("namespace", "sonom");
        String cmd = "kubectl --kubeconfig=" + DeploymentCliOperator.getKubeConfig() +
                " --namespace " + namespace +
                " get secret eric-oss-ec-son-common-access-mgmt-secret" +
                " -o jsonpath=\"{.data.kcpasswd}\"" +
                " | base64 --decode";
        EccdCliHandler eccdCliHandler = new EccdCliHandler(EccdHostGroup.getEccdDrector());
        CliCommandResult helmResult = eccdCliHandler.execute(cmd);
        return helmResult.getOutput();
    }

    private static String getKpiExporterPassword() {
        String namespace = System.getProperty("namespace", "sonom");
        String cmd = "kubectl --kubeconfig=" + DeploymentCliOperator.getKubeConfig() +
                " --namespace " + namespace +
                " get secret eric-pm-kpi-calculator-exporter-secret" +
                " -o jsonpath=\"{.data.password}\"" +
                " | base64 --decode";
        EccdCliHandler eccdCliHandler = new EccdCliHandler(EccdHostGroup.getEccdDrector());
        CliCommandResult helmResult = eccdCliHandler.execute(cmd);
        return helmResult.getOutput();
    }

    /**
     * Sends a GET request to KeyCloak.
     * @param url - URL path of KeyCloak endpoint.
     * @return the response of that GET request.
     */
    public static HttpResponse kcGet(String url) {
        String token = getAdminToken();
        HttpToolImpl tool = getHttpTool();
        HttpResponse response = tool.request()
                .header("Host", hostname)
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + token)
                .get(url);
        tool.close();
        return response;
    }

    /**
     * Sends a POST request to KeyCloak.
     * @param url - URL path of KeyCloak endpoint.
     * @param body - BODY content of the request.
     * @return the reponse of that POST request.
     */
    public static HttpResponse kcPost(String url, String body) {
        String token = getAdminToken();
        HttpToolImpl tool = getHttpTool();
        HttpResponse response = tool.request()
                .header("Host", hostname)
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + token)
                .body(body)
                .post(url);
        tool.close();
        return response;
    }

    private static String getAdminToken() {
        HttpToolImpl tool = getHttpTool();
        HttpResponse response = tool.request()
                .header("Host", hostname)
                .body("username", KC_ADMIN_ID)
                .body("password", KC_ADMIN_PW)
                .body("grant_type", "password")
                .body("client_id", "admin-cli")
                .post(OIDC_TOKEN_URL);
        tool.close();
        JSONParser parser = new JSONParser();
        JSONObject jsonResponse = null;
        try {
            jsonResponse = (JSONObject) parser.parse(response.getBody());
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return jsonResponse.get("access_token").toString();
    }

    private static void decideProtocol() {
        isProtocolHttps = false;
        HttpToolImpl tool = getHttpTool();
        HttpResponse response = tool.request()
                .header("Host", hostname)
                .body("username", KC_ADMIN_ID)
                .body("password", KC_ADMIN_PW)
                .body("grant_type", "password")
                .body("client_id", "admin-cli")
                .post(OIDC_TOKEN_URL);
        tool.close();
        if (response.getResponseCode().getCode() == 200) {
            isProtocolHttps = false;
        } else {
            isProtocolHttps = true;
        }
    }

    private static HttpToolImpl getHttpTool() {
        if (isProtocolHttps) {
            return  (HttpToolImpl) BasicHttpToolBuilder.newBuilder(eccdLbIp)
                    .withPort(443)
                    .withProtocol("https")
                    .trustSslCertificates(true)
                    .build();
        } else {
            return  (HttpToolImpl) BasicHttpToolBuilder.newBuilder(eccdLbIp)
                    .build();
        }
    }
}