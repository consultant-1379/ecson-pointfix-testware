/*
 *------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson  2020
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *------------------------------------------------------------------------------
 */

package com.ericsson.oss.services.eson.test.cases.util;

import static com.ericsson.cifwk.taf.scenario.TestScenarios.runner;

import com.ericsson.cifwk.taf.scenario.TestScenario;
import com.ericsson.cifwk.taf.scenario.TestScenarioRunner;
import com.ericsson.cifwk.taf.scenario.impl.LoggingScenarioListener;
import com.ericsson.oss.services.eson.test.eccd.EccdHostGroup;

public class CommonUtil {

    private CommonUtil() {
        //private constructor to hide implicit public one
    }

    public static void start(final TestScenario scenario) {
        final TestScenarioRunner runner = runner()
                .withListener(new LoggingScenarioListener())
                .build();
        runner.start(scenario);
    }

    /**
     * Create csv file name using network size.
     * @param csvName
     * @return csv
     */
    public static String createSource(final String csvName) {
        String source = EccdHostGroup.getByAttribute("eccd.network.type");
        return csvName + source;
    }
}
