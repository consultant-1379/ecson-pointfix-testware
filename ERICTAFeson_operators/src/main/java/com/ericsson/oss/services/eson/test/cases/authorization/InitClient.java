/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2021
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.eson.test.cases.authorization;

import com.ericsson.cifwk.taf.annotations.TestId;
import org.testng.annotations.Test;
import com.ericsson.oss.services.eson.test.operators.ClientOperator;

public class InitClient {
    private static final String TEST_ID = "SONP-44612_Create and update TAF for Auth";
    private final ClientOperator clientOperator = new ClientOperator();

    @TestId(id = TEST_ID, title = "Setup client and verify.")
    @Test(groups = { "KGB", "eSON" })
    public void setupEnv() {
        clientOperator.initClient();
    }
}