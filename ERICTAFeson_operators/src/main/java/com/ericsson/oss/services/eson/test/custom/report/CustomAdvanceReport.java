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

package com.ericsson.oss.services.eson.test.custom.report;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.testng.IReporter;
import org.testng.ISuite;
import org.testng.ITestResult;
import org.testng.xml.XmlSuite;

public class CustomAdvanceReport extends CustomReportHelper implements IReporter {

    @Override
    public final void generateReport(final List<XmlSuite> xmlSuites, final List<ISuite> suites,
            final String outputDirectory) {

        setAllInvokedTestMethods(suites);

        createTestReport(resourceConfigurationSummaryReport());
    }

    /**
     * Build resource configuration summary data template.
     *
     * @return reportTemplate ReportTemplate
     */
    private StringBuilder resourceConfigurationSummaryReport() {

        final StringBuilder reportTemplate = new StringBuilder();
        final List<ITestResult> testResults = getIncludedMethodResults(CUSTOM_RESOURCE_CONFIGURATION);

        if (!testResults.isEmpty()) {
            reportTemplate.append(BREAK)
            .append(S_TR).append(S_BOLD).append("RESOURCE CONFIGURATION REPORT:").append(BOLD_E).append(TR_E).append(BREAK)
            .append(getCustomSuiteSummary(testResults));

            if (!testResults.isEmpty()) {
                reportTemplate.append(BREAK)
                .append(S_BOLD).append("RESOURCE CONFIGURATON TEST CASES:").append(BOLD_E).append(BREAK)
                .append(BREAK).append(S_TABLE).append(S_THEAD).append(S_TR)
                .append(TH_LEFT).append(TEST_ID).append(TH_E)
                .append(TH_LEFT).append(TEST_DESC).append(TH_E)
                .append(TH_LEFT).append("CSAR NAME").append(TH_E)
                .append(TH_LEFT).append("REQUEST CPU").append(TH_E)
                .append(TH_LEFT).append("LIMIT CPU").append(TH_E)
                .append(TH_LEFT).append("REQUEST MEMORY").append(TH_E)
                .append(TH_LEFT).append("LIMIT MEMORY").append(TH_E)
                .append(TH_LEFT).append("PERSISTENCE STORAGE").append(TH_E)
                .append(TH_LEFT).append(RESULT).append(TH_E)
                .append(TH_LEFT).append(MESSAGE).append(TH_E)
                .append(THEAD_E).append(TR_E)
                .append(getCustomMethodSummary(CUSTOM_RESOURCE_CONFIGURATION, testResults))
                .append(TABLE_E).append(BREAK);
            }
        }
        return reportTemplate;
    }

    /**
     * Build suite summary data template.
     *
     * @param testResults Test Results
     * @return reportTemplate ReportTemplate
     */
    private StringBuilder getCustomSuiteSummary(final List<ITestResult> testResults) {

        final StringBuilder reportTemplate = new StringBuilder();
        final AtomicInteger totalTestPassed = new AtomicInteger();
        final AtomicInteger totalTestFailed = new AtomicInteger();

        for (final ITestResult testresult : testResults) {
            if (Integer.parseInt(testresult.getAttribute(CUSTOM_TEST_RESULT).toString()) == ITestResult.SUCCESS) {
                totalTestPassed.incrementAndGet();
            } else {
                totalTestFailed.incrementAndGet();
            }
        }
        final int totalTestCount = totalTestPassed.get() + totalTestFailed.get();

        reportTemplate.append(BREAK)
        .append(S_TABLE).append(S_THEAD).append(S_TR)
        .append(TH_TOTAL).append(TH_PASSED).append(TH_FAILED).append(THEAD_E).append(TR_E)
        .append(S_TR_GREY)
        .append(S_TD_CENTER).append(totalTestCount).append(TD_E)
        .append(S_TD_CENTER).append(totalTestPassed).append(TD_E)
        .append(S_TD_CENTER).append(totalTestFailed).append(TD_E)
        .append(TR_E).append(TABLE_E).append(BREAK);
        return reportTemplate;
    }

    /**
     * Build test method data template.
     *
     * @param customName Custom Name
     * @param testResults Test Results
     * @return reportTemplate ReportTemplate
     */
    private StringBuilder getCustomMethodSummary(final String customName, final List<ITestResult> testResults) {

        final StringBuilder reportTemplate = new StringBuilder();

        for (final ITestResult testResult : testResults) {
            reportTemplate.append(S_TR_GREY)
            .append(S_TD).append(testResult.getAttribute(TEST_ID)).append(TD_E)
            .append(S_TD).append(testResult.getAttribute(TEST_DESC)).append(TD_E);
            final Object[] parameters = (Object[]) testResult.getAttribute(customName);
            for (final Object parameter : parameters) {
                reportTemplate.append(S_TD).append(parameter.toString()).append(TD_E);
            }
            reportTemplate.append(TR_E);
        }
        return reportTemplate;
    }
}
