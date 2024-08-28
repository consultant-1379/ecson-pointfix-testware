/*
 *------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2018 - 2020
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *------------------------------------------------------------------------------
 */
package com.ericsson.oss.services.eson.test.cases.ret;

import static com.ericsson.oss.services.eson.test.constants.common.EnvDataSourceConstants.DATABASE_TABLES;
import static com.ericsson.oss.services.eson.test.constants.common.EnvDataSourceConstants.ENVIRONMENT_FILE;

import java.lang.reflect.Method;

import org.testng.ITestResult;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.ericsson.cifwk.taf.TafTestBase;
import com.ericsson.cifwk.taf.annotations.TestId;
import com.ericsson.cifwk.taf.annotations.TestSuite;
import com.ericsson.oss.services.eson.test.cases.common.ResetEnvironment;
import com.ericsson.oss.services.eson.test.teststeps.ResetEnvironmentTestSteps;
import com.google.inject.Inject;

public class ResetEnvironmentRet extends TafTestBase {

    private static final String TEST_CASE_TITLE = "eSON : SONP-32625_ScheduleRetAndVerifyProposedChange " +
            "- Schedule RET Algorithm and Verify proposed Change.";
    private static final String TEST_ID = "SONP-32625_ScheduleRetAndVerifyProposedChange";
    private static final String TEST_FILTER = "testCaseId contains 'SONP-32625_ScheduleRetAndVerifyProposedChange'";

    @Inject
    private ResetEnvironment resetEnvironment;

    @BeforeMethod(alwaysRun = true)
    public void logBeforeMethod(final Method method) {
        resetEnvironment.logBeforeMethod(method);
    }

    @TestSuite
    @TestId(id = TEST_ID, title = TEST_CASE_TITLE)
    @Test(priority = 1)
    public void createIngressAndClearLogs() {
        resetEnvironment.buildTestScenario("Create ingress", "Create ingress and services",
                ResetEnvironmentTestSteps.StepIds.CREATE_INGRESS_CLEAR_LOGS, ENVIRONMENT_FILE, TEST_FILTER);
    }

    @TestSuite
    @TestId(id = TEST_ID, title = TEST_CASE_TITLE)
    @Test(priority = 2)
    public void truncateDatabaseTables() {
        resetEnvironment.buildTestScenario("Reset database tables", "Truncate database tables",
                ResetEnvironmentTestSteps.StepIds.RESET_ENVIRONMENT_DATABASE, DATABASE_TABLES, TEST_FILTER);
    }

    @AfterMethod(alwaysRun = true)
    public void logAfterMethod(final ITestResult result) {
        resetEnvironment.logAfterMethod(result);
    }

}