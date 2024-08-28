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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import com.ericsson.cifwk.taf.annotations.Input;
import com.ericsson.cifwk.taf.annotations.TestStep;
import com.ericsson.oss.services.eson.test.eccd.EccdHostGroup;
import com.ericsson.oss.services.eson.test.handlers.EccdCliHandler;
import com.ericsson.oss.services.eson.test.operators.DeploymentCliOperator;
import com.google.inject.Inject;

public class VerifyInstallationTestSteps {

    private final EccdCliHandler eccdHandler = new EccdCliHandler(EccdHostGroup.getEccdDrector());

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Inject
    private DeploymentCliOperator deploymentOperator;

    @TestStep(id = StepIds.VERIFY_SUCCESSFUL_INSTALLATION)
    @Test(groups = { "KGB", "eSON" })
    public void verifySonInstallation(@Input("namespace") final String ns) {
        final String namespace = System.getProperty("namespace", ns);

        logger.info("Verify that eSON has deployed Successfully");
        assertTrue(deploymentOperator.verifySuccessfulDeployment(namespace, eccdHandler), DeploymentCliOperator.getMessage());
        logger.info("The deployment of eSON is Successful");

        logger.info("Verify that eSON deployments are Available");
        assertTrue(deploymentOperator.verifyDeploymentsAvailable(namespace, eccdHandler), DeploymentCliOperator.getMessage());
        logger.info("All eSON deployments are Available");

        logger.info("Verify that eSON Pods are Running");
        assertTrue(deploymentOperator.verifyPodsRunning(namespace, eccdHandler), DeploymentCliOperator.getMessage());
        logger.info("The eSON Pods are Running");
    }

    public static final class StepIds {
        public static final String VERIFY_SUCCESSFUL_INSTALLATION = "verifySuccessfulInstallation";

        private StepIds() {
        }
    }
}
