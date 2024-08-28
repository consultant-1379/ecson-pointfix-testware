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

package com.ericsson.oss.services.eson.test.teststeps;

import static org.testng.Assert.assertTrue;

import com.ericsson.cifwk.taf.annotations.Input;
import com.ericsson.cifwk.taf.annotations.TestStep;
import com.ericsson.oss.services.eson.test.operators.ResourceConfigurationOperator;
import com.google.inject.Inject;

public class VerifyResourceConfigTestSteps {

    @Inject
    private ResourceConfigurationOperator resourceConfigurationOperator;

    /**
     * Test verify CSAR Resource dimensions.
     * @param filter filter
     * @param containerName containerName
     * @param cpuRequest cpuRequest
     * @param cpuLimit cpuLimit
     * @param memoryRequest memoryRequest
     * @param memoryLimit memoryLimit
     * @param persistentStorage persistentStorage
     */
    @TestStep(id = VerifyResourceConfigTestSteps.StepIds.VERIFY_CSAR_RESOURCE_CONFIGURATION)
    public void verifyCsarResourceDimensions(
            @Input("filter") final String filter,
            @Input("containerName") final String containerName,
            @Input("cpuRequest") final String cpuRequest,
            @Input("cpuLimit") final String cpuLimit,
            @Input("memoryRequest") final String memoryRequest,
            @Input("memoryLimit") final String memoryLimit,
            @Input("persistentStorage") final String persistentStorage) {

        assertTrue(resourceConfigurationOperator.verifyResourceConfiguration(filter, containerName, cpuRequest, cpuLimit,
                memoryRequest, memoryLimit, persistentStorage), resourceConfigurationOperator.getTestMessage());
    }

    public static final class StepIds {
        public static final String VERIFY_CSAR_RESOURCE_CONFIGURATION = "verifyCsarResourceConfiguration";

        private StepIds() {
        }
    }
}
