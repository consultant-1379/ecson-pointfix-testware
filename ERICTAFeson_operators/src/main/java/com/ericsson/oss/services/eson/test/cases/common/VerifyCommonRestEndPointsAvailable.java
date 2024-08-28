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
import static com.ericsson.oss.services.eson.test.constants.ret.RetDataSourceConstants.GET_REST_POINTS_EXPOSED;
import static com.ericsson.oss.services.eson.test.constants.ret.RetDataSourceConstants.PUT_REST_ENDPOINTS_EXPOSED;

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
import com.ericsson.oss.services.eson.test.teststeps.RestTestSteps;
import com.ericsson.oss.services.eson.test.util.DateTimeUtility;
import com.google.inject.Inject;

public class VerifyCommonRestEndPointsAvailable extends TafTestBase {

    private static final String TESTID = "SONP-37761_VerifyCommon";
    private static final String TEST_CASE_ID_CONTAINS = "testCaseId contains ";
    private static final Logger LOGGER = LoggerFactory.getLogger(VerifyCommonRestEndPointsAvailable.class);

    @Inject
    private RestTestSteps restTestSteps;

    @BeforeMethod(alwaysRun = true)
    public void logBeforeMethod(final Method method) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(DateTimeUtility.testStartMessage(method.getName()));
        }
    }

    @TestSuite
    @TestId(id = TESTID, title = "eSON : SONP-37761_VerifyCommon - Verify REST Endpoints are working using default ingress.")
    @Test(groups = { "KGB", "E_SON", "Setup" })
    public void setUpServerForEnv() {
        final TestScenario scenario = dataDrivenScenario("set up server for EC-SON set default ingress")
                .addFlow(flow("set up server for EC-SON ")
                        .addTestStep(annotatedMethod(restTestSteps, RestTestSteps.StepIds.SET_UP_SERVER)))
                .withScenarioDataSources(dataSource(ENVIRONMENT_FILE).withFilter(TEST_CASE_ID_CONTAINS + "'" + TESTID + "'"))
                .build();
        CommonUtil.start(scenario);
    }

    @TestSuite
    @TestId(id = TESTID, title = "eSON : SONP-37761_VerifyCommon - Verify REST Endpoints are working using default ingress.")
    @Test(groups = { "KGB", "eSON" })
    public void verifyGetRestEndPoint() {
        final TestScenario scenario = dataDrivenScenario("Verify RET rest endpoints")
                .addFlow(flow("Verify GET Common rest endpoints")
                        .addTestStep(annotatedMethod(restTestSteps, RestTestSteps.StepIds.VERIFY_REST_GET_ENDPOINTS)))
                .withScenarioDataSources(dataSource(GET_REST_POINTS_EXPOSED).withFilter(TEST_CASE_ID_CONTAINS + "'" + TESTID + "'"))
                .build();
        CommonUtil.start(scenario);
    }

    @TestSuite
    @TestId(id = TESTID, title = "eSON : SONP-37761_VerifyCommon - Verify REST Endpoints are working using default ingress.")
    @Test(groups = { "KGB", "eSON" })
    public void verifyPutRestEndPoint() {
        final TestScenario scenario = dataDrivenScenario("Verify RET rest endpoints exposed")
                .addFlow(flow("Verify PUT Common rest endpoints")
                        .addTestStep(annotatedMethod(restTestSteps, RestTestSteps.StepIds.VERIFY_REST_PUT_ENDPOINTS)))
                .withScenarioDataSources(dataSource(PUT_REST_ENDPOINTS_EXPOSED).withFilter(TEST_CASE_ID_CONTAINS + "'" + TESTID + "'"))
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