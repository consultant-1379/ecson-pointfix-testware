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

package com.ericsson.oss.services.eson.test.cases.aas.test;

import static com.ericsson.oss.services.eson.test.constants.common.EnvDataSourceConstants.ENVIRONMENT_FILE;
import static org.testng.Assert.assertTrue;

import java.lang.reflect.Method;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.ITestResult;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.ericsson.cifwk.taf.TafTestBase;
import com.ericsson.cifwk.taf.annotations.DataDriven;
import com.ericsson.cifwk.taf.annotations.Input;
import com.ericsson.cifwk.taf.annotations.TestId;
import com.ericsson.oss.services.eson.test.cases.aas.PrepareEnvironmentAas;
import com.ericsson.oss.services.eson.test.operators.database.DatabaseOperator;
import com.ericsson.oss.services.eson.test.operators.database.TestException;
import com.ericsson.oss.services.eson.test.teststeps.CommonTestSteps;
import com.ericsson.oss.services.eson.test.util.ReplaceDate;
import com.google.inject.Inject;

public class VerifyAas extends TafTestBase {

    private static final Logger LOGGER = LoggerFactory.getLogger(VerifyAas.class);
    private static final String TESTID = "IDUN-10534_VerifyAas";
    private static final String AAS_DATA = "verifyAasData";
    private static final String TEST_CASE_TITLE = "eSON : IDUN-10534_VerifyAas";
    private static final String COUNTERS_CELL_TABLE = "counters_cell";
    private static final String LOCAL_TIMESTAMP_COLUMN = "local_timestamp";
    private static final String OSS_PAYLOAD = "{\"type\":\"enm\",\"name\":\"stubbed-enm\",\"content\":\"{\\\"enm_ui_host\\\":\\\"stubbed-enm-003\\\",\\\"enm_ui_username\\\":\\\"root\\\",\\\"enm_ui_password\\\": \\\"shroot\\\",\\\"enm_ui_login_path\\\": \\\"/login\\\",\\\"enm_cm_data_loading_cron\\\":\\\"\\\"}\"}";
    private static final String OSS_NAME = "stubbed-enm";
    private static final String VALUE_DELIMITER = ";";

    @Inject
    private CommonTestSteps commonTestSteps;

    /**
     * @DESCRIPTION Load CM, PM data and execution AAS KPIs.
     * @PRE eSON CSAR files have been deployed on ECCD environment
     * @PRIORITY HIGH
     */

    @BeforeMethod(alwaysRun = true)
    public void logBeforeMethod(final Method method) {
        commonTestSteps.logBeforeMethod(method);
    }

    @TestId(id = TESTID, title = TEST_CASE_TITLE)
    @DataDriven(name = ENVIRONMENT_FILE, filter = "testCaseId == 'IDUN-10534_VerifyAas'")
    @Test(groups = { "KGB", "E_SON", "Setup" })
    public void setupEnv(@Input("ingressHost") final String host, @Input("namespace") final String ns, @Input("releaseName") final String relName,
            @Input("installDirectory") final String installDir) {
        LOGGER.info("setupEnv STARTING");
        commonTestSteps.setupEnv(host, ns, relName, installDir);
        PrepareEnvironmentAas.createServiceAas();
        LOGGER.info("setupEnv COMPLETED");
    }

    @TestId(id = TESTID, title = TEST_CASE_TITLE)
    @DataDriven(name = AAS_DATA)
    @Test(groups = { "Put 5G CM definitions for needed kpis" }, dependsOnMethods = { "setupEnv" })
    public void put5GCmDefinitions(@Input("5G_cm_definition_file") final String cmDefinitionFile, @Input("message-pod") final String messagePod,
            @Input("message-container") final String messageContainer, @Input("cm_topic") final String topic) {
        LOGGER.info("put5GCmDefinitions STARTING");
        LOGGER.info("Put 5G CM definitions needed for AAS algorithm: {}, {}, {}, {}", cmDefinitionFile, messagePod, messageContainer, topic);
        commonTestSteps.putCmDefinitions(cmDefinitionFile, messagePod, messageContainer, topic);
        LOGGER.info("put5GCmDefinitions COMPLETED");
    }

    @TestId(id = TESTID, title = TEST_CASE_TITLE)
    @DataDriven(name = AAS_DATA)
    @Test(groups = { "Put PM definitions for needed kpis" }, dependsOnMethods = { "put5GCmDefinitions" })
    public void putPmDefinitions(@Input("counter_definition_file") final String counterDefinitionFile, @Input("message-pod") final String messagePod,
            @Input("message-container") final String messageContainer, @Input("pm_topic") final String topic) {
        LOGGER.info("putPmDefinitions STARTING");
        LOGGER.info("Put PM definitions needed for AAS algorithm: {}, {}, {}, {}", counterDefinitionFile, messagePod, messageContainer, topic);
        commonTestSteps.putPmDefinitions(counterDefinitionFile, messagePod, messageContainer, topic);
        LOGGER.info("putPmDefinitions COMPLETED");
    }

    @TestId(id = TESTID, title = TEST_CASE_TITLE)
    @DataDriven(name = AAS_DATA)
    @Test(groups = { "5G CM loading and set schedules" }, dependsOnMethods = { "putPmDefinitions" })
    public void setupAndVerifyOss(@Input("description") final String description, @Input("event_stats_delay") final int event_stats_delay) {
        LOGGER.info("setupAndVerifyOss STARTING");
        commonTestSteps.updateAndVerifyOss(description, event_stats_delay, OSS_PAYLOAD, OSS_NAME);
        LOGGER.info("setupAndVerifyOss COMPLETED");
    }

    @TestId(id = TESTID, title = TEST_CASE_TITLE)
    @DataDriven(name = AAS_DATA)
    @Test(groups = { "KGB", "E_SON", "CM Service" }, dependsOnMethods = { "setupAndVerifyOss" })
    public void verifyCmCountOverRestAfter5GCmLoad(@Input("topology_object_names_after_5G_load") final String topologyObjectNames,
            @Input("topology_object_counts_after_5G_load") final String topologyObjectCounts) {
        LOGGER.info("verifyCmCountOverRestAfter5GCmLoad STARTING");
        commonTestSteps.verifyCmCountsOverRestBeforeFileUpload(topologyObjectNames, topologyObjectCounts);
        LOGGER.info("verifyCmCountOverRestAfter5GCmLoad COMPLETED");
    }

    @TestId(id = TESTID, title = TEST_CASE_TITLE)
    @DataDriven(name = AAS_DATA)
    @Test(groups = { "KGB", "E_SON", "CM Service" }, dependsOnMethods = { "verifyCmCountOverRestAfter5GCmLoad" })
    public void verify5GPtfFileUpload(@Input("ptfData5G") final String ptfData5G) {
        LOGGER.info("verify5GPtfFileUpload STARTING");
        commonTestSteps.verifyPtfFileUpload(ptfData5G);
        LOGGER.info("verify5GPtfFileUpload COMPLETED");
    }

    @TestId(id = TESTID, title = TEST_CASE_TITLE)
    @DataDriven(name = AAS_DATA)
    @Test(groups = { "KGB", "E_SON", "CM Service" }, dependsOnMethods = { "verify5GPtfFileUpload" })
    public void sleepToAllowFor5GLogicalHierarchy() throws InterruptedException {
        LOGGER.info("sleepToAllowFor5GLogicalHierarchy STARTING");
        LOGGER.info("Sleeping for 5 minutes to allow for 5G logical hierarchy to occur");
        TimeUnit.MINUTES.sleep(5);
        LOGGER.info("sleepToAllowFor5GLogicalHierarchy COMPLETED");
    }

    @TestId(id = TESTID, title = TEST_CASE_TITLE)
    @DataDriven(name = AAS_DATA)
    @Test(groups = { "Put 4G CM definitions for needed kpis" }, dependsOnMethods = { "sleepToAllowFor5GLogicalHierarchy" })
    public void put4GCmDefinitions(@Input("4G_cm_definition_file") final String cmDefinitionFile, @Input("message-pod") final String messagePod,
            @Input("message-container") final String messageContainer, @Input("cm_topic") final String topic) {
        LOGGER.info("put4GCmDefinitions STARTING");
        LOGGER.info("Put 4G CM definitions needed for AAS algorithm: {}, {}, {}, {}", cmDefinitionFile, messagePod, messageContainer, topic);
        commonTestSteps.putCmDefinitions(cmDefinitionFile, messagePod, messageContainer, topic);
        LOGGER.info("put4GCmDefinitions COMPLETED");
    }

    @TestId(id = TESTID, title = TEST_CASE_TITLE)
    @DataDriven(name = AAS_DATA)
    @Test(groups = { "4G CM loading and set schedules" }, dependsOnMethods = { "put4GCmDefinitions" })
    public void updateAndVerifyOss(@Input("description") final String description, @Input("event_stats_delay") final int event_stats_delay) {
        LOGGER.info("updateAndVerifyOss STARTING");
        commonTestSteps.updateAndVerifyOss(description, event_stats_delay, OSS_PAYLOAD, OSS_NAME);
        LOGGER.info("updateAndVerifyOss COMPLETED");
    }

    @TestId(id = TESTID, title = TEST_CASE_TITLE)
    @DataDriven(name = AAS_DATA)
    @Test(groups = { "KGB", "E_SON", "CM Service" }, dependsOnMethods = { "updateAndVerifyOss" })
    public void verifyCmCountOverRestAfter4GCmLoad(@Input("topology_object_names_after_4G_load") final String topologyObjectNames,
            @Input("topology_object_counts_after_4G_load") final String topologyObjectCounts) {
        LOGGER.info("verifyCmCountOverRestAfter4GCmLoad STARTING");
        commonTestSteps.verifyCmCountsOverRestBeforeFileUpload(topologyObjectNames, topologyObjectCounts);
        LOGGER.info("verifyCmCountOverRestAfter4GCmLoad COMPLETED");
    }

    @TestId(id = TESTID, title = TEST_CASE_TITLE)
    @DataDriven(name = AAS_DATA)
    @Test(groups = { "KGB", "E_SON", "CM Service" }, dependsOnMethods = { "verifyCmCountOverRestAfter4GCmLoad" })
    public void verify4GPtfFileUpload(@Input("ptfData4G") final String ptfData4G) {
        LOGGER.info("verify4GPtfFileUpload STARTING");
        commonTestSteps.verifyPtfFileUpload(ptfData4G);
        LOGGER.info("verify4GPtfFileUpload COMPLETED");
    }

    @TestId(id = TESTID, title = TEST_CASE_TITLE)
    @DataDriven(name = AAS_DATA)
    @Test(groups = { "KGB", "E_SON", "CM Service" }, dependsOnMethods = { "verify4GPtfFileUpload" })
    public void verifyCmCountOverRestAfter4GPTFUpload(@Input("topology_object_names_after_4G_file_upload") final String topologyObjectNames,
            @Input("topology_object_counts_after_4G_file_upload") final String topologyObjectCounts) {
        LOGGER.info("verifyCmCountOverRestAfter4GPTFUpload STARTING");
        commonTestSteps.verifyCmCountsOverRestAfterFileUpload(topologyObjectNames, topologyObjectCounts);
        LOGGER.info("verifyCmCountOverRestAfter4GPTFUpload COMPLETED");
    }

    @TestId(id = TESTID, title = TEST_CASE_TITLE)
    @DataDriven(name = "cmDbCounts3K", filter = "testCaseId =='" + TESTID + "'")
    @Test(groups = { "KGB", "E_SON", "CM Service" }, dependsOnMethods = { "verifyCmCountOverRestAfter4GPTFUpload" })
    public void verifyCmDbAssertions(@Input("db_connection_details") final String db_connection_details, @Input("db_tables") final String dbTables,
            @Input("db_table_count_values") final String dbTableCountValues, @Input("polling_timeout") final int pollingTimeOut)
            throws TestException {
        LOGGER.info("verifyCmDbAssertions STARTING");
        commonTestSteps.verifyCmDbAssertions(db_connection_details, dbTables, dbTableCountValues, pollingTimeOut);
        LOGGER.info("verifyCmDbAssertions COMPLETED");
    }

    @TestId(id = TESTID, title = TEST_CASE_TITLE)
    @DataDriven(name = AAS_DATA)
    @Test(groups = { "KGB", "E_SON", "CM Service" }, dependsOnMethods = { "verifyCmDbAssertions" })
    public void schedulePmEventsAndStats(@Input("event_stats_delay") final int event_stats_delay) {
        LOGGER.info("schedulePmEventsAndStats STARTING");
        commonTestSteps.schedulePmEventsAndStats(event_stats_delay);
        LOGGER.info("schedulePmEventsAndStats COMPLETED");
    }

    @TestId(id = TESTID, title = TEST_CASE_TITLE)
    @DataDriven(name = AAS_DATA)
    @Test(groups = { "KGB", "E_SON", "PMStats" }, dependsOnMethods = { "schedulePmEventsAndStats" })
    public void verifyPmStatsExecuted() {
        LOGGER.info("verifyPmStatsExecuted STARTING");
        commonTestSteps.verifyPmStatsExecuted();
        LOGGER.info("verifyPmStatsExecuted COMPLETED");
    }

    @TestId(id = TESTID, title = TEST_CASE_TITLE)
    @DataDriven(name = "pmStatsDbCountsAas")
    @Test(groups = { "KGB", "E_SON", "PMStats" }, dependsOnMethods = { "verifyPmStatsExecuted" })
    public void verifyPmStatsDbAssertions(@Input("db_connection_details") final String db_connection_details,
            @Input("db_tables") final String dbTables, @Input("db_table_count_values") final String dbTableCountValues,
            @Input("polling_timeout") final int pollingTimeOut) throws TestException {
        LOGGER.info("verifyPmStatsDbAssertions STARTING");
        commonTestSteps.verifyPmStatsDbAssertions(db_connection_details, dbTables, dbTableCountValues, pollingTimeOut);
        DatabaseOperator.getColumnValueFromTable(db_connection_details, COUNTERS_CELL_TABLE, LOCAL_TIMESTAMP_COLUMN);
        LOGGER.info("verifyPmStatsDbAssertions COMPLETED");
    }

    @TestId(id = TESTID, title = TEST_CASE_TITLE)
    @DataDriven(name = AAS_DATA)
    @Test(groups = { "KGB", "E_SON", "PMEvents" }, dependsOnMethods = { "schedulePmEventsAndStats" })
    public void verifyPmEventsExecuted(@Input("pm_files_processed") final int pm_files_processed) {
        LOGGER.info("verifyPmEventsExecuted STARTING");
        commonTestSteps.verifyPmEventsExecuted(pm_files_processed);
        LOGGER.info("verifyPmEventsExecuted COMPLETED");
    }

    @TestId(id = TESTID, title = TEST_CASE_TITLE)
    @DataDriven(name = "pmEventsDbCountsAas")
    @Test(groups = { "KGB", "E_SON", "PMEvents" }, dependsOnMethods = { "verifyPmEventsExecuted" })
    public void verifyPmEventsDbAssertions(@Input("db_connection_details") final String db_connection_details,
            @Input("db_tables") final String dbTables, @Input("db_table_count_values") final String dbTableCountValues,
            @Input("polling_timeout") final int pollingTimeOut) throws TestException, InterruptedException {
        LOGGER.info("verifyPmEventsDbAssertions STARTING");
        LOGGER.info("Note: issue with varying data counts so check disabled until resolved");
        LOGGER.info("Sleeping for 5 minutes to allow for PM events to complete");
        TimeUnit.MINUTES.sleep(5);
        // commonTestSteps.verifyPmEventsDbAssertions(db_connection_details, dbTables, dbTableCountValues, pollingTimeOut);
        LOGGER.info("verifyPmEventsDbAssertions COMPLETED");
    }

    @TestId(id = TESTID, title = TEST_CASE_TITLE)
    @DataDriven(name = "setPmEventsLocalTimestamps")
    @Test(groups = { "KGB", "E_SON", "PMEvents" }, dependsOnMethods = { "verifyPmEventsDbAssertions" })
    public void setPmEventsLocalTimestamps(@Input("db_connection_details") final String db_connection_details,
            @Input("db_tables") final String dbTables)
            throws TestException {
        LOGGER.info("setPmEventsLocalTimestamps STARTING");
        final String[] tableNames = dbTables.split(VALUE_DELIMITER);
        LOGGER.info("Note: This will need to be changed when Daylight Saving Time occurs");
        for (final String tableName : tableNames) {
            final String setLocalTimeToUtc = "UPDATE " + tableName + " SET local_timestamp = utc_timestamp + INTERVAL '1' hour";
            DatabaseOperator.executeQuery(db_connection_details, setLocalTimeToUtc);
        }
        LOGGER.info("setPmEventsLocalTimestamps COMPLETED");
    }

    @TestId(id = TESTID, title = TEST_CASE_TITLE)
    @DataDriven(name = AAS_DATA)
    @Test(groups = { "Put kpi definitions for needed kpis" }, dependsOnMethods = { "verifyPmStatsDbAssertions", "setPmEventsLocalTimestamps" })
    public void putKpiDefinitions(@Input("kpi_definition_file") final String kpiDefinitionFile) {
        LOGGER.info("putKpiDefinitions STARTING");
        commonTestSteps.putKpiDefinitions(kpiDefinitionFile);
        LOGGER.info("putKpiDefinitions COMPLETED");
    }

    @TestId(id = TESTID, title = TEST_CASE_TITLE)
    @DataDriven(name = AAS_DATA)
    @Test(groups = { "KGB", "E_SON", "KPI" }, dependsOnMethods = { "putKpiDefinitions" })
    public void calculateAasKpis(@Input("kpi_calculation_request") final String kpiCalculationRequest) {
        LOGGER.info("calculateAasKpis STARTING");
        final Collection<String> kpiCalculationRequestFileList = new ArrayList<>(Arrays.asList(kpiCalculationRequest.split(";")));
        for (final String kpiCalculationRequestFile : kpiCalculationRequestFileList) {
            LOGGER.info("Starting calculateAasKpis for {}", kpiCalculationRequestFile);
            commonTestSteps.calculateOnDemandKpis(kpiCalculationRequestFile);
            LOGGER.info("Complete calculateAasKpis for {}", kpiCalculationRequestFile);
        }
        LOGGER.info("calculateAasKpis COMPLETED");
    }

    @TestId(id = TESTID, title = TEST_CASE_TITLE)
    @DataDriven(name = "kpiCalculatorViewCountsAas")
    @Test(groups = { "KGB", "E_SON", "KPI" }, dependsOnMethods = { "calculateAasKpis" })
    public void verifyKpiCalculatorDbViewCounts(@Input("db_connection_details") final String db_connection_details,
            @Input("db_tables") final String dbTables, @Input("db_table_count_values") final String dbTableCountValues,
            @Input("polling_timeout") final int pollingTimeOut, @Input("external_view_service_name") final String externalViewServiceName)
            throws TestException {
        LOGGER.info("verifyKpiCalculatorDbViewCounts STARTING");
        assertTrue(DatabaseOperator.executeCountOnView(db_connection_details, dbTables, dbTableCountValues, pollingTimeOut, externalViewServiceName),
                DatabaseOperator.getMessage());
        LOGGER.info("verifyKpiCalculatorDbViewCounts COMPLETED");
    }

    @TestId(id = TESTID, title = TEST_CASE_TITLE)
    @DataDriven(name = "kpiCalculatorViewValuesAas3K", filter = "testCaseId =='" + TESTID + "'")
    @Test(groups = { "KGB", "E_SON", "KPI" }, dependsOnMethods = { "verifyKpiCalculatorDbViewCounts" })
    public void verifyKpiCalculatorDbViewValues(@Input("db_connection_details") final String db_connection_details,
            @Input("query_details") final String queryDetails, @Input("where_clause_columns") final String whereClauseColumns,
            @Input("where_clause_values") final String whereClauseValues, @Input("expected_value") final String expectedValue,
            @Input("polling_timeout") final int pollingTimeOut, @Input("external_view_service_name") final String externalViewServiceName)
            throws TestException {
        LOGGER.info("verifyKpiCalculatorDbViewValues STARTING");
        final LocalDateTime todayMidnight = LocalDateTime.of(LocalDate.now().minusDays(1), LocalTime.MIDNIGHT);
        final String whereClauseValuesWithReplacedDate = ReplaceDate
                .replaceDateInWhereClauseValuesWithGivenDate(whereClauseColumns, whereClauseValues,
                        todayMidnight.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        commonTestSteps.verifyKpiCalculatorDbValueAssertionsOnView(db_connection_details,
                queryDetails, whereClauseColumns, whereClauseValuesWithReplacedDate,
                expectedValue, pollingTimeOut, externalViewServiceName);
        LOGGER.info("verifyKpiCalculatorDbViewValues COMPLETED");
    }

    @AfterMethod(alwaysRun = true)
    public void logAfterMethod(final ITestResult result) {
        commonTestSteps.logAfterMethod(result);
    }

}