/*
 *------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2021
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
import static com.ericsson.oss.services.eson.test.constants.common.EnvDataSourceConstants.CM_COLLECTION_DEFINITIONS;
import static com.ericsson.oss.services.eson.test.constants.common.EnvDataSourceConstants.CM_COLLECTION_UPDATE_DEFINITIONS;
import static com.ericsson.oss.services.eson.test.constants.common.EnvDataSourceConstants.CM_DEFINITIONS;
import static com.ericsson.oss.services.eson.test.constants.common.EnvDataSourceConstants.COMMON_DATA;
import static com.ericsson.oss.services.eson.test.constants.common.EnvDataSourceConstants.COUNTER_DEFINITIONS;
import static com.ericsson.oss.services.eson.test.constants.common.EnvDataSourceConstants.DATABASE_TABLES;
import static com.ericsson.oss.services.eson.test.constants.common.EnvDataSourceConstants.ENVIRONMENT_FILE;
import static org.testng.Assert.assertTrue;

import java.lang.reflect.Method;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.ITestResult;
import org.testng.Reporter;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.ericsson.cifwk.taf.TafTestBase;
import com.ericsson.cifwk.taf.annotations.DataDriven;
import com.ericsson.cifwk.taf.annotations.Input;
import com.ericsson.cifwk.taf.annotations.TestId;
import com.ericsson.cifwk.taf.annotations.TestSuite;
import com.ericsson.cifwk.taf.scenario.TestScenario;
import com.ericsson.oss.services.eson.test.cases.util.CommonUtil;
import com.ericsson.oss.services.eson.test.eccd.EccdHostGroup;
import com.ericsson.oss.services.eson.test.handlers.EccdCliHandler;
import com.ericsson.oss.services.eson.test.operators.CmServiceRestOperator;
import com.ericsson.oss.services.eson.test.operators.DeploymentCliOperator;
import com.ericsson.oss.services.eson.test.operators.IamAccessRestOperator;
import com.ericsson.oss.services.eson.test.operators.OssRepositoryRestOperator;
import com.ericsson.oss.services.eson.test.teststeps.CommonTestSteps;
import com.ericsson.oss.services.eson.test.util.DateTimeUtility;
import com.google.inject.Inject;

/**
 * @DESCRIPTION Execute Common Services and verify working
 * @PRE eSON CSAR files or charts have been deployed on ECCD environment
 * @PRIORITY HIGH
 */
public class VerifyCommon extends TafTestBase {

    private static final String TEST_CASE_TITLE = "SONP-37761_VerifyCommon";
    private static final String TESTID = "SONP-37761_VerifyCommon";
    private static final String TEST_CASE_TITLE_CM_COLLECTIONS = "VerifyCommon_Cm_Collections";
    private static final String TESTID_CM_COLLECTIONS = "VerifyCommon_Cm_Collections";
    private static final String TEST_CASE_ID_CONTAINS = "testCaseId contains ";
    private static final Logger LOGGER = LoggerFactory.getLogger(VerifyCommon.class);

    private final EccdCliHandler eccdHandler = new EccdCliHandler(EccdHostGroup.getEccdDrector());
    private final String eccdLbIp = DeploymentCliOperator.getLoadbalancerIP(eccdHandler);

    private String ingressHost;

    @Inject
    private CmServiceRestOperator cmOperator;

    @Inject
    private CommonTestSteps commonTestSteps;

    @BeforeMethod(alwaysRun = true)
    public void logBeforeMethod(final Method method) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(DateTimeUtility.testStartMessage(method.getName()));
        }
    }

    @TestId(id = TESTID, title = TEST_CASE_TITLE)
    @DataDriven(name = ENVIRONMENT_FILE, filter = "testCaseId == 'SONP-37761_VerifyCommon'")
    @Test(groups = { "KGB", "E_SON", "Setup" })
    public void setProperties(@Input("ingressHost") final String host) {
        ingressHost = System.getProperty("ingressHost", host);
        Reporter.log(String.format("Host is %s", host));
    }

    @TestSuite
    @TestId(id = TESTID, title = TEST_CASE_TITLE)
    @Test(priority = 1)
    public void setupEnv() {
        final TestScenario scenario = dataDrivenScenario("Setup env")
                .addFlow(flow("Setup env")
                        .addTestStep(annotatedMethod(commonTestSteps, CommonTestSteps.StepIds.SETUP_ENV)))
                .withScenarioDataSources(dataSource(ENVIRONMENT_FILE).withFilter(TEST_CASE_ID_CONTAINS +
                        "'" + TEST_CASE_TITLE + "'"))
                .build();
        CommonUtil.start(scenario);
    }

    @TestSuite
    @TestId(id = TESTID, title = TEST_CASE_TITLE)
    @Test(priority = 1, dependsOnMethods = { "setupEnv" })
    public void postKPIDefinitions() {
        final TestScenario scenario = dataDrivenScenario("Put KPI definitions")
                .addFlow(flow("Put KPI definitions").addTestStep(annotatedMethod(commonTestSteps, CommonTestSteps.StepIds.PUT_KPI_DEFINITIONS)))
                .withScenarioDataSources(dataSource(COMMON_DATA).withFilter(TEST_CASE_ID_CONTAINS +
                        "'" + TEST_CASE_TITLE + "'"))
                .build();
        CommonUtil.start(scenario);
    }

    @TestSuite
    @TestId(id = TESTID, title = TEST_CASE_TITLE)
    @Test(priority = 1, dependsOnMethods = { "postKPIDefinitions" })
    public void postCMDefinitions() {
        final TestScenario scenario = dataDrivenScenario("Post CM definitions")
                .addFlow(flow("Post CM definitions")
                        .addTestStep(annotatedMethod(commonTestSteps, CommonTestSteps.StepIds.POST_CM_DEFINITIONS)))
                .withScenarioDataSources(dataSource(CM_DEFINITIONS).withFilter(TEST_CASE_ID_CONTAINS +
                        "'" + TEST_CASE_TITLE + "'"))
                .build();
        CommonUtil.start(scenario);
    }

    @TestSuite
    @TestId(id = TESTID, title = TEST_CASE_TITLE)
    @Test(priority = 1, dependsOnMethods = { "postCMDefinitions" })
    public void putPMDefinitions() {
        final TestScenario scenario = dataDrivenScenario("Put PM definitions")
                .addFlow(flow("Put PM definitions")
                        .addTestStep(annotatedMethod(commonTestSteps, CommonTestSteps.StepIds.PUT_PM_DEFINITIONS)))
                .withScenarioDataSources(dataSource(COUNTER_DEFINITIONS).withFilter(TEST_CASE_ID_CONTAINS +
                        "'" + TEST_CASE_TITLE + "'"))
                .build();
        CommonUtil.start(scenario);
    }

    @TestSuite
    @TestId(id = TESTID, title = TEST_CASE_TITLE)
    @Test(priority = 1, dependsOnMethods = { "putPMDefinitions" })
    public void setupAndVerifyOss() {
        final TestScenario scenario = dataDrivenScenario("setup OSS")
                .addFlow(flow("setup OSS")
                        .addTestStep(annotatedMethod(commonTestSteps, CommonTestSteps.StepIds.SETUP_AND_VERIFY_OSS)))
                .withScenarioDataSources(dataSource(COMMON_DATA).withFilter(TEST_CASE_ID_CONTAINS +
                        "'" + TEST_CASE_TITLE + "'"))
                .build();
        CommonUtil.start(scenario);
    }

    @TestSuite
    @TestId(id = TESTID, title = TEST_CASE_TITLE)
    @Test(priority = 1, dependsOnMethods = { "setupAndVerifyOss" })
    public void verifyCmCountsOverRestBeforeFileUpload() {
        final TestScenario scenario = dataDrivenScenario("Verify CM count")
                .addFlow(flow(CommonTestSteps.StepIds.VERIFY_CM_COUNT_BEFORE)
                        .addTestStep(annotatedMethod(commonTestSteps, CommonTestSteps.StepIds.VERIFY_CM_COUNT_BEFORE)))
                .withScenarioDataSources(dataSource(COMMON_DATA).withFilter(TEST_CASE_ID_CONTAINS +
                        "'" + TEST_CASE_TITLE + "'"))
                .build();
        CommonUtil.start(scenario);
    }

    @TestSuite
    @TestId(id = TESTID, title = TEST_CASE_TITLE)
    @Test(priority = 1, dependsOnMethods = { "verifyCmCountsOverRestBeforeFileUpload" })
    public void verifyEmfFileUpload() {
        final TestScenario scenario = dataDrivenScenario("upload EMF").addFlow(flow("upload EMF")
                .addTestStep(annotatedMethod(commonTestSteps, CommonTestSteps.StepIds.EMF_UPLOAD)))
                .withScenarioDataSources(dataSource(COMMON_DATA).withFilter(TEST_CASE_ID_CONTAINS +
                        "'" + TEST_CASE_TITLE + "'"))
                .build();
        CommonUtil.start(scenario);
    }

    @TestSuite
    @TestId(id = TESTID, title = TEST_CASE_TITLE)
    @Test(priority = 1, dependsOnMethods = { "verifyCmCountsOverRestBeforeFileUpload" })
    public void verifyPtfUpload() {
        final TestScenario scenario = dataDrivenScenario("verify PTF upload").addFlow(flow("verify PTF upload")
                .addTestStep(annotatedMethod(commonTestSteps, CommonTestSteps.StepIds.PTF_UPLOAD)))
                .withScenarioDataSources(dataSource(COMMON_DATA).withFilter(TEST_CASE_ID_CONTAINS +
                        "'" + TEST_CASE_TITLE + "'"))
                .build();
        CommonUtil.start(scenario);
    }

    @TestSuite
    @TestId(id = TESTID, title = TEST_CASE_TITLE)
    @Test(priority = 1, dependsOnMethods = { "verifyPtfUpload" })
    public void verifyCmCountsOverRestAfterFileUpload() {
        final TestScenario scenario = dataDrivenScenario("Verify CM count")
                .addFlow(flow(CommonTestSteps.StepIds.VERIFY_CM_COUNT_AFTER)
                        .addTestStep(annotatedMethod(commonTestSteps, CommonTestSteps.StepIds.VERIFY_CM_COUNT_AFTER)))
                .withScenarioDataSources(dataSource(COMMON_DATA).withFilter(TEST_CASE_ID_CONTAINS +
                        "'" + TEST_CASE_TITLE + "'"))
                .build();
        CommonUtil.start(scenario);
    }

    @TestSuite
    @TestId(id = TESTID, title = TEST_CASE_TITLE)
    @Test(priority = 1, dependsOnMethods = { "verifyPtfUpload" })
    public void verifyCmDbAssertions() {
        final TestScenario scenario = dataDrivenScenario("verify CM db Assertions").addFlow(flow("verify CM db Assertions")
                .addTestStep(annotatedMethod(commonTestSteps, CommonTestSteps.StepIds.VERIFY_CM_DB_ASSERTIONS)))
                .withScenarioDataSources(dataSource("cmDbCounts3K").withFilter(TEST_CASE_ID_CONTAINS +
                        "'" + TEST_CASE_TITLE + "'"))
                .build();
        CommonUtil.start(scenario);

    }

    @TestSuite
    @TestId(id = TESTID, title = TEST_CASE_TITLE)
    @Test(priority = 1, dependsOnMethods = { "verifyCmDbAssertions" })
    public void schedulePmEventsAndStats() {
        final TestScenario scenario = dataDrivenScenario("Schedule PM Events and Stats")
                .addFlow(flow("Schedule PM Events and Stats")
                        .addTestStep(annotatedMethod(commonTestSteps, CommonTestSteps.StepIds.SCHEDULE_PM_EVENTS_AND_STATS)))
                .withScenarioDataSources(dataSource(COMMON_DATA).withFilter(TEST_CASE_ID_CONTAINS +
                        "'" + TEST_CASE_TITLE + "'"))
                .build();
        CommonUtil.start(scenario);
    }

    @TestSuite
    @TestId(id = TESTID_CM_COLLECTIONS, title = TEST_CASE_TITLE_CM_COLLECTIONS)
    @Test(priority = 1, dependsOnMethods = { "setupAndVerifyOss", "verifyCmDbAssertions" })
    public void resetCmCollectionIdCounter() {
        final TestScenario scenario = dataDrivenScenario("Reset CM Collection auto increment id column counter")
                .addFlow(flow("Reset CM Collection auto increment id column counter")
                        .addTestStep(annotatedMethod(commonTestSteps, CommonTestSteps.StepIds.RESET_COLLECTION_ID_COUNTER)))
                .withScenarioDataSources(dataSource(DATABASE_TABLES).withFilter(TEST_CASE_ID_CONTAINS +
                        "'" + TEST_CASE_TITLE_CM_COLLECTIONS + "'"))
                .build();
        CommonUtil.start(scenario);
    }

    @TestSuite
    @TestId(id = TESTID, title = TEST_CASE_TITLE)
    @Test(priority = 1, dependsOnMethods = { "resetCmCollectionIdCounter" })
    public void postCMCollection() {
        final TestScenario scenario = dataDrivenScenario("Post CM collection")
                .addFlow(flow("Post CM collection").addTestStep(annotatedMethod(commonTestSteps, CommonTestSteps.StepIds.POST_CM_COLLECTION)))
                .withScenarioDataSources(dataSource(CM_COLLECTION_DEFINITIONS).withFilter(TEST_CASE_ID_CONTAINS +
                        "'" + TEST_CASE_TITLE + "'"))
                .build();
        CommonUtil.start(scenario);
    }

    @TestSuite
    @TestId(id = TESTID, title = TEST_CASE_TITLE)
    @Test(priority = 1, dependsOnMethods = { "postCMCollection" })
    public void getCMCollection() {
        final TestScenario scenario = dataDrivenScenario("Get CM collection")
                .addFlow(flow("Get CM collection").addTestStep(annotatedMethod(commonTestSteps, CommonTestSteps.StepIds.GET_CM_COLLECTION)))
                .withScenarioDataSources(dataSource(CM_COLLECTION_DEFINITIONS).withFilter(TEST_CASE_ID_CONTAINS +
                        "'" + TEST_CASE_TITLE + "'"))
                .build();
        CommonUtil.start(scenario);
    }

    @TestSuite
    @TestId(id = TESTID, title = TEST_CASE_TITLE)
    @Test(priority = 1, dependsOnMethods = { "postCMCollection" })
    public void getAllCMCollections() {
        final TestScenario scenario = dataDrivenScenario("Get all CM Collections")
                .addFlow(flow("Get all CM collections").addTestStep(annotatedMethod(commonTestSteps, CommonTestSteps.StepIds.GET_CM_COLLECTIONS)))
                .withScenarioDataSources(dataSource(CM_COLLECTION_DEFINITIONS).withFilter(TEST_CASE_ID_CONTAINS +
                        "'" + TEST_CASE_TITLE + "'"))
                .build();
        CommonUtil.start(scenario);
    }

    @TestSuite
    @TestId(id = TESTID, title = TEST_CASE_TITLE)
    @Test(priority = 1, dependsOnMethods = { "postCMCollection" })
    public void evaluateCMCollection() {
        final TestScenario scenario = dataDrivenScenario("Evaluate CM collection")
                .addFlow(flow("Evaluate CM collection").addTestStep(annotatedMethod(commonTestSteps, CommonTestSteps.StepIds.EVALUATE_CM_COLLECTION)))
                .withScenarioDataSources(dataSource(CM_COLLECTION_DEFINITIONS).withFilter(TEST_CASE_ID_CONTAINS +
                        "'" + TEST_CASE_TITLE + "'"))
                .build();
        CommonUtil.start(scenario);
    }

    @TestSuite
    @TestId(id = TESTID, title = TEST_CASE_TITLE)
    @Test(priority = 1, dependsOnMethods = { "getCMCollection", "getAllCMCollections", "evaluateCMCollection" })
    public void updateCMCollection() {
        final TestScenario scenario = dataDrivenScenario("Update CM collection")
                .addFlow(flow("Update CM collection").addTestStep(annotatedMethod(commonTestSteps, CommonTestSteps.StepIds.UPDATE_CM_COLLECTION)))
                .withScenarioDataSources(dataSource(CM_COLLECTION_UPDATE_DEFINITIONS).withFilter(TEST_CASE_ID_CONTAINS +
                        "'" + TEST_CASE_TITLE + "'"))
                .build();
        CommonUtil.start(scenario);
    }

    @TestSuite
    @TestId(id = TESTID, title = TEST_CASE_TITLE)
    @Test(priority = 1, dependsOnMethods = { "updateCMCollection" })
    public void deleteCMCollection() {
        final TestScenario scenario = dataDrivenScenario("Delete CM collection")
                .addFlow(flow("Delete CM collection").addTestStep(annotatedMethod(commonTestSteps, CommonTestSteps.StepIds.DELETE_CM_COLLECTION)))
                .withScenarioDataSources(dataSource(CM_COLLECTION_DEFINITIONS).withFilter(TEST_CASE_ID_CONTAINS +
                        "'" + TEST_CASE_TITLE + "'"))
                .build();
        CommonUtil.start(scenario);
    }

    @TestSuite
    @TestId(id = TESTID, title = TEST_CASE_TITLE)
    @Test(priority = 1, dependsOnMethods = { "verifyCmDbAssertions", "schedulePmEventsAndStats" })
    public void verifyPmEventsExecuted() {
        final TestScenario scenario = dataDrivenScenario("verify PM Events Executed").addFlow(flow("verify PM Events Executed")
                .addTestStep(annotatedMethod(commonTestSteps, CommonTestSteps.StepIds.VERIFY_PM_EVENTS_EXECUTED)))
                .withScenarioDataSources(dataSource(COMMON_DATA).withFilter(TEST_CASE_ID_CONTAINS +
                        "'" + TEST_CASE_TITLE + "'"))
                .build();
        CommonUtil.start(scenario);
    }

    @TestSuite
    @TestId(id = TESTID, title = TEST_CASE_TITLE)
    @Test(priority = 1, dependsOnMethods = { "verifyCmDbAssertions", "verifyPmEventsExecuted" })
    public void verifyPmEventsDbAssertions() {
        final TestScenario scenario = dataDrivenScenario("verify PM Events db Assertions").addFlow(flow("verify PM Events db Assertions")
                .addTestStep(annotatedMethod(commonTestSteps, CommonTestSteps.StepIds.VERIFY_PM_EVENTS_DB_ASSERTIONS)))
                .withScenarioDataSources(dataSource("pmEventsDbCounts3K").withFilter(TEST_CASE_ID_CONTAINS +
                        "'" + TEST_CASE_TITLE + "'"))
                .build();
        CommonUtil.start(scenario);

    }

    @TestSuite
    @TestId(id = TESTID, title = TEST_CASE_TITLE)
    @Test(priority = 1, dependsOnMethods = { "verifyCmDbAssertions", "verifyPmEventsDbAssertions" })
    public void verifyPmEventsDbValueAssertions() {
        final TestScenario scenario = dataDrivenScenario("verify PM Events db value Assertions").addFlow(flow("verify PM Events db value Assertions")
                .addTestStep(annotatedMethod(commonTestSteps, CommonTestSteps.StepIds.VERIFY_PM_EVENTS_DB_VALUE_ASSERTIONS)))
                .withScenarioDataSources(dataSource("pmEventsDbValues3K")
                        .withFilter(TEST_CASE_ID_CONTAINS +
                                "'" + TEST_CASE_TITLE + "'"))
                .build();
        CommonUtil.start(scenario);
    }

    @TestSuite
    @TestId(id = TESTID, title = TEST_CASE_TITLE)
    @Test(priority = 1, dependsOnMethods = { "schedulePmEventsAndStats", "verifyCmDbAssertions", "verifyPmEventsDbValueAssertions" })
    public void verifyPmStatsExecuted() {
        final TestScenario scenario = dataDrivenScenario("verify PM stats Executed").addFlow(flow("verify PM Stats Executed")
                .addTestStep(annotatedMethod(commonTestSteps, CommonTestSteps.StepIds.VERIFY_PM_STATS_EXECUTED)))
                .withScenarioDataSources(dataSource(COMMON_DATA).withFilter(TEST_CASE_ID_CONTAINS +
                        "'" + TEST_CASE_TITLE + "'"))
                .build();
        CommonUtil.start(scenario);

    }

    @TestSuite
    @TestId(id = TESTID, title = TEST_CASE_TITLE)
    @Test(priority = 1, dependsOnMethods = { "verifyPmStatsExecuted" })
    public void verifyPmStatsDbAssertions() {
        final TestScenario scenario = dataDrivenScenario("verify PM Stats db Assertions").addFlow(flow("verify PM Stats db Assertions")
                .addTestStep(annotatedMethod(commonTestSteps, CommonTestSteps.StepIds.VERIFY_PM_STATS_DB_ASSERTIONS)))
                .withScenarioDataSources(dataSource("pmStatsDbCounts3K").withFilter(TEST_CASE_ID_CONTAINS +
                        "'" + TEST_CASE_TITLE + "'"))
                .build();
        CommonUtil.start(scenario);

    }

    @TestSuite
    @TestId(id = TESTID, title = TEST_CASE_TITLE)
    @Test(priority = 1, dependsOnMethods = { "verifyPmStatsDbAssertions" })
    public void verifyPmStatsDbValueAssertions() {
        final TestScenario scenario = dataDrivenScenario("verify PM Stats db value Assertions").addFlow(flow("verify PM Stats db value Assertions")
                .addTestStep(annotatedMethod(commonTestSteps, CommonTestSteps.StepIds.VERIFY_PM_STATS_DB_VALUE_ASSERTIONS)))
                .withScenarioDataSources(dataSource("pmStatsDbValues3K").withFilter(TEST_CASE_ID_CONTAINS +
                        "'" + TEST_CASE_TITLE + "'"))
                .build();
        CommonUtil.start(scenario);

    }

    @TestSuite
    @TestId(id = TESTID, title = TEST_CASE_TITLE)
    @Test(priority = 1, dependsOnMethods = { "verifyPmEventsDbValueAssertions", "verifyPmStatsDbValueAssertions" })
    public void verifyScheduledKpisExecuted() {
        final TestScenario scenario = dataDrivenScenario("verify scheduled KPI Calculation Executed")
                .addFlow(flow("verify scheduled KPI Calculation Executed")
                        .addTestStep(annotatedMethod(commonTestSteps, CommonTestSteps.StepIds.VERIFY_SCHEDULED_KPIS_EXECUTED)))
                .withScenarioDataSources(dataSource(COMMON_DATA).withFilter(TEST_CASE_ID_CONTAINS +
                        "'" + TEST_CASE_TITLE + "'"))
                .build();
        CommonUtil.start(scenario);
    }

    @TestSuite
    @TestId(id = TESTID, title = TEST_CASE_TITLE)
    @Test(priority = 1, dependsOnMethods = { "verifyScheduledKpisExecuted" })
    public void verifyKpiCalculatorDbAssertions() {
        final TestScenario scenario = dataDrivenScenario("verify KPI db Assertions").addFlow(flow("verify KPI db Assertions")
                .addTestStep(annotatedMethod(commonTestSteps, CommonTestSteps.StepIds.VERIFY_KPI_DB_ASSERTIONS)))
                .withScenarioDataSources(dataSource("kpiCalculatorDbCounts3K").withFilter(TEST_CASE_ID_CONTAINS + "'" + TEST_CASE_TITLE + "'"))
                .build();
        CommonUtil.start(scenario);

    }

    @TestSuite
    @TestId(id = TESTID, title = TEST_CASE_TITLE)
    @Test(priority = 1, dependsOnMethods = { "verifyKpiCalculatorDbAssertions" })
    public void verifyKpiCalculatorDbValueAssertionsOnView() {
        final TestScenario scenario = dataDrivenScenario("verify KPI db value Assertions").addFlow(flow("verify KPI db value Assertions")
                .addTestStep(annotatedMethod(commonTestSteps, CommonTestSteps.StepIds.VERIFY_KPI_DB_VALUE_ASSERTIONS)))
                .withScenarioDataSources(dataSource("kpiCalculatorViewValues3K").withFilter(TEST_CASE_ID_CONTAINS +
                        "'" + TEST_CASE_TITLE + "'"))
                .build();
        CommonUtil.start(scenario);

    }

    @TestSuite
    @TestId(id = TESTID, title = TEST_CASE_TITLE)
    @Test(priority = 1, dependsOnMethods = { "verifyKpiCalculatorDbValueAssertionsOnView" })
    public void postCmChanges() {
        final TestScenario scenario = dataDrivenScenario("Post CM Changes").addFlow(flow("Post CM Changes")
                .addTestStep(annotatedMethod(commonTestSteps, CommonTestSteps.StepIds.POST_CHANGES)))
                .withScenarioDataSources(dataSource("changes").withFilter(TEST_CASE_ID_CONTAINS +
                        "'" + TEST_CASE_TITLE + "'"))
                .build();
        CommonUtil.start(scenario);

    }

    @TestSuite
    @TestId(id = TESTID, title = TEST_CASE_TITLE)
    @Test(groups = { "KGB", "E_SON", "CAS" }, dependsOnMethods = { "postCmChanges" })
    public void verifyChanges() {
        final TestScenario scenario = dataDrivenScenario("Verify Changes").addFlow(flow("Verify Changes")
                .addTestStep(annotatedMethod(commonTestSteps, CommonTestSteps.StepIds.VERIFY_CHANGES)))
                .withScenarioDataSources(dataSource("changes").withFilter(TEST_CASE_ID_CONTAINS +
                        "'" + TEST_CASE_TITLE + "'"))
                .build();
        CommonUtil.start(scenario);

    }

    @AfterMethod(alwaysRun = true)
    public void logAfterMethod(final ITestResult result) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(DateTimeUtility.testEndMessage(result.getMethod().getMethodName()));
        }
    }

    @AfterClass(alwaysRun = true)
    public void deleteOSS() {
        LOGGER.info("Delete OSS from VerifyCommon");
        assertTrue(OssRepositoryRestOperator.deleteConfiguredOSS(eccdLbIp, ingressHost, IamAccessRestOperator.TAF_SUPER_USER));
    }
}
