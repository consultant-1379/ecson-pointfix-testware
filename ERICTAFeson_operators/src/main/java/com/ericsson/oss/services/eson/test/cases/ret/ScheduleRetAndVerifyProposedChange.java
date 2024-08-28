/*
 *------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2018 - 2022
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

import static com.ericsson.oss.services.eson.test.constants.ret.RetRestConstants.SLEEP_TIME_TO_ALLOW_RET_TRIGGER;
import static org.testng.Assert.assertTrue;

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.ITestResult;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.ericsson.cifwk.taf.TafTestBase;
import com.ericsson.cifwk.taf.annotations.DataDriven;
import com.ericsson.cifwk.taf.annotations.Input;
import com.ericsson.cifwk.taf.annotations.TestId;
import com.ericsson.oss.services.eson.test.eccd.EccdHostGroup;
import com.ericsson.oss.services.eson.test.handlers.EccdCliHandler;
import com.ericsson.oss.services.eson.test.operators.DeploymentCliOperator;
import com.ericsson.oss.services.eson.test.operators.IamAccessRestOperator;
import com.ericsson.oss.services.eson.test.operators.RetAlgorithmRestOperator;
import com.ericsson.oss.services.eson.test.operators.database.DatabaseOperator;
import com.ericsson.oss.services.eson.test.operators.database.TestException;
import com.ericsson.oss.services.eson.test.teststeps.CommonTestSteps;
import com.ericsson.oss.services.eson.test.util.ReplaceDate;
import com.google.inject.Inject;

public class ScheduleRetAndVerifyProposedChange extends TafTestBase {

    private static final Logger LOGGER = LoggerFactory.getLogger(ScheduleRetAndVerifyProposedChange.class);

    private static final EccdCliHandler ECCD_CLI_HANDLER = new EccdCliHandler(EccdHostGroup.getEccdDrector());

    private static final String RET_DATA = "retData";
    private static final String ENVIRONMENT_FILE = "environment";
    private static final String TEST_CASE_TITLE = "eSON : SONP-32625_ScheduleRetAndVerifyProposedChange - Schedule RET Algorithm and Verify proposed Change.";
    private static final String TESTID = "SONP-32625_ScheduleRetAndVerifyProposedChange";
    private static final String ECCD_LB_IP = DeploymentCliOperator.getLoadbalancerIP(ECCD_CLI_HANDLER);

    @Inject
    private CommonTestSteps commonTestSteps;

    /**
     * @DESCRIPTION Schedule RET Algorithm and Verify proposed Change.
     * @PRE eSON CSAR files have been deployed on ECCD environment
     * @PRIORITY HIGH
     */

    @BeforeMethod(alwaysRun = true)
    public void logBeforeMethod(final Method method) {
        commonTestSteps.logBeforeMethod(method);
    }

    @TestId(id = TESTID, title = TEST_CASE_TITLE)
    @DataDriven(name = ENVIRONMENT_FILE, filter = "testCaseId == 'SONP-32625_ScheduleRetAndVerifyProposedChange'")
    @Test(groups = { "KGB", "E_SON", "Setup" })
    public void setupEnv(@Input("ingressHost") final String host,
            @Input("namespace") final String ns,
            @Input("releaseName") final String relName,
            @Input("installDirectory") final String installDir) {
        commonTestSteps.setupEnv(host, ns, relName, installDir);
    }

    @TestId(id = TESTID, title = TEST_CASE_TITLE)
    @DataDriven(name = RET_DATA)
    @Test(groups = { "Put kpi definitions for needed kpis" }, dependsOnGroups = { "Setup" })
    public void putKpiDefinitions(@Input("kpi_definition_file") final String kpiDefinitionFile) {
        LOGGER.info("Put kpi definitions needed for RET algorithm");
        commonTestSteps.putKpiDefinitions(kpiDefinitionFile);
    }

    @TestId(id = TESTID, title = TEST_CASE_TITLE)
    @DataDriven(name = RET_DATA)
    @Test(groups = { "CM loading and set schedules" }, dependsOnMethods = { "putKpiDefinitions" })
    public void setupAndVerifyOss(@Input("description") final String description,
            @Input("event_stats_delay") final int event_stats_delay) {
        final String ossPayload = "{\"type\":\"enm\",\"name\":\"stubbed-enm\",\"content\":\"{\\\"enm_ui_host\\\":\\\"stubbed-enm\\\",\\\"enm_ui_username\\\":\\\"eson_user\\\",\\\"enm_ui_password\\\": \\\"enm12admin\\\",\\\"enm_cm_data_loading_cron\\\":\\\"\\\"}\"}";
        commonTestSteps.setupAndVerifyOss(description, event_stats_delay, ossPayload);
    }

    @TestId(id = TESTID, title = TEST_CASE_TITLE)
    @DataDriven(name = RET_DATA)
    @Test(groups = { "KGB", "E_SON", "CM Service" }, dependsOnMethods = { "setupAndVerifyOss" })
    public void verifyCmCountOverRestBeforeFileUpload(@Input("topology_object_names_before_file_upload") final String topologyObjectNames,
            @Input("topology_object_counts_before_file_upload") final String topologyObjectCounts) {
        commonTestSteps.verifyCmCountsOverRestBeforeFileUpload(topologyObjectNames, topologyObjectCounts);
    }

    @TestId(id = TESTID, title = TEST_CASE_TITLE)
    @DataDriven(name = RET_DATA)
    @Test(groups = { "KGB", "E_SON", "CM Service" }, dependsOnMethods = { "verifyCmCountOverRestBeforeFileUpload" })
    public void verifyEmfFileUpload(@Input("emfData") final String emfData) {
        commonTestSteps.verifyEmfFileUpload(emfData);
    }

    @TestId(id = TESTID, title = TEST_CASE_TITLE)
    @DataDriven(name = RET_DATA)
    @Test(groups = { "KGB", "E_SON", "CM Service" }, dependsOnMethods = { "verifyCmCountOverRestBeforeFileUpload" })
    public void verifyPtfFileUpload(@Input("ptfData") final String ptfData) {
        commonTestSteps.verifyPtfFileUpload(ptfData);
    }

    @TestId(id = TESTID, title = TEST_CASE_TITLE)
    @DataDriven(name = RET_DATA)
    @Test(groups = { "KGB", "E_SON", "CM Service" }, dependsOnMethods = { "verifyPtfFileUpload" })
    public void verifyCmCountOverRestAfterFileUpload(@Input("topology_object_names_after_file_upload") final String topologyObjectNames,
            @Input("topology_object_counts_after_file_upload") final String topologyObjectCounts) {
        commonTestSteps.verifyCmCountsOverRestAfterFileUpload(topologyObjectNames, topologyObjectCounts);
    }

    @TestId(id = TESTID, title = TEST_CASE_TITLE)
    @DataDriven(name = "cmDbCounts3K", filter = "testCaseId =='" + TESTID + "'")
    @Test(groups = { "KGB", "E_SON", "CM Service" }, dependsOnMethods = { "verifyPtfFileUpload" })
    public void verifyCmDbAssertions(@Input("db_connection_details") final String db_connection_details, @Input("db_tables") final String dbTables,
            @Input("db_table_count_values") final String dbTableCountValues, @Input("polling_timeout") final int pollingTimeOut)
            throws TestException {
        commonTestSteps.verifyCmDbAssertions(db_connection_details, dbTables, dbTableCountValues, pollingTimeOut);
    }

    @TestId(id = TESTID, title = TEST_CASE_TITLE)
    //@DataDriven(name = "commonData3K", filter = "testCaseId =='" + TESTID + "'")
    @DataDriven(name = RET_DATA)
    @Test(groups = { "KGB", "E_SON", "CM Service" }, dependsOnMethods = { "verifyCmDbAssertions" })
    public void schedulePmEventsAndStats(@Input("event_stats_delay") final int event_stats_delay) {
        LOGGER.info("Scheduling pm events and stats");
        commonTestSteps.schedulePmEventsAndStats(event_stats_delay);
    }

    @TestId(id = TESTID, title = TEST_CASE_TITLE)
    @DataDriven(name = RET_DATA)
    @Test(groups = { "KGB", "E_SON", "PMEvents" }, dependsOnMethods = { "verifyCmDbAssertions" })
    public void verifyPmEventsExecuted(@Input("pm_files_processed") final int pm_files_processed) {
        commonTestSteps.verifyPmEventsExecuted(pm_files_processed);
    }

    @TestId(id = TESTID, title = TEST_CASE_TITLE)
    @DataDriven(name = "pmEventsDbCounts3K")
    @Test(groups = { "KGB", "E_SON", "PMEvents" }, dependsOnMethods = { "verifyPmEventsExecuted" })
    public void verifyPmEventsDbAssertions(@Input("db_connection_details") final String db_connection_details,
            @Input("db_tables") final String dbTables, @Input("db_table_count_values") final String dbTableCountValues,
            @Input("polling_timeout") final int pollingTimeOut) throws TestException {
        commonTestSteps.verifyPmEventsDbAssertions(db_connection_details, dbTables, dbTableCountValues, pollingTimeOut);
    }

    @TestId(id = TESTID, title = TEST_CASE_TITLE)
    @DataDriven(name = "pmEventsAutoAlignDates")
    @Test(groups = { "KGB", "E_SON", "PMEvents" }, dependsOnMethods = { "verifyPmEventsDbAssertions" })
    public void autoAlignPmEventsDates(@Input("db_connection_details") final String db_connection_details, @Input("db_tables") final String dbTables)
            throws TestException {
        assertTrue(DatabaseOperator.autoAlignPmEventsTableDates(db_connection_details, dbTables), DatabaseOperator.getMessage());
    }

    @TestId(id = TESTID, title = TEST_CASE_TITLE)
    @DataDriven(name = RET_DATA)
    @Test(groups = { "KGB", "E_SON", "PMStats" }, dependsOnMethods = { "autoAlignPmEventsDates" })
    public void verifyPmStatsExecuted() {
        commonTestSteps.verifyPmStatsExecuted();
    }

    @TestId(id = TESTID, title = TEST_CASE_TITLE)
    @DataDriven(name = "pmStatsDbCounts3K")
    @Test(groups = { "KGB", "E_SON", "PMStats" }, dependsOnMethods = { "verifyPmStatsExecuted" })
    public void verifyPmStatsDbAssertions(@Input("db_connection_details") final String db_connection_details,
            @Input("db_tables") final String dbTables, @Input("db_table_count_values") final String dbTableCountValues,
            @Input("polling_timeout") final int pollingTimeOut) throws TestException {
        commonTestSteps.verifyPmStatsDbAssertions(db_connection_details, dbTables, dbTableCountValues, pollingTimeOut);
    }

    @TestId(id = TESTID, title = TEST_CASE_TITLE)
    @DataDriven(name = RET_DATA)
    @Test(groups = { "KGB", "E_SON", "KPI" }, dependsOnMethods = { "verifyPmEventsDbAssertions", "verifyPmStatsDbAssertions" })
    public void verifyScheduledKpisExecuted(@Input("kpi_verification_file") final String kpiVerificationFile) {
        LOGGER.info("Requesting calculation of KPIs required for RET through /calculation endpoint");
        commonTestSteps.verifyScheduledKpisExecuted(kpiVerificationFile);
    }

    @TestId(id = TESTID, title = TEST_CASE_TITLE)
    @DataDriven(name = "kpiCalculatorDbCounts3K")
    @Test(groups = { "KGB", "E_SON", "KPI" }, dependsOnMethods = { "verifyScheduledKpisExecuted" })
    public void verifyKpiCalculatorDbAssertions(@Input("db_connection_details") final String db_connection_details,
            @Input("db_tables") final String dbTables, @Input("db_table_count_values") final String dbTableCountValues,
            @Input("polling_timeout") final int pollingTimeOut) throws TestException {
        commonTestSteps.verifyKpiCalculatorDbAssertions(db_connection_details, dbTables, dbTableCountValues, pollingTimeOut);
    }

    @TestId(id = TESTID, title = TEST_CASE_TITLE)
    @DataDriven(name = "kpiCalculatorAutoAlignDates")
    @Test(groups = { "KGB", "E_SON", "RET" }, dependsOnMethods = { "verifyKpiCalculatorDbAssertions" })
    public void autoAlignKpiDates(@Input("db_connection_details") final String db_connection_details, @Input("db_tables") final String dbTables)
            throws TestException {
        assertTrue(DatabaseOperator.autoAlignTableDates(db_connection_details, dbTables), DatabaseOperator.getMessage());
    }

    @TestId(id = TESTID, title = TEST_CASE_TITLE)
    @DataDriven(name = RET_DATA)
    @Test(groups = { "KGB", "E_SON", "RET" }, dependsOnMethods = { "autoAlignKpiDates" })
    public void verifyRetExecutedOpenLoop() throws InterruptedException {
        assertTrue(RetAlgorithmRestOperator.triggerRetExecution(ECCD_LB_IP, commonTestSteps.getIngressHost(), true,
                IamAccessRestOperator.TAF_SUPER_USER),
                RetAlgorithmRestOperator.getMessage());
        LOGGER.info("Waiting for {} minute(s) to allow RET open loop execution to be triggered", SLEEP_TIME_TO_ALLOW_RET_TRIGGER);
        TimeUnit.MINUTES.sleep(SLEEP_TIME_TO_ALLOW_RET_TRIGGER);
        assertTrue(
                RetAlgorithmRestOperator.checkRetExecutionSucceeded(ECCD_LB_IP, commonTestSteps.getIngressHost(),
                        IamAccessRestOperator.TAF_SUPER_USER),
                RetAlgorithmRestOperator.getMessage());
    }

    @TestId(id = TESTID, title = TEST_CASE_TITLE)
    @DataDriven(name = "changes", filter = "testCaseId == 'SONP-37761_VerifyCommon'")
    @Test(groups = { "KGB", "E_SON", "CAS" }, dependsOnMethods = { "verifyRetExecutedOpenLoop" })
    public void verifyChangesOpenLoop(@Input("message-pod") final String messagePod, @Input("message-container") final String messageContainer,
            @Input("topic") final String topic, @Input("numExpectedChanges") final int numExpectedChanges, @Input("range") final int range) {
        commonTestSteps.verifyChanges(messagePod, messageContainer, topic, true, RetAlgorithmRestOperator.getSucceededExecutionId(),
                numExpectedChanges, range);
        RetAlgorithmRestOperator.clearSucceededExecutionId();
    }

    @TestId(id = TESTID, title = TEST_CASE_TITLE)
    @DataDriven(name = RET_DATA)
    @Test(groups = { "KGB", "E_SON", "RET" }, dependsOnMethods = { "verifyChangesOpenLoop" })
    public void verifyRetExecutedClosedLoop() throws InterruptedException {
        assertTrue(
                RetAlgorithmRestOperator.triggerRetExecution(ECCD_LB_IP, commonTestSteps.getIngressHost(), false,
                        IamAccessRestOperator.TAF_SUPER_USER),
                RetAlgorithmRestOperator.getMessage());
        LOGGER.info("Waiting for {} minute(s) to allow RET closed loop execution to be triggered", SLEEP_TIME_TO_ALLOW_RET_TRIGGER);
        TimeUnit.MINUTES.sleep(SLEEP_TIME_TO_ALLOW_RET_TRIGGER);
        assertTrue(
                RetAlgorithmRestOperator.checkRetExecutionSucceeded(ECCD_LB_IP, commonTestSteps.getIngressHost(),
                        IamAccessRestOperator.TAF_SUPER_USER),
                RetAlgorithmRestOperator.getMessage());
    }

    @TestId(id = TESTID, title = TEST_CASE_TITLE)
    @DataDriven(name = "changes", filter = "testCaseId == 'SONP-37761_VerifyCommon'")
    @Test(groups = { "KGB", "E_SON", "CAS" }, dependsOnMethods = { "verifyRetExecutedClosedLoop" })
    public void verifyChangesClosedLoop(@Input("message-pod") final String messagePod, @Input("topic") final String topic,
            @Input("message-container") final String messageContainer, @Input("numExpectedChanges") final int numExpectedChanges,
            @Input("range") final int range) {
        commonTestSteps.verifyChanges(messagePod, messageContainer, topic, false, RetAlgorithmRestOperator.getSucceededExecutionId(),
                numExpectedChanges, range);
        RetAlgorithmRestOperator.clearSucceededExecutionId();
    }

    @TestId(id = TESTID, title = TEST_CASE_TITLE)
    @DataDriven(name = "kpiCalculatorViewValuesRet3K", filter = "testCaseId == 'SONP-43077_ScheduleRetAndVerifyProposedChange'")
    @Test(groups = { "KGB", "E_SON", "KPI" }, dependsOnMethods = { "verifyRetExecutedClosedLoop" })
    public void verifyKpiCalculatorDbValueAssertions(@Input("db_connection_details") final String db_connection_details,
            @Input("query_details") final String queryDetails,
            @Input("where_clause_columns") final String whereClauseColumns,
            @Input("where_clause_values") final String whereClauseValues,
            @Input("expected_value") final String expectedValue,
            @Input("polling_timeout") final int pollingTimeOut,
            @Input("external_view_service_name") final String externalViewServiceName) throws TestException {
        final String whereClauseValuesWithReplacedDate = ReplaceDate
                .replaceDateInWhereClauseValues(whereClauseColumns, whereClauseValues, RetAlgorithmRestOperator.getCronExpression());
        commonTestSteps.verifyKpiCalculatorDbValueAssertionsOnView(db_connection_details,
                queryDetails, whereClauseColumns, whereClauseValuesWithReplacedDate,
                expectedValue, pollingTimeOut, externalViewServiceName);
    }

    @TestId(id = TESTID, title = TEST_CASE_TITLE)
    @DataDriven(name = "kpiCalculatorComplexDbCounts3K")
    @Test(groups = { "KGB", "E_SON", "KPI" }, dependsOnMethods = { "verifyChangesClosedLoop" })
    public void verifyKpiCalculatorComplexDbAssertions(@Input("db_connection_details") final String db_connection_details,
            @Input("db_tables") final String dbTables, @Input("db_table_count_values") final String dbTableCountValues,
            @Input("polling_timeout") final int pollingTimeOut)
            throws TestException {
        assertTrue(DatabaseOperator.execute(db_connection_details, dbTables, dbTableCountValues, pollingTimeOut),
                DatabaseOperator.getMessage());
    }

    @AfterMethod(alwaysRun = true)
    public void logAfterMethod(final ITestResult result) {
        commonTestSteps.logAfterMethod(result);
    }

    @AfterClass(alwaysRun = true)
    public void deleteOSS() {
        commonTestSteps.deleteOSS();
    }
}