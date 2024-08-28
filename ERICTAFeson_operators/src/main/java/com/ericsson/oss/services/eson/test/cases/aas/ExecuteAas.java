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

package com.ericsson.oss.services.eson.test.cases.aas;

import java.lang.reflect.Method;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

import com.ericsson.cifwk.taf.TafTestBase;
import com.ericsson.cifwk.taf.annotations.DataDriven;
import com.ericsson.cifwk.taf.annotations.Input;
import com.ericsson.cifwk.taf.annotations.TestId;
import com.ericsson.oss.services.eson.test.operators.database.DatabaseOperator;
import com.ericsson.oss.services.eson.test.operators.database.TestException;
import com.ericsson.oss.services.eson.test.teststeps.CommonTestSteps;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.ITestResult;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static com.ericsson.oss.services.eson.test.constants.common.EnvDataSourceConstants.ENVIRONMENT_FILE;

public class ExecuteAas extends TafTestBase {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExecuteAas.class);
    private static final String TESTID = "IDUN-12526_ExecuteAas";
    private static final String AAS_DATA = "executeAasData";
    private static final String TEST_CASE_TITLE = "eSON : IDUN-12526_ExecuteAas";
    private static final String OSS_PAYLOAD = "{\"type\":\"enm\",\"name\":\"stubbed-enm\",\"content\":\"{\\\"enm_ui_host\\\":\\\"stubbed-enm-001\\\",\\\"enm_ui_username\\\":\\\"root\\\",\\\"enm_ui_password\\\": \\\"shroot\\\",\\\"enm_ui_login_path\\\": \\\"/login\\\",\\\"enm_cm_data_loading_cron\\\":\\\"\\\"}\"}";
    private static final String OSS_NAME = "stubbed-enm";
    private static final String VALUE_DELIMITER = ";";
    private Timestamp cm5gDataLoadTimestamp = null;
    private Timestamp cm4gDataLoadTimestamp = null;

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
    @DataDriven(name = ENVIRONMENT_FILE, filter = "testCaseId == 'IDUN-12526_ExecuteAas'")
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
        // remove 1 hour from timestamp to account for daylight savings   
        cm5gDataLoadTimestamp = Timestamp.from(Instant.now().minusSeconds(3600));
        commonTestSteps.updateAndVerifyOss(description, event_stats_delay, OSS_PAYLOAD, OSS_NAME);
        LOGGER.info("setupAndVerifyOss COMPLETED");
    }

    @TestId(id = TESTID, title = TEST_CASE_TITLE)
    @DataDriven(name = AAS_DATA)
    @Test(groups = { "KGB", "E_SON", "PMStats" }, dependsOnMethods = { "setupAndVerifyOss" })
    public void verify5GCmLoadCompleted() {
        LOGGER.info("verify5GCmLoadCompleted STARTING");
        commonTestSteps.verifyLoaderExecuted(cm5gDataLoadTimestamp);
        LOGGER.info("verify5GCmLoadCompleted COMPLETED");
    }

    @TestId(id = TESTID, title = TEST_CASE_TITLE)
    @DataDriven(name = AAS_DATA)
    @Test(groups = { "KGB", "E_SON", "CM Service" }, dependsOnMethods = { "verify5GCmLoadCompleted" })
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
    @Test(groups = { "Put 4G CM definitions for needed kpis" }, dependsOnMethods = { "verify5GPtfFileUpload" })
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
        // remove 1 hour from timestamp to account for daylight savings
        cm4gDataLoadTimestamp = Timestamp.from(Instant.now().minusSeconds(3600));
        commonTestSteps.updateAndVerifyOss(description, event_stats_delay, OSS_PAYLOAD, OSS_NAME);
        LOGGER.info("updateAndVerifyOss COMPLETED");
    }

    @TestId(id = TESTID, title = TEST_CASE_TITLE)
    @DataDriven(name = AAS_DATA)
    @Test(groups = { "KGB", "E_SON", "CM Service" }, dependsOnMethods = { "updateAndVerifyOss" })
    public void verify4GCmLoadCompleted() throws InterruptedException {
        LOGGER.info("verify4GCmLoadCompleted STARTING");
        commonTestSteps.verifyLoaderExecuted(cm4gDataLoadTimestamp);
        LOGGER.info("verify4GCmLoadCompleted COMPLETED");
    }

    @TestId(id = TESTID, title = TEST_CASE_TITLE)
    @DataDriven(name = AAS_DATA)
    @Test(groups = { "KGB", "E_SON", "CM Service" }, dependsOnMethods = { "verify4GCmLoadCompleted" })
    public void verify4GPtfFileUpload(@Input("ptfData4G") final String ptfData4G) {
        LOGGER.info("verify4GPtfFileUpload STARTING");
        commonTestSteps.verifyPtfFileUpload(ptfData4G);
        LOGGER.info("verify4GPtfFileUpload COMPLETED");
    }

    @TestId(id = TESTID, title = TEST_CASE_TITLE)
    @DataDriven(name = AAS_DATA)
    @Test(groups = { "KGB", "E_SON", "CM Service" }, dependsOnMethods = { "verify4GPtfFileUpload" })
    public void sleepToAllowFor4GLogicalHierarchy() throws InterruptedException {
        LOGGER.info("sleepToAllowFor4GLogicalHierarchy STARTING");
        LOGGER.info("Sleeping for 5 minutes to allow for 4G logical hierarchy to occur");
        TimeUnit.MINUTES.sleep(5);
        LOGGER.info("sleepToAllowFor4GLogicalHierarchy COMPLETED");
    }

    @TestId(id = TESTID, title = TEST_CASE_TITLE)
    @DataDriven(name = AAS_DATA)
    @Test(groups = { "KGB", "E_SON", "CM Service" }, dependsOnMethods = { "sleepToAllowFor4GLogicalHierarchy" })
    public void scheduleAndExecutePmFirst8Hours(@Input("event_stats_delay") final int event_stats_delay,
            @Input("db_connection_details") final String pmDbConnectionDetails,
            @Input("where_clause_columns") final String whereClauseColumns,
            @Input("where_clause_values") final String whereClauseValues,
            @Input("times_to_poll_to_verify") final int timesToPollToVerify,
            @Input("events_cell_table") final String eventsCellTable,
            @Input("events_relation_table") final String eventsRelationTable) throws TestException, InterruptedException {
        LOGGER.info("scheduleAndExecutePmFirst8Hours STARTING");

        final String dbTables = eventsCellTable + ";" + eventsRelationTable;
        commonTestSteps.schedulePmEventsAndStats(event_stats_delay);
        LOGGER.info("Sleeping for {} minutes to allow for PM events and stats events schedule to trigger", event_stats_delay);
        TimeUnit.MINUTES.sleep(event_stats_delay);
        commonTestSteps.pollToVerifyPmEventsAreLoadedCompletely(pmDbConnectionDetails, eventsCellTable, whereClauseColumns,
                whereClauseValues, timesToPollToVerify);
        DatabaseOperator.setLocalTimesStamps(pmDbConnectionDetails, dbTables);
        DatabaseOperator.selectNullTimeStampsFromPMEventsDb(pmDbConnectionDetails, eventsCellTable);
        DatabaseOperator.setLocalTimesStamps(pmDbConnectionDetails, dbTables);
        LOGGER.info("scheduleAndExecutePmFirst8Hours COMPLETED");
    }

    @DataDriven(name = AAS_DATA)
    @Test(groups = { "KGB", "E_SON", "CM Service" }, dependsOnMethods = { "scheduleAndExecutePmFirst8Hours" })
    public void scheduleAndExecutePmSecond8Hours(@Input("event_stats_delay") final int event_stats_delay,
            @Input("db_connection_details") final String pmDbConnectionDetails,
            @Input("where_clause_columns") final String whereClauseColumns,
            @Input("where_clause_values") final String whereClauseValues,
            @Input("times_to_poll_to_verify") final int timesToPollToVerify,
            @Input("events_cell_table") final String eventsCellTable,
            @Input("events_relation_table") final String eventsRelationTable) throws TestException, InterruptedException {

        LOGGER.info("scheduleAndExecutePmSecond8Hours STARTING");

        final String dbTables = eventsCellTable + ";" + eventsRelationTable;
        commonTestSteps.schedulePmEvents(event_stats_delay);
        LOGGER.info("Sleeping for {} minutes to allow for PM events schedule to trigger", event_stats_delay);
        TimeUnit.MINUTES.sleep(event_stats_delay);
        commonTestSteps.pollToVerifyPmEventsAreLoadedCompletely(pmDbConnectionDetails, eventsCellTable, whereClauseColumns,
                whereClauseValues, timesToPollToVerify);
        DatabaseOperator.setLocalTimesStamps(pmDbConnectionDetails, dbTables);
        DatabaseOperator.selectNullTimeStampsFromPMEventsDb(pmDbConnectionDetails, eventsCellTable);
        DatabaseOperator.setLocalTimesStamps(pmDbConnectionDetails, dbTables);
        LOGGER.info("scheduleAndExecutePmSecond8Hours COMPLETED");
    }

    @DataDriven(name = AAS_DATA)
    @Test(groups = { "KGB", "E_SON", "CM Service" }, dependsOnMethods = { "scheduleAndExecutePmSecond8Hours" })
    public void scheduleAndExecutePmThird8Hours(@Input("event_stats_delay") final int event_stats_delay,
            @Input("db_connection_details") final String pmDbConnectionDetails,
            @Input("where_clause_columns") final String whereClauseColumns,
            @Input("where_clause_values") final String whereClauseValues,
            @Input("times_to_poll_to_verify") final int timesToPollToVerify,
            @Input("events_cell_table") final String eventsCellTable,
            @Input("events_relation_table") final String eventsRelationTable) throws TestException, InterruptedException {

        LOGGER.info("scheduleAndExecutePmThird8Hours STARTING");

        final String dbTables = eventsCellTable + ";" + eventsRelationTable;
        commonTestSteps.schedulePmEvents(event_stats_delay);
        LOGGER.info("Sleeping for {} minutes to allow for PM events schedule to trigger.", event_stats_delay);
        TimeUnit.MINUTES.sleep(event_stats_delay);
        commonTestSteps.pollToVerifyPmEventsAreLoadedCompletely(pmDbConnectionDetails, eventsCellTable, whereClauseColumns,
                whereClauseValues, timesToPollToVerify);
        DatabaseOperator.setLocalTimesStamps(pmDbConnectionDetails, dbTables);
        DatabaseOperator.selectNullTimeStampsFromPMEventsDb(pmDbConnectionDetails, eventsCellTable);
        DatabaseOperator.setLocalTimesStamps(pmDbConnectionDetails, dbTables);
        LOGGER.info("scheduleAndExecutePmThird8Hours COMPLETED");
    }

    @TestId(id = TESTID, title = TEST_CASE_TITLE)
    @DataDriven(name = AAS_DATA)
    @Test(groups = { "Put kpi definitions for needed kpis" }, dependsOnMethods = { "scheduleAndExecutePmThird8Hours" })
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

    @AfterMethod(alwaysRun = true)
    public void logAfterMethod(final ITestResult result) {
        commonTestSteps.logAfterMethod(result);
    }
}