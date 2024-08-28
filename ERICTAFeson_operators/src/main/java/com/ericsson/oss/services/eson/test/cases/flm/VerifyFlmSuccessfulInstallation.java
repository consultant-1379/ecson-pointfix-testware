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

package com.ericsson.oss.services.eson.test.cases.flm;

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

public class VerifyFlmSuccessfulInstallation extends TafTestBase {

    private static final String TEST_CASE_TITLE = "Verify successful installation of EC-SON FLM";
    private static final String TESTID = "SONP-37570_VerifyFlmSuccessfulInstallation";

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Inject
    private VerifyInstallationTestSteps verifyInstallationTestSteps;

    @BeforeMethod(alwaysRun = true)
    public void logBeforeMethod(final Method method) {
        if (logger.isInfoEnabled()) {
            logger.info(DateTimeUtility.testStartMessage(method.getName()));
        }
    }

    @TestSuite
    @TestId(id = TESTID, title = TEST_CASE_TITLE)
    @Test(priority = 1)
    public void verifyFlmESonInstallation() {
        final TestScenario scenario = dataDrivenScenario("Verify successful installation of eSON FLM.")
                .addFlow(flow("Check successful installation of eSON FLM")
                        .addTestStep(
                                annotatedMethod(verifyInstallationTestSteps, VerifyInstallationTestSteps.StepIds.VERIFY_SUCCESSFUL_INSTALLATION)))
                .withScenarioDataSources(dataSource(ENVIRONMENT_FILE).withFilter("testCaseId contains " +
                        "'SONP-37570_VerifyFlmSuccessfulInstallation'"))
                .build();
        CommonUtil.start(scenario);
    }

    @AfterMethod(alwaysRun = true)
    public void logAfterMethod(final ITestResult result) {
        if (logger.isInfoEnabled()) {
            logger.info(DateTimeUtility.testEndMessage(result.getMethod().getMethodName()));
        }
    }

}
