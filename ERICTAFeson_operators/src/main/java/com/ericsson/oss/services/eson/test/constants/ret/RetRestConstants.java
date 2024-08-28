/*
 *------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2020
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *------------------------------------------------------------------------------
 */

package com.ericsson.oss.services.eson.test.constants.ret;

public class RetRestConstants {

    // REST End Points RET Service
    public static final String PUT_CONFIGURATIONS = "/son-om/algorithms/ret/v1/configurations";
    public static final String GET_EXECUTIONS = "/son-om/algorithms/ret/v1/executions";
    public static final int SLEEP_TIME_TO_ALLOW_RET_TRIGGER = 2;

    private RetRestConstants() {
    }
}
