/*
 *------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2018 - 2021
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *------------------------------------------------------------------------------
 */
package com.ericsson.oss.services.eson.test.cases.ret5g;

import com.ericsson.cifwk.taf.TafTestBase;
import com.ericsson.cifwk.taf.annotations.TestId;
import com.ericsson.cifwk.taf.annotations.TestSuite;
import com.ericsson.oss.services.eson.test.cases.common.ResetEnvironment;
import com.ericsson.oss.services.eson.test.teststeps.ResetEnvironmentTestSteps;
import com.google.inject.Inject;
import org.testng.ITestResult;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.lang.reflect.Method;

import static com.ericsson.oss.services.eson.test.constants.common.EnvDataSourceConstants.DATABASE_TABLES;
import static com.ericsson.oss.services.eson.test.constants.common.EnvDataSourceConstants.ENVIRONMENT_FILE;

public class ResetEnvironmentRet5G extends TafTestBase {

    private static final String TEST_CASE_TITLE = "eSON : IDUN-7785_ResetEnvironmentRet5G - Create ingress and reset database tables.";
    private static final String TEST_ID = "IDUN-7785_ResetEnvironmentRet5G";
    private static final String TEST_FILTER = "testCaseId contains 'IDUN-7785_VerifyRet5G'";

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
                ResetEnvironmentTestSteps.StepIds.CREATE_INGRESS_CLEAR_LOGS, ENVIRONMENT_FILE, "testCaseId =='IDUN-7785_VerifyRet5GSuccessfulInstallation'");
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