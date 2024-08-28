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
import com.ericsson.cifwk.taf.scenario.TestScenario;
import com.ericsson.oss.services.eson.test.cases.util.CommonUtil;
import com.ericsson.oss.services.eson.test.teststeps.CommonTestSteps;
import com.ericsson.oss.services.eson.test.teststeps.VerifyInstallationTestSteps;
import com.google.inject.Inject;
import org.testng.ITestResult;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.lang.reflect.Method;

import static com.ericsson.cifwk.taf.scenario.TestScenarios.annotatedMethod;
import static com.ericsson.cifwk.taf.scenario.TestScenarios.dataDrivenScenario;
import static com.ericsson.cifwk.taf.scenario.TestScenarios.dataSource;
import static com.ericsson.cifwk.taf.scenario.TestScenarios.flow;
import static com.ericsson.oss.services.eson.test.constants.common.EnvDataSourceConstants.ENVIRONMENT_FILE;

public class VerifyRet5GSuccessfulInstallation extends TafTestBase {

    private static final String TEST_ID = "IDUN-7785_VerifyRet5GSuccessfulInstallation";
    private static final String FILTER_BY_TESTCASE_ID = "testCaseId contains '%s'";

    @Inject
    private VerifyInstallationTestSteps verifyInstallationTestSteps;

    @Inject
    private CommonTestSteps commonTestSteps;

    @BeforeMethod(alwaysRun = true)
    public void logBeforeMethod(final Method method) {
        commonTestSteps.logBeforeMethod(method);
    }

    @TestSuite
    @TestId(id = TEST_ID, title = "eSON : IDUN-7785_VerifyRet5GSuccessfulInstallation - Verify successful installation of RET 5G.")
    @Test(groups = { "KGB", "eSON" })
    public void verifySonInstallation() {
        final TestScenario scenario = dataDrivenScenario("Verify successful installation of RET 5G ")
                .addFlow(flow("Check successful installation of RET 5G ")
                        .addTestStep(
                                annotatedMethod(verifyInstallationTestSteps, VerifyInstallationTestSteps.StepIds.VERIFY_SUCCESSFUL_INSTALLATION)))
                .withScenarioDataSources(dataSource(ENVIRONMENT_FILE)
                        .withFilter(String.format(FILTER_BY_TESTCASE_ID, TEST_ID)))
                .build();
        CommonUtil.start(scenario);
    }

    @AfterMethod(alwaysRun = true)
    public void logAfterMethod(final ITestResult result) {
        commonTestSteps.logAfterMethod(result);
    }

}
