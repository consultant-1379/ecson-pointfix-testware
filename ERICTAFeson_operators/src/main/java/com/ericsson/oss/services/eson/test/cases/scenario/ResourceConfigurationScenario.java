/*
 *------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson  2021
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *------------------------------------------------------------------------------
 */

package com.ericsson.oss.services.eson.test.cases.scenario;

import static com.ericsson.cifwk.taf.scenario.TestScenarios.annotatedMethod;
import static com.ericsson.cifwk.taf.scenario.TestScenarios.dataDrivenScenario;
import static com.ericsson.cifwk.taf.scenario.TestScenarios.dataSource;
import static com.ericsson.cifwk.taf.scenario.TestScenarios.flow;
import static com.ericsson.oss.services.eson.test.cases.util.CommonUtil.createSource;
import static com.ericsson.oss.services.eson.test.custom.report.CustomReportHelper.setTestId;

import java.lang.reflect.Method;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.ITestResult;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.ericsson.cifwk.taf.TafTestBase;
import com.ericsson.cifwk.taf.annotations.TestSuite;
import com.ericsson.cifwk.taf.scenario.TestScenario;
import com.ericsson.oss.services.eson.test.cases.util.CommonUtil;
import com.ericsson.oss.services.eson.test.teststeps.VerifyResourceConfigTestSteps;
import com.ericsson.oss.services.eson.test.util.DateTimeUtility;

public class ResourceConfigurationScenario extends TafTestBase {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResourceConfigurationScenario.class);

    private static final String FILTER_CONTAINS = "filter contains '%s'";

    /* csv filter, release name*/
    private static final String COMMON_RELEASE = "eric-oss-ec-son-common";
    private static final String MEDIATION_RELEASE = "eric-oss-ec-son-mediation";
    private static final String FLM_RELEASE = "eric-oss-ec-son-flm";

    /*csv file name*/
    private static final String CSAR_DIMENSION_DATA = "csarDimensionData";

    @Inject
    private VerifyResourceConfigTestSteps verifyResourceConfigTestSteps;

    /**
     * This is a configuration method and will be run before start of the test method.
     * @param testMethod testMethod
     */
    @BeforeMethod(alwaysRun = true)
    public void logBeforeMethod(final Method testMethod) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(DateTimeUtility.testStartMessage(testMethod.getName()));
        }
    }

    /**
     * Verify Common Resource Configuration.
     */
    @TestSuite
    @Test()
    public void commonCsarResourceConfiguration() {

        setTestId("CONFIGURATION-1", "COMMON RESOURCE CONFIGURATION");

        final TestScenario scenario = dataDrivenScenario(COMMON_RELEASE)
                .addFlow(flow(COMMON_RELEASE)
                        .addTestStep(annotatedMethod(verifyResourceConfigTestSteps, VerifyResourceConfigTestSteps.StepIds.VERIFY_CSAR_RESOURCE_CONFIGURATION)))
                .withScenarioDataSources(dataSource(createSource(CSAR_DIMENSION_DATA)).withFilter(String.format(FILTER_CONTAINS, COMMON_RELEASE)))
                .build();
        CommonUtil.start(scenario);
    }

    /**
     * Verify Mediation Resource Configuration.
     */
    @TestSuite
    @Test()
    public void mediationCsarResourceConfiguration() {

        setTestId("CONFIGURATION-2", "MEDIATION RESOURCE CONFIGURATION");

        final TestScenario scenario = dataDrivenScenario(MEDIATION_RELEASE)
                .addFlow(flow(MEDIATION_RELEASE)
                        .addTestStep(annotatedMethod(verifyResourceConfigTestSteps, VerifyResourceConfigTestSteps.StepIds.VERIFY_CSAR_RESOURCE_CONFIGURATION)))
                .withScenarioDataSources(dataSource(createSource(CSAR_DIMENSION_DATA)).withFilter(String.format(FILTER_CONTAINS, MEDIATION_RELEASE)))
                .build();
        CommonUtil.start(scenario);
    }

    /**
     * Verify Flm Resource Configuration.
     */
    @TestSuite
    @Test()
    public void flmCsarResourceConfiguration() {

        setTestId("CONFIGURATION-3", "FLM RESOURCE CONFIGURATION");

        final TestScenario scenario = dataDrivenScenario(FLM_RELEASE)
                .addFlow(flow(FLM_RELEASE)
                        .addTestStep(annotatedMethod(verifyResourceConfigTestSteps, VerifyResourceConfigTestSteps.StepIds.VERIFY_CSAR_RESOURCE_CONFIGURATION)))
                .withScenarioDataSources(dataSource(createSource(CSAR_DIMENSION_DATA)).withFilter(String.format(FILTER_CONTAINS, FLM_RELEASE)))
                .build();
        CommonUtil.start(scenario);
    }

    /**
     * This is a configuration method and will be run after end of the test method.
     * @param testResult testResult
     */
    @AfterMethod(alwaysRun = true)
    public void logAfterMethod(final ITestResult testResult) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(DateTimeUtility.testEndMessage(testResult.getMethod().getMethodName()));
        }
    }
}
