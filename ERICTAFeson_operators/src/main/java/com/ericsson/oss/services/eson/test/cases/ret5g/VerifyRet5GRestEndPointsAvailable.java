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
import com.ericsson.oss.services.eson.test.teststeps.RestTestSteps;
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
import static com.ericsson.oss.services.eson.test.constants.ret.RetDataSourceConstants.GET_REST_POINTS_EXPOSED;
import static com.ericsson.oss.services.eson.test.constants.ret.RetDataSourceConstants.PUT_REST_ENDPOINTS_EXPOSED;

public class VerifyRet5GRestEndPointsAvailable extends TafTestBase {

    private static final String TEST_ID = "IDUN-7785_VerifyRestEndPointsAvailable";
    private static final String FILTER_BY_TESTCASE_ID = "testCaseId contains '%s'";
    private static final String TEST_FILTER = "SONP-32623_VerifyRestEndPointsAvailable";

    @Inject
    private RestTestSteps restTestSteps;

    @Inject
    private CommonTestSteps commonTestSteps;

    @BeforeMethod(alwaysRun = true)
    public void logBeforeMethod(final Method method) {
        commonTestSteps.logBeforeMethod(method);
    }

    @TestSuite
    @TestId(id = TEST_ID, title = "eSON : IDUN-7785_VerifyRet5G - Verify REST Endpoints are working using default ingress.")
    @Test(groups = { "KGB", "E_SON", "Setup" })
    public void setUpServerForEnv() {
        final TestScenario scenario = dataDrivenScenario("set up server for EC-SON set default ingress")
                .addFlow(flow("set up server for EC-SON ")
                        .addTestStep(annotatedMethod(restTestSteps, RestTestSteps.StepIds.SET_UP_SERVER)))
                .withScenarioDataSources(dataSource(ENVIRONMENT_FILE)
                        .withFilter(String.format(FILTER_BY_TESTCASE_ID, "IDUN-7785_VerifyRet5GSuccessfulInstallation")))
                .build();
        CommonUtil.start(scenario);
    }

    @TestSuite
    @TestId(id = TEST_ID, title = "eSON : IDUN-7785_VerifyRestEndPointsAvailable - Verify REST Endpoints are working using default ingress.")
    @Test(groups = { "KGB", "eSON" })
    public void verifyGetRestEndPoint() {
        final TestScenario scenario = dataDrivenScenario("Verify RET 5G rest endpoints")
                .addFlow(flow("Verify GET RET rest endpoints")
                        .addTestStep(annotatedMethod(restTestSteps, RestTestSteps.StepIds.VERIFY_REST_GET_ENDPOINTS)))
                .withScenarioDataSources(dataSource(GET_REST_POINTS_EXPOSED)
                        .withFilter(String.format(FILTER_BY_TESTCASE_ID, TEST_FILTER)))
                .build();
        CommonUtil.start(scenario);
    }

    @TestSuite
    @TestId(id = TEST_ID, title = "eSON : IDUN-7785_VerifyRestEndPointsAvailable - Verify REST Endpoints are working using default ingress.")
    @Test(groups = { "KGB", "eSON" })
    public void verifyPutRestEndPoint() {
        final TestScenario scenario = dataDrivenScenario("Verify RET 5G rest endpoints exposed")
                .addFlow(flow("Verify PUT RET rest endpoints")
                        .addTestStep(annotatedMethod(restTestSteps, RestTestSteps.StepIds.VERIFY_REST_PUT_ENDPOINTS)))
                .withScenarioDataSources(dataSource(PUT_REST_ENDPOINTS_EXPOSED)
                        .withFilter(String.format(FILTER_BY_TESTCASE_ID, TEST_FILTER)))
                .build();
        CommonUtil.start(scenario);
    }

    @AfterMethod(alwaysRun = true)
    public void logAfterMethod(final ITestResult result) {
        commonTestSteps.logAfterMethod(result);
    }

}