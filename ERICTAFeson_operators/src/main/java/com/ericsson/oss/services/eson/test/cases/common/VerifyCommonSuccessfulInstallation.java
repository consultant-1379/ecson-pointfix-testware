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
import com.ericsson.oss.services.eson.test.teststeps.VerifyInstallationTestSteps;
import com.ericsson.oss.services.eson.test.util.DateTimeUtility;
import com.google.inject.Inject;

public class VerifyCommonSuccessfulInstallation extends TafTestBase {

    private static final String TESTID = "SONP-37761_VerifyCommo";
    private static final Logger LOGGER = LoggerFactory.getLogger(VerifyCommonSuccessfulInstallation.class);

    @Inject
    private VerifyInstallationTestSteps verifyInstallationTestSteps;

    @BeforeMethod(alwaysRun = true)
    public void logBeforeMethod(final Method method) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(DateTimeUtility.testStartMessage(method.getName()));
        }
    }

    @TestSuite
    @TestId(id = TESTID, title = "eSON : SONP-37761_VerifyCommon - Verify successful installation of eSON.")
    @Test(groups = { "KGB", "eSON" })
    public void verifySonInstallation() {
        final TestScenario scenario = dataDrivenScenario("Verify successful installation of eSON ")
                .addFlow(flow("Check successful installation of eSON ")
                        .addTestStep(
                                annotatedMethod(verifyInstallationTestSteps, VerifyInstallationTestSteps.StepIds.VERIFY_SUCCESSFUL_INSTALLATION)))
                .withScenarioDataSources(dataSource(ENVIRONMENT_FILE).withFilter("testCaseId contains " +
                        "'SONP-37761_VerifyCommon'"))
                .build();
        CommonUtil.start(scenario);
    }

    @AfterMethod(alwaysRun = true)
    public void logAfterMethod(final ITestResult result) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(DateTimeUtility.testEndMessage(result.getMethod().getMethodName()));
        }
    }

}
