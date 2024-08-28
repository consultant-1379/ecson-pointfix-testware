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

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.IInvokedMethod;
import org.testng.ISuite;
import org.testng.ITestResult;
import org.testng.Reporter;

public class CustomReportHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(CustomReportHelper.class);

    public static final String TEST_ID = "TEST ID";
    public static final String TEST_DESC = "DESCRIPTION";
    public static final String CUSTOM_RESOURCE_CONFIGURATION = "customResourceDimension";
    public static final String CUSTOM_TEST_RESULT = "customTestResult";

    /* This is the customize emailabel report template file path in workspace */
    private static final String CUSTOM_REPORT_PATH = "target/custom-emailable-report.html";

    protected static final String MESSAGE = "MESSAGE";
    protected static final String RESULT = "RESULT";

    /* HTML tags*/
    protected static final String BREAK = "</br>";
    protected static final String S_BOLD = "<b><u style='font-family: sans-serif;'>";
    protected static final String BOLD_E = "</u></b>";
    protected static final String S_TR = "<tr style='height: 30px;'>";
    protected static final String S_TR_GREY = "<tr style='height: 30px; background: #f5f5f5;'>";
    protected static final String TR_E = "</tr>";
    protected static final String S_TD = "<td style='padding: 5px; border-left: 1px solid #36304a; border-bottom: 2px solid #36304a;'>";
    protected static final String S_TD_CENTER = "<td align=center style='padding: 5px; border-left: 1px solid #36304a; "
            + "border-bottom: 2px solid #36304a;'>";
    protected static final String TD_E = "</td>";
    protected static final String TH_E = "</th>";
    protected static final String S_THEAD = "<thead style='background: #36304a; color: white;'>";
    protected static final String THEAD_E = "</thead>";
    protected static final String TH_LEFT = "<th align=left style='padding: 5px; border-left: 1px solid #767676;'>";
    protected static final String TH_TOTAL = "<th>TOTAL</th>";
    protected static final String TH_PASSED = "<th bgcolor=green>PASSED</th>";
    protected static final String TH_FAILED = "<th bgcolor=red>FAILED</th>";
    protected static final String TABLE_E = "</table>";
    protected static final String S_TABLE = "<table style='width: 80%; margin-top: 10px; font-family:sans-serif; "
            + "font-size: 0.8em; border-collapse: collapse; border-radius: 10px;' border='1'>";

    private static final Set<IInvokedMethod> ALL_INVOKED_TEST_METHODS = new HashSet<>();

    /**
     * Write test report content to custom-emailable-report.html.
     * @param reportTemplate ReportTemplate
     */
    protected void createTestReport(final StringBuilder reportTemplate) {

        if (StringUtils.isNotEmpty(reportTemplate.toString())) {
            try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(CUSTOM_REPORT_PATH),
                    StandardCharsets.UTF_8)) {
                writer.write(reportTemplate.toString());
                writer.flush();
            } catch (final IOException e) {
                LOGGER.error("{}", e.getMessage());
            }
        }
    }

    /**
     * Get all included method test results for the custom attribute.
     * @param attribute Attribute
     * @return testResults.
     */
    protected List<ITestResult> getIncludedMethodResults(final String attribute) {

        final List<ITestResult> testResults = new ArrayList<>();

        for (final IInvokedMethod testMethod : ALL_INVOKED_TEST_METHODS) {
            final ITestResult testResult = testMethod.getTestResult();
            if (testResult.getAttribute(attribute) != null) {
                testResults.add(testResult);
            }
        }
        return testResults;
    }

    /**
     * Setting all the test methods invoked in suites.
     * @param suites suites
     */
    public void setAllInvokedTestMethods(final List<ISuite> suites) {
        for (final ISuite suite : suites) {
            for (final IInvokedMethod allInvokedMethods : suite.getAllInvokedMethods()) {
                if (allInvokedMethods.isTestMethod()) {
                    ALL_INVOKED_TEST_METHODS.add(allInvokedMethods);
                }
            }

        }
    }

    /**
     * Setting testId attributes for custom testng emailable report.
     *
     * @param testId TestId
     * @param testDescription TestDescription
     */
    public static void setTestId(final String testId, final String testDescription) {
        final ITestResult attributes = Reporter.getCurrentTestResult();
        attributes.setAttribute(TEST_ID, testId);
        attributes.setAttribute(TEST_DESC, testDescription);
    }

    /**
     * Setting report attributes for custom testng emailable report.
     *
     * @param customName customName
     * @param customAttributes customAttributes
     * @param testResult testResult
     */
    public static void setCustomAttributes(final String customName, final boolean testResult, final String... customAttributes) {
        final ITestResult attributes = Reporter.getCurrentTestResult();
        attributes.setAttribute(customName, customAttributes);
        attributes.setAttribute(CUSTOM_TEST_RESULT, testResult ? ITestResult.SUCCESS : ITestResult.FAILURE);
    }
}
