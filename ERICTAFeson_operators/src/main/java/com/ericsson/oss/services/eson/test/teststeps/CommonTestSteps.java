/*
 *------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2020 - 2022
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *------------------------------------------------------------------------------
 */

package com.ericsson.oss.services.eson.test.teststeps;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.lang.reflect.Method;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.ITestResult;
import org.testng.Reporter;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.ericsson.cifwk.taf.annotations.Input;
import com.ericsson.cifwk.taf.annotations.TestStep;
import com.ericsson.oss.services.eson.test.cases.common.kafka.KafkaPredicatesHelper;
import com.ericsson.oss.services.eson.test.eccd.EccdHostGroup;
import com.ericsson.oss.services.eson.test.handlers.EccdCliHandler;
import com.ericsson.oss.services.eson.test.handlers.EccdResthandler;
import com.ericsson.oss.services.eson.test.operators.CasRestOperator;
import com.ericsson.oss.services.eson.test.operators.CmServiceRestOperator;
import com.ericsson.oss.services.eson.test.operators.DeploymentCliOperator;
import com.ericsson.oss.services.eson.test.operators.IamAccessRestOperator;
import com.ericsson.oss.services.eson.test.operators.KafkaOperator;
import com.ericsson.oss.services.eson.test.operators.KpiServiceRestOperator;
import com.ericsson.oss.services.eson.test.operators.OssRepositoryRestOperator;
import com.ericsson.oss.services.eson.test.operators.database.DatabaseOperator;
import com.ericsson.oss.services.eson.test.operators.database.TestException;
import com.ericsson.oss.services.eson.test.util.DateTimeUtility;
import com.google.inject.Inject;

public class CommonTestSteps {

    private static final String ERIC_PM_EVENTS_PROCESSOR_ER_CONTAINER = "eric-pm-events-processor-er";
    private static final String ERIC_PM_STATS_PROCESSOR_ER_CONTAINER = "eric-pm-stats-processor-er";
    private static final String ERIC_CM_LOADER_ER_CONTAINER = "eric-cm-loader-er";

    private static final String ERIC_CM_TOPOLOGY_MODEL_SN_CONTAINER = "eric-cm-topology-model-sn";
    // Pa Settings env
    private static final String ERIC_SON_FREQUENCY_LAYER_MANAGER_CONTAINER = "eric-son-frequency-layer-manager";
    public static final String PA_WINDOW_DURATION_IN_MINUTES = "PA_WINDOW_DURATION_IN_MINUTES";
    public static final String NUMBER_OF_PA_EXECUTIONS = "NUMBER_OF_PA_EXECUTIONS";
    public static final String INITIAL_PA_WINDOW_OFFSET_TIME_IN_MINUTES = "INITIAL_PA_WINDOW_OFFSET_TIME_IN_MINUTES";
    public static final String PA_EXECUTION_OFFSET_TIME_IN_MINUTES = "PA_EXECUTION_OFFSET_TIME_IN_MINUTES";

    private static final String COMPLETED_LOGICAL_HIERARCHY = "Completed logical hierarchy creation for logical hierarchy scheduler";
    private static final String PM_STATS_STARTING_LOADING_SEARCH_STRING = "Starting loading of PM stats data from";
    private static final String PM_STATS_FINISHED_LOADING_SEARCH_STRING = "Finished loading of PM stats data from";

    private static final String CM_LOADER_FINISHED_LOADING_SEARCH_STRING = "CM oss data removal with ossId";

    private static final String PM_EVENTS_STARTING_LOADING_SEARCH_STRING = "Starting loading of PM Event data";
    private static final String PM_EVENTS_FILES_SENT_FOR_PARSING_SEARCH_STRING = "files for PM event parsing";
    private static final String KPI_KUBECTL_HOURLY_SEARCH_FINISHED_STRING = "All defined KPIs with default filter have been calculated for aggregation period: 60";
    private static final String KPI_KUBECTL_DAILY_SEARCH_FINISHED_STRING = "All defined KPIs with default filter have been calculated for aggregation period: 1440";
    private static final String FINISHED_STATE = "FINISHED";
    private static final Logger LOGGER = LoggerFactory.getLogger(CommonTestSteps.class);

    private final EccdCliHandler eccdHandler = new EccdCliHandler(EccdHostGroup.getEccdDrector());
    private final String eccdLbIp = DeploymentCliOperator.getLoadbalancerIP(eccdHandler);

    private String ingressHost;
    private String namespace;
    private String releaseName;
    private String installDirectory;

    private int paWindow;
    private int noOfPaExecutions;
    private int initialOffset;
    private int paExecutionOffset;
    private Instant flmStartTime;

    @Inject
    private CmServiceRestOperator cmOperator;

    @TestStep(id = StepIds.SETUP_ENV)
    @Test(groups = { "KGB", "E_SON", "Setup" })
    public void setupEnv(@Input("ingressHost") final String host, @Input("namespace") final String ns, @Input("releaseName") final String relName,
            @Input("installDirectory") final String installDir) {
        ingressHost = System.getProperty("ingressHost", host);
        namespace = System.getProperty("namespace", ns);
        installDirectory = System.getProperty("install_dir", installDir);
        releaseName = System.getProperty("release_name", relName);
        Reporter.log(String.format("Host is %s and Load Balancer address is %s", host, eccdLbIp));
    }

    @TestStep(id = StepIds.PA_ENV_SETTINGS)
    @Test(groups = { "KGB", "E_SON", "Setup" })
    public void paEnvSettings(final int pa_window, final int pa_executions, final int initial_pa_offset, final int pa_execution_offset) {
        paWindow = pa_window;
        noOfPaExecutions = pa_executions;
        initialOffset = initial_pa_offset;
        paExecutionOffset = pa_execution_offset;

        final Map<String, String> envPAVariables = new HashMap<>();
        envPAVariables.put(PA_WINDOW_DURATION_IN_MINUTES, String.valueOf(paWindow));
        envPAVariables.put(NUMBER_OF_PA_EXECUTIONS, String.valueOf(noOfPaExecutions));
        envPAVariables.put(INITIAL_PA_WINDOW_OFFSET_TIME_IN_MINUTES, String.valueOf(initialOffset));
        envPAVariables.put(PA_EXECUTION_OFFSET_TIME_IN_MINUTES, String.valueOf(paExecutionOffset));

        assertTrue(DeploymentCliOperator.setEnvForDeployment(namespace, eccdHandler, ERIC_SON_FREQUENCY_LAYER_MANAGER_CONTAINER, envPAVariables));
    }

    @TestStep(id = StepIds.PRE_POPULATE_ENV)
    @Test(groups = { "KGB", "E_SON", "Setup" })
    public void prePopulateEnv(@Input("db_connection_details") final String dbConnectionDetails, @Input("db_table") final String dbTables,
            @Input("columns") final String columns, @Input("values") final String values, @Input("kpi_names") final String kpiNames,
            @Input("kpi_values") final String kpiValues, final String executionId) throws TestException {
        LOGGER.info("Pre-Populate Environment");
        final int ossId = OssRepositoryRestOperator.getIdOfConfiguredOssByName(eccdLbIp, ingressHost, "stubbed-enm",
                IamAccessRestOperator.TAF_SUPER_USER);
        final Instant UTC_TIME_STAMP = getFlmStartTime().plusSeconds((getInitialOffset() + (getPaWindow() / 2)) * 60);
        assertTrue(
                DatabaseOperator.insertItems(dbConnectionDetails, dbTables, columns, values, kpiNames, kpiValues, UTC_TIME_STAMP, ossId, executionId),
                DatabaseOperator.getMessage());
    }

    @Test(groups = { "Put kpi definitions for needed kpis" }, dependsOnGroups = { "Setup" })
    @TestStep(id = StepIds.PUT_KPI_DEFINITIONS)
    public void putKpiDefinitions(@Input("kpi_definition_file") final String kpiDefinitionFile) {
        LOGGER.info("Put kpi definitions needed to verify KPI service");
        assertTrue(KpiServiceRestOperator.removeCalculationFrequencyAndPutKpiDefinitions(ingressHost, eccdLbIp, kpiDefinitionFile,
                IamAccessRestOperator.TAF_SUPER_USER), EccdResthandler.getMessage());
    }

    @Test(groups = { "Put required CM Elements" })
    @TestStep(id = StepIds.POST_CM_DEFINITIONS)
    public void putCmDefinitions(@Input("cm_definition_file") final String cmDefinitionFile, @Input("message-pod") final String messagePod,
            @Input("message-container") final String messageContainer, @Input("topic") final String topic) {
        LOGGER.info("Put CM definitions needed to verify KPI service");
        assertTrue(cmOperator.postRequiredCmElements(ingressHost, eccdLbIp, cmDefinitionFile, IamAccessRestOperator.TAF_SUPER_USER),
                EccdResthandler.getMessage());
        assertTrue(new KafkaOperator(eccdHandler).verifyMessagesOnTopic(messagePod, messageContainer, namespace, topic,
                KafkaPredicatesHelper.getCmMediationPredicate()));
    }

    @Test(groups = { "Reset CM Collection auto increment id column counter" })
    @TestStep(id = StepIds.RESET_COLLECTION_ID_COUNTER)
    public void resetCmCollectionIdCounter(@Input("db_connection_details") final String db_connection_details) throws TestException {
        LOGGER.info("Reset CM Collections ID auto increment column counter needed to verify TopologyCollection");
        DatabaseOperator.executeResetAutoIncrementCounter(db_connection_details, "COLLECTIONS", "ID");
    }

    @Test(groups = { "Post CM Collection" })
    @TestStep(id = StepIds.POST_CM_COLLECTION)
    public void postCmCollections(@Input("cm_collection_definition_file") final String cmCollectionDefinitionFileOne) {
        LOGGER.info("Post CM Collections needed to verify TopologyCollection");
        assertTrue(cmOperator.postCmCollection(ingressHost, eccdLbIp, cmCollectionDefinitionFileOne, IamAccessRestOperator.TAF_SUPER_USER),
                EccdResthandler.getMessage());
    }

    @Test(groups = { "Get CM Collection" }, dependsOnGroups = { "Post CM Collection" })
    @TestStep(id = StepIds.GET_CM_COLLECTION)
    public void getCmCollection(@Input("cm_collection_name") final String cmCollectionName) {
        LOGGER.info("Get CM Collection needed to verify TopologyCollection");
        final String expectedOutput = String.format("\"collectionName\":\"%s\"", cmCollectionName);
        assertTrue(
                cmOperator.getCmCollection(ingressHost, eccdLbIp, "?name=" + cmCollectionName, expectedOutput, IamAccessRestOperator.TAF_SUPER_USER),
                EccdResthandler.getMessage());
    }

    @Test(groups = { "Get all CM Collections" }, dependsOnGroups = { "Post CM Collection" })
    @TestStep(id = StepIds.GET_CM_COLLECTIONS)
    public void getAllCmCollections() {
        LOGGER.info("Get all CM Collections needed to verify TopologyCollections");
        final String expectedOutputOne = "\"collectionName\":\"ecson-trxpoint-filter-group\"";
        final String expectedOutputTwo = "\"collectionName\":\"ecson-cell-filter-group\"";
        assertTrue(cmOperator.getAllCmCollections(ingressHost, eccdLbIp, expectedOutputOne, expectedOutputTwo, IamAccessRestOperator.TAF_SUPER_USER),
                EccdResthandler.getMessage());
    }

    @Test(groups = { "Evaluate CM Collection" }, dependsOnGroups = { "Post CM Collection" })
    @TestStep(id = StepIds.EVALUATE_CM_COLLECTION)
    public void evaluateCmCollection(@Input("cm_collection_name") final String collectionName) {
        LOGGER.info("Evaluate CM Collection needed to verify TopologyCollection");
        assertTrue(cmOperator.evaluateCmCollection(ingressHost, eccdLbIp, "/evaluate?name=" + collectionName, IamAccessRestOperator.TAF_SUPER_USER),
                EccdResthandler.getMessage());
    }

    @Test(groups = { "Update CM Collection" }, dependsOnGroups = { "Post CM Collection", "Get CM Collection", "Get all CM Collections",
            "Evaluate CM Collection" })
    @TestStep(id = StepIds.UPDATE_CM_COLLECTION)
    public void updateCmCollection(@Input("cm_collection_name") final String collectionName,
            @Input("cm_collection_definition_file") final String cmCollectionDefinitionFile) {
        LOGGER.info("Update CM Collection needed to verify TopologyCollection");
        assertTrue(cmOperator.updateCmCollection(ingressHost, eccdLbIp, "?name=" + collectionName, cmCollectionDefinitionFile,
                IamAccessRestOperator.TAF_SUPER_USER), EccdResthandler.getMessage());
    }

    @Test(groups = { "Delete CM Collection" }, dependsOnGroups = { "Post CM Collection", "Get CM Collection", "Get all CM Collections",
            "Evaluate CM Collection", "Update CM Collection" })
    @TestStep(id = StepIds.DELETE_CM_COLLECTION)
    public void deleteCmCollection(@Input("cm_collection_name") final String collectionName) {
        LOGGER.info("Delete CM Collection needed to verify TopologyCollection");
        assertTrue(cmOperator.deleteCmCollection(ingressHost, eccdLbIp, "?name=" + collectionName, IamAccessRestOperator.TAF_SUPER_USER),
                EccdResthandler.getMessage());
    }

    @Test(groups = { "Put PM definitions for needed kpis" })
    @TestStep(id = StepIds.PUT_PM_DEFINITIONS)
    public void putPmDefinitions(@Input("counter_definition_file") final String counterDefinitionFile, @Input("message-pod") final String messagePod,
            @Input("message-container") final String messageContainer, @Input("topic") final String topic) {
        LOGGER.info("Put counter definitions needed to verify KPI service");
        assertTrue(KpiServiceRestOperator.postCountersDefinitions(ingressHost, eccdLbIp, counterDefinitionFile, IamAccessRestOperator.TAF_SUPER_USER),
                EccdResthandler.getMessage());
        assertTrue(new KafkaOperator(eccdHandler).verifyMessagesOnTopic(messagePod, messageContainer, namespace, topic,
                KafkaPredicatesHelper.getPmMediationPredicate()));
    }

    @Test(groups = { "CM loading and set schedules" })
    @TestStep(id = StepIds.SETUP_AND_VERIFY_OSS)
    public void setupAndVerifyOss(@Input("description") final String description, @Input("event_stats_delay") final int event_stats_delay,
            @Input("oss_payload") final String ossPayload) {
        Reporter.log(String.format("Data details %s", description));
        LOGGER.info("Configure new OSS and schedule immediate CM Data Loading, No PM-Events-Stats Scheduling");
        assertTrue(OssRepositoryRestOperator.configureOSS(ossPayload, eccdLbIp, ingressHost, IamAccessRestOperator.TAF_SUPER_USER),
                EccdResthandler.getMessage());
        assertTrue(OssRepositoryRestOperator.checkConfiguredOSS(eccdLbIp, ingressHost, IamAccessRestOperator.TAF_SUPER_USER),
                EccdResthandler.getMessage());
    }

    @Test(groups = { "CM loading and set schedules" })
    @TestStep(id = StepIds.UPDATE_AND_VERIFY_OSS)
    public void updateAndVerifyOss(@Input("description") final String description, @Input("event_stats_delay") final int event_stats_delay,
            @Input("oss_payload") final String ossPayload, @Input("oss_name") final String ossName) {
        Reporter.log(String.format("Data details %s", description));
        LOGGER.info("Configure existing OSS (or create a new OSS) and schedule immediate CM Data Loading, No PM-Events-Stats Scheduling");
        assertTrue(OssRepositoryRestOperator.updateOSS(ossPayload, ossName, eccdLbIp, ingressHost, IamAccessRestOperator.TAF_SUPER_USER),
                EccdResthandler.getMessage());
        assertTrue(OssRepositoryRestOperator.checkConfiguredOSS(eccdLbIp, ingressHost, IamAccessRestOperator.TAF_SUPER_USER),
                EccdResthandler.getMessage());
    }

    @Test(groups = { "CM loading and set schedules " })
    @TestStep(id = StepIds.SCHEDULE_PM_EVENTS_AND_STATS)
    public void schedulePmEventsAndStats(@Input("event_stats_delay") final int event_stats_delay) {
        LOGGER.info("Configure PM Stats and Events with delay of {} minutes", event_stats_delay);
        assertTrue(DeploymentCliOperator.setSchedulesOnDeploymentForPmStatsEvents(namespace, event_stats_delay, eccdHandler, installDirectory,
                releaseName), DeploymentCliOperator.getMessage());
    }

    @Test(groups = { "CM loading and set schedules " })
    @TestStep(id = StepIds.SCHEDULE_PM_EVENTS_AND_STATS)
    public void schedulePmEvents(@Input("event_stats_delay") final int event_stats_delay) {
        LOGGER.info("Configure PM Events with delay of {} minutes", event_stats_delay);
        assertTrue(DeploymentCliOperator.setSchedulesOnDeploymentForPmEvents(namespace, event_stats_delay, eccdHandler, installDirectory,
                releaseName), DeploymentCliOperator.getMessage());
    }

    @Test(groups = { "CM loading and set schedules" })
    @TestStep(id = StepIds.VERIFY_CM_COUNT_BEFORE)
    public void verifyCmCountsOverRestBeforeFileUpload(@Input("topology_object_names_before_file_upload") final String topologyObjectNames,
            @Input("topology_object_counts_before_file_upload") final String topologyObjectCounts) {
        assertTrue(CmServiceRestOperator.checkNumberOfObjectsReturned(eccdLbIp, ingressHost, topologyObjectNames, topologyObjectCounts,
                IamAccessRestOperator.TAF_SUPER_USER), CmServiceRestOperator.getMessage());
    }

    @TestStep(id = StepIds.EMF_UPLOAD)
    @Test(groups = { "KGB", "E_SON", "CM Service" })
    public void verifyEmfFileUpload(@Input("emfData") final String emfData) {
        assertTrue(cmOperator.uploadEmfData(eccdLbIp, ingressHost, emfData, IamAccessRestOperator.TAF_SUPER_USER),
                CmServiceRestOperator.getMessage());
    }

    @TestStep(id = StepIds.PTF_UPLOAD)
    @Test(groups = { "KGB", "E_SON", "CM Service" })
    public void verifyPtfFileUpload(@Input("ptfData") final String ptfData) {
        assertTrue(cmOperator.uploadPtfData(eccdLbIp, ingressHost, ptfData, IamAccessRestOperator.TAF_SUPER_USER),
                CmServiceRestOperator.getMessage());
        assertTrue(DeploymentCliOperator.verifyStringWithKubectlPodLoop(namespace, eccdHandler, ERIC_CM_TOPOLOGY_MODEL_SN_CONTAINER,
                COMPLETED_LOGICAL_HIERARCHY));
    }

    @Test(groups = { "CM loading and set schedules" })
    @TestStep(id = StepIds.VERIFY_CM_COUNT_AFTER)
    public void verifyCmCountsOverRestAfterFileUpload(@Input("topology_object_names_after_file_upload") final String topologyObjectNames,
            @Input("topology_object_counts_after_file_upload") final String topologyObjectCounts) {
        assertTrue(CmServiceRestOperator.checkNumberOfObjectsReturned(eccdLbIp, ingressHost, topologyObjectNames, topologyObjectCounts,
                IamAccessRestOperator.TAF_SUPER_USER), CmServiceRestOperator.getMessage());
    }

    @TestStep(id = StepIds.VERIFY_CM_DB_ASSERTIONS)
    @Test(groups = { "KGB", "E_SON", "CM Service" })
    public void verifyCmDbAssertions(@Input("db_connection_details") final String db_connection_details, @Input("db_tables") final String dbTables,
            @Input("db_table_count_values") final String dbTableCountValues, @Input("polling_timeout") final int pollingTimeOut)
            throws TestException {
        assertTrue(DatabaseOperator.execute(db_connection_details, dbTables, dbTableCountValues, pollingTimeOut), DatabaseOperator.getMessage());
    }

    @TestStep(id = StepIds.VERIFY_PM_EVENTS_EXECUTED)
    @Test(groups = { "KGB", "E_SON", "PMEvents" })
    public void verifyPmEventsExecuted(@Input("pm_files_processed") final int pm_files_processed) {
        try {
            LOGGER.info("Waiting for pm processing");
            TimeUnit.MINUTES.sleep(3);
        } catch (final InterruptedException e) {
            e.printStackTrace();
        }
        assertTrue(DeploymentCliOperator.verifyStringWithKubectlPodLoop(namespace, eccdHandler, ERIC_PM_EVENTS_PROCESSOR_ER_CONTAINER,
                PM_EVENTS_STARTING_LOADING_SEARCH_STRING));
        assertTrue(DeploymentCliOperator.verifyStringWithKubectlPodLoop(namespace, eccdHandler, ERIC_PM_EVENTS_PROCESSOR_ER_CONTAINER,
                pm_files_processed + " " + PM_EVENTS_FILES_SENT_FOR_PARSING_SEARCH_STRING));
    }

    @TestStep(id = StepIds.VERIFY_PM_EVENTS_DB_ASSERTIONS)
    @Test(groups = { "KGB", "E_SON", "PMEvents" })
    public void verifyPmEventsDbAssertions(@Input("db_connection_details") final String db_connection_details,
            @Input("db_tables") final String dbTables, @Input("db_table_count_values") final String dbTableCountValues,
            @Input("polling_timeout") final int pollingTimeOut) throws TestException {
        assertTrue(DatabaseOperator.execute(db_connection_details, dbTables, dbTableCountValues, pollingTimeOut), DatabaseOperator.getMessage());
    }

    @TestStep(id = StepIds.VERIFY_PM_EVENTS_DB_VALUE_ASSERTIONS)
    @Test(groups = { "KGB", "E_SON", "KPI" }, dependsOnMethods = { "verifyPmEventsDbAssertions" })
    public void verifyPmEventsDbValueAssertions(@Input("pm_db_connection_details") final String pm_db_connection_details,
            // NOSONAR Exception suitably logged
            @Input("query_details") final String queryDetails, @Input("where_clause_columns") final String whereClauseColumns,
            @Input("where_clause_values") final String whereClauseValues, @Input("expected_value") final String expectedValue,
            @Input("polling_timeout") final int pollingTimeOut, @Input("cm_details") final String cmDetails,
            @Input("cm_db_connection_details") final String cm_db_connection_details) throws TestException {
        final String whereClauseValuesModified = whereClauseValues.replace("guid_value",
                DatabaseOperator.getGuidFromFdnInCm(cm_db_connection_details, cmDetails));
        assertTrue(DatabaseOperator.executeAndAssertSelectOnTable(pm_db_connection_details, queryDetails, whereClauseColumns,
                whereClauseValuesModified, expectedValue, pollingTimeOut), DatabaseOperator.getMessage());
    }

    @TestStep(id = StepIds.POLL_PM_EVENTS_DB_VALUE)
    @Test(groups = { "KGB", "E_SON", "PMEvents" }, dependsOnMethods = { "verifyPmEventsDbAssertions" })
    public void pollToVerifyPmEventsAreLoadedCompletely(@Input("pm_db_connection_details") final String pmDbConnectionDetails,
            @Input("pm_query_details") final String pmQueryDetails,
            @Input("where_clause_columns") final String whereClauseColumns,
            @Input("where_clause_values") final String whereClauseValues,
            @Input("times_to_poll_to_verify") final int timesToPollToVerify) throws TestException {

        final LocalDate todayMinusOne = LocalDate.now().minusDays(1);
        final LocalDate todayMinusTwo = LocalDate.now().minusDays(2);

        final String todayMinusOneDate = todayMinusOne.toString();
        final String todayMinusTwoDate = todayMinusTwo.toString();

        final String whereClauseValuesModified = whereClauseValues.replaceAll("%today_minus_one", todayMinusOneDate)
                .replaceAll("%today_minus_two", todayMinusTwoDate);

        DatabaseOperator.executeSelectOnPmEventsDb(pmDbConnectionDetails, pmQueryDetails, whereClauseColumns, whereClauseValuesModified,
                timesToPollToVerify);
    }

    @TestStep(id = StepIds.VERIFY_PM_STATS_EXECUTED)
    @Test(groups = { "KGB", "E_SON", "PMStats" })
    public void verifyPmStatsExecuted() {
        assertTrue(DeploymentCliOperator.verifyStringWithKubectlPodLoop(namespace, eccdHandler, ERIC_PM_STATS_PROCESSOR_ER_CONTAINER,
                PM_STATS_STARTING_LOADING_SEARCH_STRING));
        assertTrue(DeploymentCliOperator.verifyStringWithKubectlPodLoop(namespace, eccdHandler, ERIC_PM_STATS_PROCESSOR_ER_CONTAINER,
                PM_STATS_FINISHED_LOADING_SEARCH_STRING));
    }

    @TestStep(id = StepIds.VERIFY_PM_STATS_EXECUTED)
    @Test(groups = { "KGB", "E_SON", "PMStats" })
    public void verifyLoaderExecuted(final Timestamp afterTimestamp) {
        assertTrue(DeploymentCliOperator.verifyStringWithKubectlPodLoop(namespace, eccdHandler, ERIC_CM_LOADER_ER_CONTAINER,
                CM_LOADER_FINISHED_LOADING_SEARCH_STRING, afterTimestamp, 60));
    }

    @TestStep(id = StepIds.VERIFY_PM_STATS_DB_ASSERTIONS)
    @Test(groups = { "KGB", "E_SON", "PMStats" })
    public void verifyPmStatsDbAssertions(@Input("db_connection_details") final String db_connection_details,
            @Input("db_tables") final String dbTables, @Input("db_table_count_values") final String dbTableCountValues,
            @Input("polling_timeout") final int pollingTimeOut) throws TestException {
        assertTrue(DatabaseOperator.execute(db_connection_details, dbTables, dbTableCountValues, pollingTimeOut), DatabaseOperator.getMessage());
    }

    @TestStep(id = StepIds.VERIFY_PM_STATS_DB_VALUE_ASSERTIONS)
    @Test(groups = { "KGB", "E_SON", "KPI" })
    public void verifyPmStatsDbValueAssertions(@Input("pm_db_connection_details") final String pm_db_connection_details,
            @Input("query_]details") final String queryDetails, @Input("where_clause_columns") final String whereClauseColumns,
            @Input("where_clause_values") final String whereClauseValues, @Input("expected_value") final String expectedValue,
            @Input("polling_timeout") final int pollingTimeOut) throws TestException {
        assertTrue(DatabaseOperator.executeAndAssertSelectOnTable(pm_db_connection_details, queryDetails, whereClauseColumns, whereClauseValues,
                expectedValue, pollingTimeOut), DatabaseOperator.getMessage());
    }

    @TestStep(id = StepIds.VERIFY_SCHEDULED_KPIS_EXECUTED)
    @Test(groups = { "KGB", "E_SON", "KPI" })
    public void verifyScheduledKpisExecuted(@Input("kpi_verification_file") final String kpiDefinitionFile) {
        LOGGER.info("Requesting calculation of scheduled KPIs required through /calculation endpoint");
        final Map<String, String> calculationStatesByCalculationFrequency = KpiServiceRestOperator.calculateKpisAndPollState(ingressHost, eccdLbIp,
                kpiDefinitionFile, IamAccessRestOperator.TAF_SUPER_USER);
        final Set<String> calculationStates = new HashSet<>(calculationStatesByCalculationFrequency.values());
        assertEquals(calculationStates.size(), 1);
        assertTrue(calculationStates.contains(FINISHED_STATE));
    }

    @TestStep(id = StepIds.CALCULATE_ON_DEMAND_KPIS)
    @Test(groups = { "KGB", "E_SON", "KPI" })
    public void calculateOnDemandKpis(@Input("kpi_calculation_request") final String kpiCalculationRequest) {
        LOGGER.info("Requesting calculation of KPIs through /calculation endpoint");
        final String calculationState = KpiServiceRestOperator.calculatePreviousDayKpis(ingressHost, eccdLbIp, kpiCalculationRequest,
                IamAccessRestOperator.TAF_SUPER_USER);
        assertEquals(FINISHED_STATE, calculationState);
    }

    @TestStep(id = StepIds.VERIFY_KPI_DB_ASSERTIONS)
    @Test(groups = { "KGB", "E_SON", "KPI" })
    public void verifyKpiCalculatorDbAssertions(@Input("db_connection_details") final String db_connection_details,
            @Input("db_tables") final String dbTables, @Input("db_table_count_values") final String dbTableCountValues,
            @Input("polling_timeout") final int pollingTimeOut) throws TestException {
        assertTrue(DatabaseOperator.execute(db_connection_details, dbTables, dbTableCountValues, pollingTimeOut), DatabaseOperator.getMessage());
    }

    @TestStep(id = StepIds.VERIFY_KPI_DB_VALUE_ASSERTIONS)
    @Test(groups = { "KGB", "E_SON", "KPI" })
    public void verifyKpiCalculatorDbValueAssertionsOnView(@Input("db_connection_details") final String db_connection_details,
            @Input("query_details") final String queryDetails, @Input("where_clause_columns") final String whereClauseColumns,
            @Input("where_clause_values") final String whereClauseValues, @Input("expected_value") final String expectedValue,
            @Input("polling_timeout") final int pollingTimeOut, @Input("external_view_service_name") final String externalViewServiceName)
            throws TestException {
        assertTrue(DatabaseOperator.executeAndAssertSelectOnView(db_connection_details, queryDetails, whereClauseColumns, whereClauseValues,
                expectedValue, pollingTimeOut, externalViewServiceName), DatabaseOperator.getMessage());
    }

    @TestStep(id = StepIds.POST_CHANGES)
    @Test(groups = { "KGB", "E_SON", "CAS" })
    public void postChanges(@Input("changes_file") final String changesFile) {
        LOGGER.info("POST CM changes");
        assertTrue(CasRestOperator.postChanges(ingressHost, eccdLbIp, changesFile, IamAccessRestOperator.TAF_SUPER_USER),
                EccdResthandler.getMessage());
    }

    @TestStep(id = StepIds.VERIFY_CHANGES_CREATED)
    @Test(groups = { "KGB", "E_SON", "CAS" })
    public void verifyPendingApprovalChangesCreated(@Input("execution") final String executionId,
            @Input("numExpectedChanges") final int numExpectedChanges, @Input("range") final int range,
            @Input("changeType") final String changeType) {
        final int numChangesExpectedLowerLimit = numExpectedChanges - range;
        assertTrue(CasRestOperator.checkChangePendingApproval(eccdLbIp, ingressHost, executionId, IamAccessRestOperator.TAF_SUPER_USER,
                numChangesExpectedLowerLimit, changeType), CasRestOperator.getMessage());
    }

    @TestStep(id = StepIds.VERIFY_CHANGES)
    @Test(groups = { "KGB", "E_SON", "CAS" })
    public void verifyChanges(@Input("message-pod") final String messagePod, @Input("message-container") final String messageContainer,
            @Input("topic") final String topic, @Input("open-loop") final boolean openLoop, @Input("execution") final String executionId,
            @Input("numExpectedChanges") final int numExpectedChanges, @Input("range") final int range) {
        final int numChangesExpectedLowerLimit = numExpectedChanges - range;
        if (openLoop) {
            assertTrue(CasRestOperator.checkChangePendingApproval(eccdLbIp, ingressHost, executionId, IamAccessRestOperator.TAF_SUPER_USER,
                    numChangesExpectedLowerLimit), CasRestOperator.getMessage());
            assertTrue(CasRestOperator.updateChangeToProposed(eccdLbIp, ingressHost, executionId, IamAccessRestOperator.TAF_SUPER_USER),
                    CasRestOperator.getMessage());
        }
        assertTrue(new KafkaOperator(eccdHandler).verifyMessagesOnTopic(messagePod, messageContainer, namespace, topic,
                KafkaPredicatesHelper.getCmChangeMediationPredicate()));
        assertTrue(CasRestOperator.checkChangeSucceeded(eccdLbIp, ingressHost, executionId, IamAccessRestOperator.TAF_SUPER_USER,
                numChangesExpectedLowerLimit), CasRestOperator.getMessage());
    }

    @TestStep(id = StepIds.VERIFY_CHANGES_FLM)
    @Test(groups = { "KGB", "E_SON", "CAS" })
    public void verifyChanges(@Input("open-loop") final boolean openLoop, @Input("execution") final String executionId,
            @Input("numExpectedChanges") final int numExpectedChanges, @Input("range") final int range,
            @Input("changeType") final String changeType) {
        final int numChangesExpectedLowerLimit = numExpectedChanges - range;
        if (openLoop) {
            assertTrue(CasRestOperator.updateChangeToProposed(eccdLbIp, ingressHost, executionId, IamAccessRestOperator.TAF_SUPER_USER, changeType),
                    CasRestOperator.getMessage());
        }
        assertTrue(CasRestOperator.checkChangeSucceeded(eccdLbIp, ingressHost, executionId, IamAccessRestOperator.TAF_SUPER_USER,
                numChangesExpectedLowerLimit, changeType), CasRestOperator.getMessage());
    }

    @BeforeMethod(alwaysRun = true)
    public void logBeforeMethod(final Method method) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(DateTimeUtility.testStartMessage(method.getName()));
        }
    }

    @AfterMethod(alwaysRun = true)
    public void logAfterMethod(final ITestResult result) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(DateTimeUtility.testEndMessage(result.getMethod().getMethodName()));
        }
    }

    @AfterClass(alwaysRun = true)
    public void deleteOSS() {
        LOGGER.info("Delete OSS from CommonTestSteps");
        assertTrue(OssRepositoryRestOperator.deleteConfiguredOSS(eccdLbIp, ingressHost, IamAccessRestOperator.TAF_SUPER_USER));
    }

    public String getIngressHost() {
        return ingressHost;
    }

    public String getNamespace() {
        return namespace;
    }

    public String getEccdLbIp() {
        return eccdLbIp;
    }

    public EccdCliHandler getEccdHandler() {
        return eccdHandler;
    }

    public int getPaWindow() {
        return paWindow;
    }

    public int getNoOfPaExecutions() {
        return noOfPaExecutions;
    }

    public int getInitialOffset() {
        return initialOffset;
    }

    public int getPaExecutionOffset() {
        return paExecutionOffset;
    }

    public Instant getFlmStartTime() {
        return flmStartTime;
    }

    public void setFlmStartTime(final Instant flmStartTime) {
        this.flmStartTime = flmStartTime;
    }

    public static final class StepIds { // NOPMD
        public static final String SETUP_ENV = "setupEnv";
        public static final String PA_ENV_SETTINGS = "paEnvSettings";
        public static final String PRE_POPULATE_ENV = "prePopulateEnv";
        public static final String PUT_KPI_DEFINITIONS = "putKPIDefinitions";
        public static final String PUT_PM_DEFINITIONS = "putPMDefinitions";
        public static final String RESET_COLLECTION_ID_COUNTER = "resetCollectionIdCounter";
        public static final String POST_CM_COLLECTION = "postCMCollection";
        public static final String GET_CM_COLLECTION = "getCMCollection";
        public static final String GET_CM_COLLECTIONS = "getCMCollections";
        public static final String EVALUATE_CM_COLLECTION = "evaluateCMCollection";
        public static final String UPDATE_CM_COLLECTION = "updateCMCollection";
        public static final String DELETE_CM_COLLECTION = "deleteCMCollection";
        public static final String POST_CM_DEFINITIONS = "postCMDefinitions";
        public static final String SETUP_AND_VERIFY_OSS = "setupAndVerifyOss";
        public static final String UPDATE_AND_VERIFY_OSS = "updateAndVerifyOss";
        public static final String SCHEDULE_PM_EVENTS_AND_STATS = "schedulePmEventsAndStats";
        public static final String EMF_UPLOAD = "verifyEmfFileUpload";
        public static final String VERIFY_CM_COUNT_BEFORE = "VerifyCmCountsOverRestBeforeFileUpload";
        public static final String VERIFY_CM_COUNT_AFTER = "VerifyCmCountsOverRestAfterFileUpload";
        public static final String PTF_UPLOAD = "verifyPtfFileUpload";
        public static final String CALCULATE_ON_DEMAND_KPIS = "calculateOnDemandKpis";
        public static final String VERIFY_CM_DB_ASSERTIONS = "verifyCmDbAssertions";
        public static final String VERIFY_PM_EVENTS_EXECUTED = "verifyPmEventsExecuted";
        public static final String VERIFY_PM_EVENTS_DB_ASSERTIONS = "verifyPmEventsDbAssertions";
        public static final String VERIFY_PM_EVENTS_DB_VALUE_ASSERTIONS = "verifyPmEventsDbValueAssertions";
        public static final String POLL_PM_EVENTS_DB_VALUE = "pollPmEventsDbValue";
        public static final String VERIFY_PM_STATS_EXECUTED = "verifyPmStatsExecuted";
        public static final String VERIFY_PM_STATS_DB_ASSERTIONS = "verifyPmStatsDbAssertions";
        public static final String VERIFY_PM_STATS_DB_VALUE_ASSERTIONS = "verifyPmStatsDbValueAssertions";
        public static final String VERIFY_SCHEDULED_KPIS_EXECUTED = "verifyScheduledKpisExecuted";
        public static final String VERIFY_KPI_DB_ASSERTIONS = "verifyKpiDbAssertions";
        public static final String VERIFY_KPI_DB_VALUE_ASSERTIONS = "verifyKpiDbValueAssertions";
        public static final String VERIFY_CHANGES_CREATED = "verifyPendingApprovalChangesCreated";
        public static final String VERIFY_CHANGES = "verifyChanges";
        public static final String VERIFY_CHANGES_FLM = "verifyChangesFlm";
        public static final String POST_CHANGES = "postChanges";

        private StepIds() {
        }
    }
}