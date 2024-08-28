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

import java.util.concurrent.TimeUnit;

import com.ericsson.oss.services.eson.test.exceptions.DeploymentException;
import com.ericsson.oss.services.eson.test.operators.DeploymentCliOperator;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import com.ericsson.cifwk.taf.annotations.Input;
import com.ericsson.cifwk.taf.annotations.TestStep;
import com.ericsson.oss.services.eson.test.eccd.EccdHostGroup;
import com.ericsson.oss.services.eson.test.handlers.EccdCliHandler;
import com.ericsson.oss.services.eson.test.handlers.EccdResthandler;
import com.ericsson.oss.services.eson.test.operators.EsLogRestOperator;
import com.ericsson.oss.services.eson.test.operators.database.DatabaseOperator;
import com.ericsson.oss.services.eson.test.operators.database.TestException;

public class ResetEnvironmentTestSteps {

    private final EccdCliHandler eccdHandler = new EccdCliHandler(EccdHostGroup.getEccdDrector());
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Inject
    private DeploymentCliOperator deploymentOperator;

    @TestStep(id = StepIds.CREATE_INGRESS_CLEAR_LOGS)
    @Test(groups = { "RESET ENVIRONMENT" })
    public void createIngressAndClearLogs(@Input("namespace") final String ns) throws InterruptedException {
        final int TIME_TO_ALLOW_FOR_INGRESS_CREATION = 1;
        EsLogRestOperator.createIngressAndServices(eccdHandler, System.getProperty("namespace", ns));
        logger.info("Allow {} minute(s) for ingres creation", TIME_TO_ALLOW_FOR_INGRESS_CREATION);
        TimeUnit.MINUTES.sleep(TIME_TO_ALLOW_FOR_INGRESS_CREATION);
    }

    @TestStep(id = StepIds.RESET_ENVIRONMENT_DATABASE)
    @Test(groups = { "RESET ENVIRONMENT" })
    public void truncateDatabaseTables(@Input("db_connection_details") final String db_connection_details, @Input("db_tables") final String dbTables)
            throws TestException {
        assertTrue(DatabaseOperator.resetDbTables(db_connection_details, dbTables), EccdResthandler.getMessage());
    }

    @TestStep(id=StepIds.RESCALE_PM_EVENT_SPARK_POD)
    @Test(groups = {"RESET ENVIRONMENT"})
    public void rescalePmEventSparkPod(@Input("namespace") final String ns) throws DeploymentException {
        assertTrue(deploymentOperator.rescalePod(3,"eric-pm-events-spark-v2-worker",System.getProperty("namespace", ns),eccdHandler));
    }

    public static final class StepIds {
        public static final String CREATE_INGRESS_CLEAR_LOGS = "createIngress";
        public static final String RESET_ENVIRONMENT_DATABASE = "resetDatabase";
        public static final String RESCALE_PM_EVENT_SPARK_POD="rescalePmEventSparkPod";

        private StepIds() {
        }
    }
}
