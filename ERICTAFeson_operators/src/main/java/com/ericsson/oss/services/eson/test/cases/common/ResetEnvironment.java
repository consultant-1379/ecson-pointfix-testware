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
package com.ericsson.oss.services.eson.test.cases.common;

import static com.ericsson.cifwk.taf.scenario.TestScenarios.annotatedMethod;
import static com.ericsson.cifwk.taf.scenario.TestScenarios.dataDrivenScenario;
import static com.ericsson.cifwk.taf.scenario.TestScenarios.dataSource;
import static com.ericsson.cifwk.taf.scenario.TestScenarios.flow;
import static com.ericsson.oss.services.eson.test.constants.common.EnvDataSourceConstants.DATABASE_TABLES;
import static com.ericsson.oss.services.eson.test.constants.common.EnvDataSourceConstants.ENVIRONMENT_FILE;

import java.lang.reflect.Method;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.ITestResult;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.ericsson.cifwk.taf.TafTestBase;
import com.ericsson.cifwk.taf.annotations.TestId;
import com.ericsson.cifwk.taf.annotations.TestSuite;
import com.ericsson.cifwk.taf.scenario.TestScenario;
import com.ericsson.oss.services.eson.test.cases.util.CommonUtil;
import com.ericsson.oss.services.eson.test.teststeps.ResetEnvironmentTestSteps;
import com.ericsson.oss.services.eson.test.util.DateTimeUtility;
import com.google.inject.Inject;

public class ResetEnvironment extends TafTestBase {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResetEnvironment.class);
    private static final String TEST_CASE_TITLE = "eSON : SONP-37761_VerifyCommon";
    private static final String TESTID = "SONP-37761_VerifyCommon";
    private static final String TEST_FILTER = "testCaseId contains 'SONP-37761_VerifyCommon'";

    @Inject
    private ResetEnvironmentTestSteps resetEnvironmentTestSteps;

    @BeforeMethod(alwaysRun = true)
    public void logBeforeMethod(final Method method) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(DateTimeUtility.testStartMessage(method.getName()));
        }
    }

    @TestSuite
    @TestId(id = TESTID, title = TEST_CASE_TITLE)
    @Test(priority = 1)
    public void createIngressAndClearLogs() {
        buildTestScenario("Create ingress", "Create ingress and services", ResetEnvironmentTestSteps.StepIds.CREATE_INGRESS_CLEAR_LOGS,
                ENVIRONMENT_FILE, TEST_FILTER);
    }

    @TestSuite
    @TestId(id = TESTID, title = TEST_CASE_TITLE)
    @Test(priority = 2)
    public void truncateDatabaseTables() {
        buildTestScenario("Reset database tables", "Truncate database tables", ResetEnvironmentTestSteps.StepIds.RESET_ENVIRONMENT_DATABASE,
                DATABASE_TABLES, TEST_FILTER);
    }

    @AfterMethod(alwaysRun = true)
    public void logAfterMethod(final ITestResult result) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(DateTimeUtility.testEndMessage(result.getMethod().getMethodName()));
        }
    }

    public void buildTestScenario(final String scenarioName, final String flow, final String steps, final String dataSource, final String filter) {
        final TestScenario scenario = dataDrivenScenario(scenarioName)
                .addFlow(flow(flow)
                        .addTestStep(annotatedMethod(resetEnvironmentTestSteps, steps)))
                .withScenarioDataSources(dataSource(dataSource).withFilter(filter))
                .build();
        CommonUtil.start(scenario);
    }

}