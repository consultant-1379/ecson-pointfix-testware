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
package com.ericsson.oss.services.eson.test.cases.authorization;

import com.ericsson.cifwk.taf.TafTestBase;
import com.ericsson.cifwk.taf.annotations.TestId;
import com.ericsson.cifwk.taf.annotations.DataDriven;
import com.ericsson.cifwk.taf.annotations.Input;
import com.ericsson.oss.services.eson.test.constants.authorization.AuthorizationConstants;
import com.ericsson.oss.services.eson.test.eccd.EccdHostGroup;
import com.ericsson.oss.services.eson.test.handlers.EccdCliHandler;
import com.ericsson.oss.services.eson.test.operators.AuthorizationRestOperator;
import com.ericsson.oss.services.eson.test.operators.DeploymentCliOperator;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Class to verify user roles.
 */
public class VerifyUserRoles extends TafTestBase {
    private final String TESTID = "SONP-44855_VerifyUserRoles";
    private final EccdCliHandler ECCD_CLI_HANDLER = new EccdCliHandler(EccdHostGroup.getEccdDrector());
    private final String ECCD_LB_IP = DeploymentCliOperator.getLoadbalancerIP(ECCD_CLI_HANDLER);
    private AuthorizationRestOperator restOperator = new AuthorizationRestOperator();

    @TestId(id = TESTID, title = "Unauthorized user can't access resources.")
    @DataDriven(name = AuthorizationConstants.AUTHORIZATION_FILE_NAME,
                filter = "userName == 'unauthorized_user' && operationMode == 'GET'")
    @Test
    public void unauthorizedUserCantAccessResources(@Input("serviceName") final String serviceName, @Input("url") final String url, @Input("userName") final String userName, @Input("expectedResponseCode") final int expectedResponseCode, @Input("operationMode") final String operationMode) {
        Assert.assertEquals(restOperator.verifyUserAccess(serviceName,
                AuthorizationConstants.INGRESS_HOST, url, userName, ECCD_LB_IP, operationMode,
                expectedResponseCode), expectedResponseCode);
    }

    @TestId(id = TESTID, title = "TAF read-only user can call GET method")
    @DataDriven(name = AuthorizationConstants.AUTHORIZATION_FILE_NAME,
                filter = "userName == 'tafreadonlyuser' && operationMode == 'GET'")
    @Test
    public void readOnlyUserCanCallGetMethod(@Input("serviceName") final String serviceName, @Input("url") final String url, @Input("userName") final String userName, @Input("expectedResponseCode") final int expectedResponseCode, @Input("operationMode") final String operationMode) {
        Assert.assertEquals(restOperator.verifyUserAccess(serviceName, AuthorizationConstants.INGRESS_HOST, url,
                userName, ECCD_LB_IP, operationMode, expectedResponseCode), expectedResponseCode);
    }

    @TestId(id = TESTID, title = "TAF read-only user can't call PUT method.")
    @DataDriven(name = AuthorizationConstants.AUTHORIZATION_FILE_NAME,
                filter = "userName == 'tafreadonlyuser' && operationMode == 'PUT'")
    @Test
    public void readOnlyUserCantCallPutMethod(@Input("serviceName") final String serviceName, @Input("url") final String url, @Input("userName") final String userName, @Input("expectedResponseCode") final int expectedResponseCode, @Input("operationMode") final String operationMode) {
        Assert.assertEquals(restOperator.verifyUserAccess(serviceName, AuthorizationConstants.INGRESS_HOST, url,
                userName, ECCD_LB_IP, operationMode, expectedResponseCode), expectedResponseCode);
    }

    @TestId(id = TESTID, title = "TAF read-only user can't call POST method.")
    @DataDriven(name = AuthorizationConstants.AUTHORIZATION_FILE_NAME,
            filter = "userName == 'tafreadonlyuser' && operationMode == 'POST'")
    @Test
    public void readOnlyUserCantCallPostMethod(@Input("serviceName") final String serviceName, @Input("url") final String url, @Input("userName") final String userName, @Input("expectedResponseCode") final int expectedResponseCode, @Input("operationMode") final String operationMode) {
        Assert.assertEquals(restOperator.verifyUserAccess(serviceName, AuthorizationConstants.INGRESS_HOST, url,
                userName, ECCD_LB_IP, operationMode, expectedResponseCode), expectedResponseCode);
    }

    @TestId(id = TESTID, title = "TAF Super user can call GET method.")
    @DataDriven(name = AuthorizationConstants.AUTHORIZATION_FILE_NAME,
                filter = "userName == 'tafsuperuser' && operationMode == 'GET'")
    @Test
    public void superUserCanCallGetMethod(@Input("serviceName") final String serviceName, @Input("url") final String url, @Input("userName") final String userName, @Input("expectedResponseCode") final int expectedResponseCode, @Input("operationMode") final String operationMode) {
        Assert.assertEquals(restOperator.verifyUserAccess(serviceName, AuthorizationConstants.INGRESS_HOST, url,
                userName, ECCD_LB_IP, operationMode, expectedResponseCode), expectedResponseCode);
    }

    @TestId(id = TESTID, title = "TAF Super user can call PUT method.")
    @DataDriven(name = AuthorizationConstants.AUTHORIZATION_FILE_NAME,
                filter = "userName == 'tafsuperuser' && operationMode == 'PUT'")
    @Test
    public void superUserCanCallPutMethod(@Input("serviceName") final String serviceName, @Input("url") final String url, @Input("userName") final String userName, @Input("expectedResponseCode") final int expectedResponseCode, @Input("operationMode") final String operationMode) {
        Assert.assertEquals(restOperator.verifyUserAccess(serviceName, AuthorizationConstants.INGRESS_HOST, url,
                userName, ECCD_LB_IP, operationMode, expectedResponseCode), expectedResponseCode);
    }

    @TestId(id = TESTID, title = "TAF Super user can call POST method.")
    @DataDriven(name = AuthorizationConstants.AUTHORIZATION_FILE_NAME,
            filter = "userName == 'tafsuperuser' && operationMode == 'POST'")
    @Test
    public void superUserCanCallPostMethod(@Input("serviceName") final String serviceName, @Input("url") final String url, @Input("userName") final String userName, @Input("expectedResponseCode") final int expectedResponseCode, @Input("operationMode") final String operationMode) {
        Assert.assertEquals(restOperator.verifyUserAccess(serviceName, AuthorizationConstants.INGRESS_HOST, url,
                userName, ECCD_LB_IP, operationMode, expectedResponseCode), expectedResponseCode);
    }
}