/*
 *------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT 2022
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

import static com.ericsson.oss.services.eson.test.constants.ret.RetRestConstants.SLEEP_TIME_TO_ALLOW_RET_TRIGGER;
import static org.testng.Assert.assertTrue;

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

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
import com.ericsson.oss.services.eson.test.operators.Ret5GAlgorithmRestOperator;
import com.ericsson.oss.services.eson.test.operators.database.DatabaseOperator;
import com.ericsson.oss.services.eson.test.operators.database.TestException;
import com.ericsson.oss.services.eson.test.teststeps.CommonTestSteps;
import com.ericsson.oss.services.eson.test.util.ReplaceDate;
import com.google.inject.Inject;

/**
 * Schedule RET 5G Algorithm and Verify KPIs.
 */
public class VerifyRet5G extends TafTestBase {

    private static final Logger LOGGER = LoggerFactory.getLogger(VerifyRet5G.class);

    private static final EccdCliHandler ECCD_CLI_HANDLER = new EccdCliHandler(EccdHostGroup.getEccdDrector());

    private static final AtomicBoolean DELETE_OSS = new AtomicBoolean(true);

    // Test Groups
    private static final String KGB = "KGB";
    private static final String E_SON = "E_SON";
    private static final String ENV = "ENV";
    private static final String OSS = "OSS";
    private static final String CM = "CM";
    private static final String PM = "PM";
    private static final String KPI = "KPI";
    private static final String RET_5G = "RET_5G";
    // Data driven references
    private static final String RET_DATA = "ret5gData";
    private static final String CM_DEFINITIONS_5G = "cmDefinitions5G";
    private static final String ENVIRONMENT_FILE = "environment";

    private static final String TEST_CASE_TITLE = "eSON : IDUN-7785_VerifyRet5G - Schedule RET 5G Algorithm and Verify execution.";
    private static final String TESTID = "IDUN-7785_VerifyRet5G";

    private static final String ECCD_LB_IP = DeploymentCliOperator.getLoadbalancerIP(ECCD_CLI_HANDLER);

    @Inject
    private CommonTestSteps commonTestSteps;

    @BeforeMethod(alwaysRun = true)
    public void logBeforeMethod(final Method method) {
        commonTestSteps.logBeforeMethod(method);
    }

    @TestId(id = TESTID, title = TEST_CASE_TITLE)
    @DataDriven(name = ENVIRONMENT_FILE, filter = "testCaseId == 'IDUN-7785_VerifyRet5GSuccessfulInstallation'")
    @Test(groups = { KGB, E_SON, ENV })
    public void setupEnv(@Input("ingressHost") final String host,
            @Input("namespace") final String ns,
            @Input("releaseName") final String relName,
            @Input("installDirectory") final String installDir) {
        LOGGER.info("setupEnv:: host: {}, namespace: {}, release name: {}, install location: {}", host, ns, relName, installDir);
        commonTestSteps.setupEnv(host, ns, relName, installDir);
    }

    @TestId(id = TESTID, title = TEST_CASE_TITLE)
    @DataDriven(name = CM_DEFINITIONS_5G)
    @Test(groups = { KGB, E_SON, CM }, dependsOnMethods = { "setupEnv" })
    public void putCmDefinitions(@Input("cm_definition_file") final String cmDefinitionFile, @Input("message-pod") final String messagePod,
            @Input("message-container") final String messageContainer, @Input("cm_topic") final String topic) {
        LOGGER.info("putCmDefinitions:: cm_definition_file: {}, message-pod: {}, message-container: {}, cm_topic: {}",
                cmDefinitionFile, messagePod, messageContainer, topic);
        commonTestSteps.putCmDefinitions(cmDefinitionFile, messagePod, messageContainer, topic);
    }

    @TestId(id = TESTID, title = TEST_CASE_TITLE)
    @DataDriven(name = CM_DEFINITIONS_5G)
    @Test(groups = { KGB, E_SON, PM }, dependsOnMethods = { "putCmDefinitions" })
    public void putPmDefinitions(@Input("pm_definition_file") final String pmDefinitionFile, @Input("message-pod") final String messagePod,
            @Input("message-container") final String messageContainer, @Input("pm_topic") final String topic) {
        LOGGER.info("putPmDefinitions:: pm_definition_file: {}, message-pod: {}, message-container: {}, pm_topic: {}",
                pmDefinitionFile, messagePod, messageContainer, topic);
        commonTestSteps.putPmDefinitions(pmDefinitionFile, messagePod, messageContainer, topic);
    }

    @TestId(id = TESTID, title = TEST_CASE_TITLE)
    @DataDriven(name = RET_DATA)
    @Test(groups = { KGB, E_SON, KPI }, dependsOnMethods = { "putPmDefinitions" })
    public void putKpiDefinitions(@Input("kpi_definition_file") final String kpiDefinitionFile) {
        LOGGER.info("putKpiDefinitions:: kpi_definition_file: {}", kpiDefinitionFile);
        commonTestSteps.putKpiDefinitions(kpiDefinitionFile);
    }

    @TestId(id = TESTID, title = TEST_CASE_TITLE)
    @DataDriven(name = RET_DATA)
    @Test(groups = { KGB, E_SON, OSS }, dependsOnMethods = { "putKpiDefinitions" })
    public void setupAndVerifyOss(@Input("description") final String description,
            @Input("event_stats_delay") final int event_stats_delay) {
        LOGGER.info("setupAndVerifyOss:: description: {}, events_stats_delay: {}", description, event_stats_delay);
        final String ossPayload = "{\"type\":\"enm\",\"name\":\"stubbed-enm\",\"content\":\"{\\\"enm_ui_host\\\":\\\"stubbed-enm\\\",\\\"enm_ui_username\\\":\\\"eson_user\\\",\\\"enm_ui_password\\\": \\\"enm12admin\\\",\\\"enm_cm_data_loading_cron\\\":\\\"\\\"}\"}";
        commonTestSteps.setupAndVerifyOss(description, event_stats_delay, ossPayload);
    }

    @TestId(id = TESTID, title = TEST_CASE_TITLE)
    @DataDriven(name = RET_DATA)
    @Test(groups = { KGB, E_SON, CM }, dependsOnMethods = { "setupAndVerifyOss" })
    public void verifyCmCountOverRestBeforeFileUpload(@Input("topology_object_names_before_file_upload") final String topologyObjectNames,
            @Input("topology_object_counts_before_file_upload") final String topologyObjectCounts) {
        LOGGER.info(
                "verifyCmCountOverRestBeforeFileUpload:: topology_object_names_before_file_upload: {}, topology_object_counts_before_file_upload: {}",
                topologyObjectNames, topologyObjectCounts);
        commonTestSteps.verifyCmCountsOverRestBeforeFileUpload(topologyObjectNames, topologyObjectCounts);
    }

    @TestId(id = TESTID, title = TEST_CASE_TITLE)
    @DataDriven(name = RET_DATA)
    @Test(groups = { KGB, E_SON, CM }, dependsOnMethods = { "verifyCmCountOverRestBeforeFileUpload" })
    public void verifyPtfFileUpload(@Input("ptfData") final String ptfData) {
        LOGGER.info("verifyPtfFileUpload:: ptfData: {}", ptfData);
        commonTestSteps.verifyPtfFileUpload(ptfData);
    }

    @TestId(id = TESTID, title = TEST_CASE_TITLE)
    @DataDriven(name = RET_DATA)
    @Test(groups = { KGB, E_SON, CM }, dependsOnMethods = { "verifyPtfFileUpload" })
    public void verifyCmCountOverRestAfterFileUpload(@Input("topology_object_names_after_file_upload") final String topologyObjectNames,
            @Input("topology_object_counts_after_file_upload") final String topologyObjectCounts) {
        LOGGER.info(
                "verifyCmCountOverRestAfterFileUpload:: topology_object_names_after_file_upload: {}, topology_object_counts_after_file_upload: {}",
                topologyObjectNames, topologyObjectCounts);
        commonTestSteps.verifyCmCountsOverRestAfterFileUpload(topologyObjectNames, topologyObjectCounts);
    }

    @TestId(id = TESTID, title = TEST_CASE_TITLE)
    @DataDriven(name = "cmDbCounts3K", filter = "testCaseId =='" + TESTID + "'")
    @Test(groups = { KGB, E_SON, CM }, dependsOnMethods = { "verifyPtfFileUpload" })
    public void verifyCmDbAssertions(@Input("db_connection_details") final String db_connection_details, @Input("db_tables") final String dbTables,
            @Input("db_table_count_values") final String dbTableCountValues, @Input("polling_timeout") final int pollingTimeOut)
            throws TestException {
        LOGGER.info("verifyCmDbAssertions:: db_connection_details: {}, db_tables: {}, db_table_count_values: {}, polling_timeout: {}",
                db_connection_details, dbTables, dbTableCountValues, pollingTimeOut);
        commonTestSteps.verifyCmDbAssertions(db_connection_details, dbTables, dbTableCountValues, pollingTimeOut);
        // TODO remove the following once we get Verizon data, update required due to empty timezone column
        LOGGER.info("Fixing nr_cell timezone issue");
        DatabaseOperator.executeUpdate(db_connection_details, "UPDATE nr_cell SET timezone = 'UTC'");
    }

    @TestId(id = TESTID, title = TEST_CASE_TITLE)
    @DataDriven(name = RET_DATA)
    @Test(groups = { KGB, E_SON, PM }, dependsOnMethods = { "verifyCmDbAssertions" })
    public void schedulePmEventsAndStats(@Input("event_stats_delay") final int event_stats_delay) {
        LOGGER.info("schedulePmEventsAndStats:: event_stats_delay: {}", event_stats_delay);
        commonTestSteps.schedulePmEventsAndStats(event_stats_delay);
    }

    @TestId(id = TESTID, title = TEST_CASE_TITLE)
    @DataDriven(name = RET_DATA)
    @Test(groups = { KGB, E_SON, PM }, dependsOnMethods = { "verifyCmDbAssertions" })
    public void verifyPmEventsExecuted(@Input("pm_files_processed") final int pm_files_processed) {
        LOGGER.info("verifyPmEventsExecuted:: pm_files_processed: {}", pm_files_processed);
        commonTestSteps.verifyPmEventsExecuted(pm_files_processed);
    }

    @TestId(id = TESTID, title = TEST_CASE_TITLE)
    @DataDriven(name = "pmEventsDbCounts3K", filter = "testCaseId =='" + TESTID + "'")
    @Test(groups = { KGB, E_SON, PM }, dependsOnMethods = { "verifyPmEventsExecuted" })
    public void verifyPmEventsDbAssertions(@Input("db_connection_details") final String db_connection_details,
            @Input("db_tables") final String dbTables, @Input("db_table_count_values") final String dbTableCountValues,
            @Input("polling_timeout") final int pollingTimeOut) throws TestException {
        LOGGER.info("verifyPmEventsDbAssertions:: db_connection_details: {}, db_tables: {}, db_table_count_values: {}, polling_timeout: {}",
                db_connection_details, dbTables, dbTableCountValues, pollingTimeOut);
        commonTestSteps.verifyPmEventsDbAssertions(db_connection_details, dbTables, dbTableCountValues, pollingTimeOut);
    }

    @TestId(id = TESTID, title = TEST_CASE_TITLE)
    @DataDriven(name = "pmEventsAutoAlignDates5G")
    @Test(groups = { KGB, E_SON, PM }, dependsOnMethods = { "verifyPmEventsDbAssertions" })
    public void autoAlignPmEventsDates(@Input("db_connection_details") final String db_connection_details, @Input("db_tables") final String dbTables)
            throws TestException {
        LOGGER.info("autoAlignPmEventsDates:: db_connection_details: {}, db_tables: {}", db_connection_details, dbTables);
        assertTrue(DatabaseOperator.autoAlignPmEventsTableDates(db_connection_details, dbTables), DatabaseOperator.getMessage());
    }

    @TestId(id = TESTID, title = TEST_CASE_TITLE)
    @DataDriven(name = RET_DATA)
    @Test(groups = { KGB, E_SON, KPI }, dependsOnMethods = { "verifyPmEventsDbAssertions" })
    public void verifyScheduledKpisExecuted(@Input("kpi_verification_file") final String kpiVerificationFile) {
        LOGGER.info("verifyScheduledKpisExecuted:: kpi_verification_file: {}", kpiVerificationFile);
        commonTestSteps.verifyScheduledKpisExecuted(kpiVerificationFile);
    }

    @TestId(id = TESTID, title = TEST_CASE_TITLE)
    @DataDriven(name = "kpiCalculatorDbCounts3K", filter = "testCaseId =='" + TESTID + "'")
    @Test(groups = { KGB, E_SON, KPI }, dependsOnMethods = { "verifyScheduledKpisExecuted" })
    public void verifyKpiCalculatorDbAssertions(@Input("db_connection_details") final String db_connection_details,
            @Input("db_tables") final String dbTables, @Input("db_table_count_values") final String dbTableCountValues,
            @Input("polling_timeout") final int pollingTimeOut) throws TestException {
        LOGGER.info("verifyKpiCalculatorDbAssertions:: db_connection_details: {}, db_tables: {}, db_table_count_values: {}, polling_timeout: {}",
                db_connection_details, dbTables, dbTableCountValues, pollingTimeOut);
        commonTestSteps.verifyKpiCalculatorDbAssertions(db_connection_details, dbTables, dbTableCountValues, pollingTimeOut);
    }

    @TestId(id = TESTID, title = TEST_CASE_TITLE)
    @DataDriven(name = "kpiCalculatorAutoAlignDatesRet5G")
    @Test(groups = { KGB, E_SON, KPI }, dependsOnMethods = { "verifyKpiCalculatorDbAssertions" })
    public void autoAlignKpiDates(@Input("db_connection_details") final String db_connection_details, @Input("db_tables") final String dbTables)
            throws TestException {
        LOGGER.info("autoAlignKpiDates:: db_connection_details: {}, db_tables: {}", db_connection_details, dbTables);
        assertTrue(DatabaseOperator.autoAlignTableDates(db_connection_details, dbTables), DatabaseOperator.getMessage());
    }

    @TestId(id = TESTID, title = TEST_CASE_TITLE)
    @DataDriven(name = RET_DATA)
    @Test(groups = { KGB, E_SON, RET_5G }, dependsOnMethods = { "autoAlignKpiDates" })
    public void verifyRetExecuted() throws InterruptedException {
        LOGGER.info("verifyRetExecuted");
        assertTrue(Ret5GAlgorithmRestOperator.triggerRetExecution(ECCD_LB_IP, commonTestSteps.getIngressHost(), true,
                IamAccessRestOperator.TAF_SUPER_USER),
                Ret5GAlgorithmRestOperator.getMessage());
        LOGGER.info("Waiting for {} minute(s) to allow RET 5G execution to be triggered", SLEEP_TIME_TO_ALLOW_RET_TRIGGER);
        TimeUnit.MINUTES.sleep(SLEEP_TIME_TO_ALLOW_RET_TRIGGER);
        assertTrue(
                Ret5GAlgorithmRestOperator.checkRetExecutionSucceeded(ECCD_LB_IP, commonTestSteps.getIngressHost(),
                        IamAccessRestOperator.TAF_SUPER_USER),
                Ret5GAlgorithmRestOperator.getMessage());
    }

    @TestId(id = TESTID, title = TEST_CASE_TITLE)
    @DataDriven(name = "kpiCalculatorViewValuesRet3K", filter = "testCaseId =='" + TESTID + "'")
    @Test(groups = { KGB, E_SON, KPI }, dependsOnMethods = { "verifyRetExecuted" })
    public void verifyKpiCalculatorDbValueAssertions(@Input("db_connection_details") final String db_connection_details,
            @Input("query_details") final String queryDetails,
            @Input("where_clause_columns") final String whereClauseColumns,
            @Input("where_clause_values") final String whereClauseValues,
            @Input("expected_value") final String expectedValue,
            @Input("polling_timeout") final int pollingTimeOut,
            @Input("external_view_service_name") final String externalViewServiceName) throws TestException {
        LOGGER.info("verifyKpiCalculatorDbValueAssertions:: db_connection_details: {}, query_details: {}, " +
                "where_clause_columns: {}, where_clause_values: {}, expected_value: {}, polling_timeout: {}, " +
                "external_view_service_name: {}",
                db_connection_details, queryDetails,
                whereClauseColumns, whereClauseValues, expectedValue, pollingTimeOut,
                externalViewServiceName);
        final String whereClauseValuesWithReplacedDate = ReplaceDate
                .replaceDateInWhereClauseValues(whereClauseColumns, whereClauseValues, Ret5GAlgorithmRestOperator.getCronExpression());
        commonTestSteps.verifyKpiCalculatorDbValueAssertionsOnView(db_connection_details,
                queryDetails, whereClauseColumns, whereClauseValuesWithReplacedDate,
                expectedValue, pollingTimeOut, externalViewServiceName);
    }

    @TestId(id = TESTID, title = TEST_CASE_TITLE)
    @DataDriven(name = "kpiCalculatorComplexDbCounts3K5G")
    @Test(groups = { KGB, E_SON, KPI }, dependsOnMethods = { "verifyRetExecuted" })
    public void verifyKpiCalculatorComplexDbAssertions(@Input("db_connection_details") final String db_connection_details,
            @Input("db_tables") final String dbTables, @Input("db_table_count_values") final String dbTableCountValues,
            @Input("polling_timeout") final int pollingTimeOut)
            throws TestException {
        LOGGER.info(
                "verifyKpiCalculatorComplexDbAssertions:: db_connection_details: {}, db_tables: {}, db_table_count_values: {}, polling_timeout:{}",
                db_connection_details, dbTables, dbTableCountValues, pollingTimeOut);
        assertTrue(DatabaseOperator.execute(db_connection_details, dbTables, dbTableCountValues, pollingTimeOut),
                DatabaseOperator.getMessage());
    }

    @AfterMethod(alwaysRun = true)
    public void logAfterMethod(final ITestResult result) {
        commonTestSteps.logAfterMethod(result);
        if (!result.isSuccess()) {
            DELETE_OSS.set(false);
        }
    }

    @AfterClass(alwaysRun = true)
    public void deleteOSS() {
        LOGGER.info("deleteOSS");
        if (DELETE_OSS.get()) {
            LOGGER.info("Removing OSS");
            commonTestSteps.deleteOSS();
        } else {
            LOGGER.info("Preserving data for inspection due to failed test(s)");
        }
    }
}