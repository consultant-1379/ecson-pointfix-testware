/*
 *------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2021
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

import com.ericsson.cifwk.taf.annotations.TestId;
import com.ericsson.oss.services.eson.test.operators.IamAccessRestOperator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.Test;

import com.ericsson.oss.services.eson.test.eccd.EccdHostGroup;
import com.ericsson.oss.services.eson.test.handlers.EccdCliHandler;
import com.ericsson.oss.services.eson.test.operators.DeploymentCliOperator;

public class VerifyTAFUsersTestSteps {
    private final EccdCliHandler ECCD_HANDLER = new EccdCliHandler(EccdHostGroup.getEccdDrector());
    private final String ECCD_LB_IP = DeploymentCliOperator.getLoadbalancerIP(ECCD_HANDLER);
    private final String INGRESS_HOST = "cd-server.sonom.com";
    private static final String TEST_ID = "SONP-44612_Create and update TAF for Auth";
    private final Logger LOGGER = LoggerFactory.getLogger(getClass());

    @TestId(id = TEST_ID, title = "Create and verify TAF users.")
    @Test(groups = { "KGB", "eSON" })
    public void verifyTAFUsersCreation() {
        IamAccessRestOperator.setupTAFUsers(ECCD_LB_IP, INGRESS_HOST);
        LOGGER.info("Verify that '" + IamAccessRestOperator.TAF_SUPER_USER + "' exists");
        Assert.assertTrue(IamAccessRestOperator.isUserExistsInKeyCloak(IamAccessRestOperator.TAF_SUPER_USER));

        LOGGER.info("Verify that '" + IamAccessRestOperator.TAF_READ_ONLY_USER + "' exists");
        Assert.assertTrue(IamAccessRestOperator.isUserExistsInKeyCloak(IamAccessRestOperator.TAF_READ_ONLY_USER));
    }
}