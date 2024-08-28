/*
 *------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2021 - 2022
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

import static org.testng.Assert.assertTrue;

import java.lang.reflect.Method;
import java.time.Instant;
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
import com.ericsson.oss.services.eson.test.operators.FlmAlgorithmRestOperator;
import com.ericsson.oss.services.eson.test.operators.IamAccessRestOperator;
import com.ericsson.oss.services.eson.test.operators.database.DatabaseOperator;
import com.ericsson.oss.services.eson.test.operators.database.TestException;
import com.ericsson.oss.services.eson.test.teststeps.CommonTestSteps;
import com.ericsson.oss.services.eson.test.util.ReplaceDate;
import com.google.inject.Inject;

public class ScheduleFlmAndVerifyExecution extends TafTestBase {

    private static final String TESTID = "SONP-38799_ScheduleFlmAndVerifyExecution";
    private static final String FLM_DATA = "flmData";
    private static final String TEST_CASE_TITLE = "eSON : SONP-38799_ScheduleFlmAndVerifyExecution";
    private static final String ENVIRONMENT_FILE = "environment";
    private static final String COUNTERS_CELL_TABLE = "counters_cell";
    private static final String LOCAL_TIMESTAMP_COLUMN = "local_timestamp";
    private static final int PA_REVERSAL_TIME_DELAY_IN_MINUTES = 5;
    private static final Logger LOGGER = LoggerFactory.getLogger(ScheduleFlmAndVerifyExecution.class);

    @Inject
    private CommonTestSteps commonTestSteps;

    /**
     * @DESCRIPTION Schedule FLM Algorithm and Verify proposed Change.
     * @PRE eSON CSAR files have been deployed on ECCD environment
     * @PRIORITY HIGH
     */

    @BeforeMethod(alwaysRun = true)
    public void logBeforeMethod(final Method method) {
        commonTestSteps.logBeforeMethod(method);
    }

    @TestId(id = TESTID, title = TEST_CASE_TITLE)
    @DataDriven(name = ENVIRONMENT_FILE, filter = "testCaseId == 'SONP-37570_VerifyFlmSuccessfulInstallation'")
    @Test(groups = { "KGB", "E_SON", "Setup" })
    public void setupEnv(@Input("ingressHost") final String host,
            @Input("namespace") final String ns,
            @Input("releaseName") final String relName,
            @Input("installDirectory") final String installDir) {
        commonTestSteps.setupEnv(host, ns, relName, installDir);
    }

    @TestId(id = TESTID, title = TEST_CASE_TITLE)
    @DataDriven(name = "PaEnvSettings")
    @Test(groups = { "KGB", "E_SON", "FLM" }, dependsOnMethods = { "setupEnv" })
    public void verifyPATriggered(@Input(CommonTestSteps.PA_WINDOW_DURATION_IN_MINUTES) final int pa_window,
            @Input(CommonTestSteps.NUMBER_OF_PA_EXECUTIONS) final int pa_executions,
            @Input(CommonTestSteps.INITIAL_PA_WINDOW_OFFSET_TIME_IN_MINUTES) final int initial_pa_offset,
            @Input(CommonTestSteps.PA_EXECUTION_OFFSET_TIME_IN_MINUTES) final int pa_execution_offset) {

        //kubectl set env flm-pod through commandline or API
        commonTestSteps.paEnvSettings(pa_window, pa_executions, initial_pa_offset, pa_execution_offset);

        try {
            LOGGER.info("Sleeping for 5 minutes to allow FLM pod restart");
            TimeUnit.SECONDS.sleep(300);
        } catch (final InterruptedException e) {
            LOGGER.error("Error waiting for FLM pod restart '{}'", e.getMessage());
        }
    }

    @TestId(id = TESTID, title = TEST_CASE_TITLE)
    @DataDriven(name = FLM_DATA)
    @Test(groups = { "Put kpi definitions for needed kpis" }, dependsOnMethods = { "verifyPATriggered" })
    public void putKpiDefinitions(@Input("kpi_definition_file") final String kpiDefinitionFile) {
        LOGGER.info("Put kpi definitions needed for FLM algorithm");
        commonTestSteps.putKpiDefinitions(kpiDefinitionFile);
    }

    @TestId(id = TESTID, title = TEST_CASE_TITLE)
    @DataDriven(name = FLM_DATA)
    @Test(groups = { "CM loading and set schedules" }, dependsOnMethods = { "putKpiDefinitions" })
    public void setupAndVerifyOss(@Input("description") final String description,
            @Input("event_stats_delay") final int event_stats_delay) {
        final String ossPayload = "{\"type\":\"enm\",\"name\":\"stubbed-enm\",\"content\":\"{\\\"enm_ui_host\\\":\\\"stubbed-enm\\\",\\\"enm_ui_username\\\":\\\"eson_user\\\",\\\"enm_ui_password\\\": \\\"enm12admin\\\",\\\"enm_cm_data_loading_cron\\\":\\\"\\\"}\"}";
        commonTestSteps.setupAndVerifyOss(description, event_stats_delay, ossPayload);
    }

    @TestId(id = TESTID, title = TEST_CASE_TITLE)
    @DataDriven(name = FLM_DATA)
    @Test(groups = { "KGB", "E_SON", "CM Service" }, dependsOnMethods = { "setupAndVerifyOss" })
    public void verifyCmCountOverRestBeforeFileUpload(@Input("topology_object_names_before_file_upload") final String topologyObjectNames,
            @Input("topology_object_counts_before_file_upload") final String topologyObjectCounts) {
        commonTestSteps.verifyCmCountsOverRestBeforeFileUpload(topologyObjectNames, topologyObjectCounts);
    }

    @TestId(id = TESTID, title = TEST_CASE_TITLE)
    @DataDriven(name = FLM_DATA)
    @Test(groups = { "KGB", "E_SON", "CM Service" }, dependsOnMethods = { "verifyCmCountOverRestBeforeFileUpload" })
    public void verifyPtfFileUpload(@Input("ptfData") final String ptfData) {
        commonTestSteps.verifyPtfFileUpload(ptfData);
    }

    @TestId(id = TESTID, title = TEST_CASE_TITLE)
    @DataDriven(name = FLM_DATA)
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
    @DataDriven(name = FLM_DATA)
    @Test(groups = { "KGB", "E_SON", "CM Service" }, dependsOnMethods = { "verifyCmDbAssertions" })
    public void schedulePmEventsAndStats(@Input("event_stats_delay") final int event_stats_delay) {
        LOGGER.info("Scheduling pm events and stats");
        commonTestSteps.schedulePmEventsAndStats(event_stats_delay);
    }

    @TestId(id = TESTID, title = TEST_CASE_TITLE)
    @DataDriven(name = FLM_DATA)
    @Test(groups = { "KGB", "E_SON", "PMEvents" }, dependsOnMethods = { "schedulePmEventsAndStats" })
    public void verifyPmEventsExecuted(@Input("pm_files_processed") final int pm_files_processed) {
        commonTestSteps.verifyPmEventsExecuted(pm_files_processed);
    }

    @TestId(id = TESTID, title = TEST_CASE_TITLE)
    @DataDriven(name = "pmEventsDbCounts3K", filter = "testCaseId =='" + TESTID + "'")
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
    @DataDriven(name = FLM_DATA)
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
        DatabaseOperator.getColumnValueFromTable(db_connection_details, COUNTERS_CELL_TABLE, LOCAL_TIMESTAMP_COLUMN);
    }

    @TestId(id = TESTID, title = TEST_CASE_TITLE)
    @DataDriven(name = FLM_DATA)
    @Test(groups = { "KGB", "E_SON", "KPI" }, dependsOnMethods = { "verifyPmEventsDbAssertions", "verifyPmStatsDbAssertions" })
    public void verifyScheduledKpisExecuted(@Input("kpi_verification_file") final String kpiVerificationFile) {
        LOGGER.info("Requesting calculation of KPIs required for FLM through /calculation endpoint");
        commonTestSteps.verifyScheduledKpisExecuted(kpiVerificationFile);
    }

    @TestId(id = TESTID, title = TEST_CASE_TITLE)
    @DataDriven(name = "kpiCalculatorAutoAlignDatesFlm")
    @Test(groups = { "KGB", "E_SON", "FLM" }, dependsOnMethods = { "verifyScheduledKpisExecuted" })
    public void autoAlignKpiDates(@Input("db_connection_details") final String db_connection_details, @Input("db_tables") final String dbTables)
            throws TestException {
        assertTrue(DatabaseOperator.autoAlignTableDates(db_connection_details, dbTables), DatabaseOperator.getMessage());
    }

    @TestId(id = TESTID, title = TEST_CASE_TITLE)
    @DataDriven(name = FLM_DATA)
    @Test(groups = { "KGB", "E_SON", "FLM" }, dependsOnMethods = { "autoAlignKpiDates" })
    public void verifyFlmExecuted() {
        assertTrue(FlmAlgorithmRestOperator.triggerFlmExecution(commonTestSteps.getEccdLbIp(),
                commonTestSteps.getIngressHost(), IamAccessRestOperator.TAF_SUPER_USER),
                FlmAlgorithmRestOperator.getMessage());
        commonTestSteps.setFlmStartTime(Instant.now());
        assertTrue(FlmAlgorithmRestOperator.checkFlmExecutionSucceeded(commonTestSteps.getEccdLbIp(),
                commonTestSteps.getIngressHost(), IamAccessRestOperator.TAF_SUPER_USER));
    }

    @TestId(id = TESTID, title = TEST_CASE_TITLE)
    @DataDriven(name = "changes", filter = "testCaseId == 'SONP-46092_VerifyFlm'")
    @Test(groups = { "KGB", "E_SON", "CAS" }, dependsOnMethods = { "verifyFlmExecuted" })
    public void verifyChangesClosedLoop(@Input("numExpectedChanges") final int numExpectedChanges,
            @Input("range") final int range, @Input("changeType") final String changeType) {
        commonTestSteps.verifyChanges(false, FlmAlgorithmRestOperator.getSucceededExecutionId(),
                numExpectedChanges, range, changeType);
    }

    @TestId(id = TESTID, title = TEST_CASE_TITLE)
    @DataDriven(name = "kpiPrePopulation")
    @Test(groups = { "KGB", "E_SON", "FLM" }, dependsOnMethods = { "verifyFlmExecuted" })
    public void prePopulateEnv(@Input("db_connection_details") final String dbConnectionDetails,
            @Input("db_table") final String dbTables,
            @Input("columns") final String columns,
            @Input("values") final String values,
            @Input("kpi_names") final String kpiNames,
            @Input("kpi_values") final String kpiValues) throws TestException {
        commonTestSteps.prePopulateEnv(dbConnectionDetails, dbTables, columns, values, kpiNames, kpiValues,
                FlmAlgorithmRestOperator.getSucceededExecutionId());
    }

    @TestId(id = TESTID, title = TEST_CASE_TITLE)
    @DataDriven(name = "kpiPrePopulation")
    @Test(groups = { "KGB", "E_SON", "FLM" }, dependsOnMethods = { "verifyChangesClosedLoop" })
    public void verifyPAReversals(@Input("sectorId") final String sectorId) {
        final Instant endTime = commonTestSteps.getFlmStartTime().plusSeconds(((commonTestSteps.getInitialOffset()
                + commonTestSteps.getPaWindow() + commonTestSteps.getPaExecutionOffset() + PA_REVERSAL_TIME_DELAY_IN_MINUTES) * 60));

        if (Instant.now().getEpochSecond() < endTime.getEpochSecond()) {
            try {
                final long timeDiff = (endTime.getEpochSecond() - Instant.now().getEpochSecond());
                LOGGER.info("Sleeping for '{}' seconds waiting for PA reversal", timeDiff);
                TimeUnit.SECONDS.sleep(timeDiff);
            } catch (final InterruptedException e) {
                LOGGER.error("Error waiting for PA reversals to finish '{}'", e.getMessage());
            }
        }
        assertTrue(FlmAlgorithmRestOperator.checkPAReversionElementSucceeded(commonTestSteps.getEccdLbIp(),
                commonTestSteps.getIngressHost(), IamAccessRestOperator.TAF_SUPER_USER, sectorId),
                FlmAlgorithmRestOperator.getMessage());
    }

    @TestId(id = TESTID, title = TEST_CASE_TITLE)
    @DataDriven(name = "kpiCalculatorViewCounts3K")
    @Test(groups = { "KGB", "E_SON", "KPI" }, dependsOnMethods = { "verifyFlmExecuted" })
    public void verifyKpiCalculatorDbAssertions(@Input("db_connection_details") final String db_connection_details,
            @Input("db_tables") final String dbTables,
            @Input("db_table_count_values") final String dbTableCountValues,
            @Input("polling_timeout") final int pollingTimeOut,
            @Input("external_view_service_name") final String externalViewServiceName) throws TestException {
        assertTrue(DatabaseOperator.executeCountOnView(db_connection_details, dbTables, dbTableCountValues, pollingTimeOut,
                externalViewServiceName), DatabaseOperator.getMessage());
    }

    @TestId(id = TESTID, title = TEST_CASE_TITLE)
    @DataDriven(name = "kpiCalculatorViewValues3K")
    @Test(groups = { "KGB", "E_SON", "KPI" }, dependsOnMethods = { "verifyKpiCalculatorDbAssertions" })
    public void verifyKpiCalculatorDbValueAssertions(@Input("db_connection_details") final String db_connection_details,
            @Input("query_details") final String queryDetails,
            @Input("where_clause_columns") final String whereClauseColumns,
            @Input("where_clause_values") final String whereClauseValues,
            @Input("expected_value") final String expectedValue,
            @Input("polling_timeout") final int pollingTimeOut,
            @Input("external_view_service_name") final String externalViewServiceName) throws TestException {
        final String whereClauseValuesWithReplacedDate = ReplaceDate
                .replaceDateInWhereClauseValues(whereClauseColumns, whereClauseValues, FlmAlgorithmRestOperator.getCronExpression());
        commonTestSteps.verifyKpiCalculatorDbValueAssertionsOnView(db_connection_details,
                queryDetails, whereClauseColumns, whereClauseValuesWithReplacedDate,
                expectedValue, pollingTimeOut, externalViewServiceName);
    }

    @TestId(id = TESTID, title = TEST_CASE_TITLE)
    @DataDriven(name = "flmOptimizationsLbqCounts")
    @Test(groups = { "KGB", "E_SON", "FLM" }, dependsOnMethods = { "verifyFlmExecuted" })
    public void verifyFlmOptimizationsDbAssertions(
            @Input("db_connection_details") final String db_connection_details,
            @Input("query_table") final String queryTable,
            @Input("expected_value") final String expectedValue,
            @Input("where_clause") final String whereClause,
            @Input("polling_timeout") final int pollingTimeOut) throws TestException {
        LOGGER.info("verify count of non-empty(sourceCellFdn !='') and empty(sourceCellFdn ='') LBQs");
        assertTrue(
                DatabaseOperator.executeQueryWithWhereClauseAndAssertTables(db_connection_details, queryTable,
                        expectedValue, pollingTimeOut, whereClause),
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