/*
 *------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2020
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *------------------------------------------------------------------------------
 */
package com.ericsson.oss.services.eson.test.teststeps;

import static org.testng.Assert.assertTrue;

import com.ericsson.cifwk.taf.annotations.Input;
import com.ericsson.cifwk.taf.annotations.TestStep;
import com.ericsson.oss.services.eson.test.eccd.EccdHostGroup;
import com.ericsson.oss.services.eson.test.handlers.EccdCliHandler;
import com.ericsson.oss.services.eson.test.handlers.EccdResthandler;
import com.ericsson.oss.services.eson.test.operators.ApiRestOperator;
import com.ericsson.oss.services.eson.test.operators.DeploymentCliOperator;
import com.ericsson.oss.services.eson.test.operators.IamAccessRestOperator;
import com.google.inject.Inject;

public class RestTestSteps {

    private final EccdCliHandler eccdHandler = new EccdCliHandler(EccdHostGroup.getEccdDrector());

    private final String eccdLbIp = DeploymentCliOperator.getLoadbalancerIP(eccdHandler);

    private String ingressHost;

    @Inject
    private ApiRestOperator restOperator;

    @TestStep(id = StepIds.SET_UP_SERVER)
    public void setUpServerForEnv(@Input("ingressHost") final String host) {
        ingressHost = System.getProperty("ingressHost", host);
    }

    @TestStep(id = StepIds.VERIFY_REST_GET_ENDPOINTS)
    public void verifyGetRestEndPoint(@Input("url") final String url) {
        assertTrue(restOperator.isGetRestResponseCodeOK(url, eccdLbIp, ingressHost, IamAccessRestOperator.TAF_SUPER_USER), EccdResthandler.getMessage());
    }

    @TestStep(id = StepIds.VERIFY_REST_PUT_ENDPOINTS)
    public void verifyPutRestEndPoint(@Input("url") final String url, @Input("httpPutData") final String httpPutData) {
        assertTrue(restOperator.isPutRestResponseCodeBadRequest(url, ingressHost, httpPutData, eccdLbIp, IamAccessRestOperator.TAF_SUPER_USER), EccdResthandler.getMessage());
    }

    public static final class StepIds {
        public static final String SET_UP_SERVER = "setUpServer";
        public static final String VERIFY_REST_GET_ENDPOINTS = "verifyRestGetEndpoints";
        public static final String VERIFY_REST_PUT_ENDPOINTS = "verifyRestPutEndpoints";

        private StepIds() {
        }
    }
}
