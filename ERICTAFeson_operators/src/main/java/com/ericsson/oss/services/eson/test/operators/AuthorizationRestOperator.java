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

import com.ericsson.oss.services.eson.test.constants.authorization.AuthorizationConstants;
import com.ericsson.oss.services.eson.test.handlers.EccdResthandler;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;

/**
 * Class to handle the verification of user roles and accesses to services.
 */
public class AuthorizationRestOperator {
    private static final Logger LOGGER = LoggerFactory.getLogger(AuthorizationRestOperator.class);

    /**
     * Executes a PUT or GET or POST request to a service and checks whether the user has access to the resource.
     * @param serviceName - Name of the service.
     * @param ingressHost - Cluster host name.
     * @param url - Path of the service REST endpoint.
     * @param userName - Name of the KeyCloak user.
     * @param eccdLbIp - Load balancer IP of the cluster.
     * @param operationMode - User operation could be READ (GET) or WRITE (PUT).
     * @param expectedResponse - Expected response of the REST call.
     * @return the response code of the REST call.
     */
    public int verifyUserAccess(final String serviceName, final String ingressHost, final String url, final String userName, final String eccdLbIp, final String operationMode, final int expectedResponse){
        switch (operationMode){
            case AuthorizationConstants.PUT:
                EccdResthandler.executePutCommand(url, ingressHost, getHttpData(serviceName,ingressHost,eccdLbIp,AuthorizationConstants.CM_FILE, userName, operationMode), eccdLbIp, expectedResponse, userName);
                break;
            case AuthorizationConstants.GET:
                EccdResthandler.executeGetCommand(url, ingressHost, eccdLbIp, expectedResponse, userName);
                break;
            case AuthorizationConstants.POST:
                EccdResthandler.executePostCommand(url, ingressHost, getHttpData(serviceName,ingressHost,eccdLbIp,AuthorizationConstants.CM_FILE, userName, operationMode), eccdLbIp, expectedResponse, userName);
                break;
        }
        LOGGER.info("Verifying " + userName + "'s access for service: " + serviceName + ". Operation: " + operationMode);
        return EccdResthandler.getHttpResponse().getResponseCode().getCode();
    }

    private String getHttpData(final String serviceName, final String ingressHost, final String eccdLbIp, final String changesFile, final String user, final String operationMode){
        String httpData = AuthorizationConstants.EMPTY_HTTP_DATA;
        if(serviceName.equals(AuthorizationConstants.RET_SERVICE)){
            httpData = RetAlgorithmRestOperator.getConfigurationScheduledTwoMinutesFromNow(true);
        }else if(serviceName.equals(AuthorizationConstants.NM_SERVICE)){
            httpData = AuthorizationConstants.NM_SERVICE_DATA;
        }else if(serviceName.equals(AuthorizationConstants.CM_SERVICE)){
            httpData = generateCmHttpPutData(ingressHost,eccdLbIp,changesFile,user);
        }else if(serviceName.equals(AuthorizationConstants.FLM_SERVICE)){
            httpData = FlmAlgorithmRestOperator.getConfigurationScheduledTwoMinutesFromNow();
        }
        return httpData;
    }

    private String generateCmHttpPutData(final String ingressHost, final String eccdLbIp, final String changesFile, final String user){
        String data = AuthorizationConstants.EMPTY_HTTP_DATA;
        try {
            data = CasRestOperator.getRequiredJsonFromChangesFileAndModify(ingressHost, eccdLbIp, changesFile, user);
        }catch (final IOException | ParseException e) {
            LOGGER.error(String.format("Error parsing changes %s", e.getMessage()));
        }
        return data;
    }
}