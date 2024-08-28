/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2018 - 2019
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/

package com.ericsson.oss.services.eson.test.operators.database;

/**
 * Exceptions thrown on failure or error during the execution of a test.
 */
public class TestException extends Exception {

    private static final long serialVersionUID = 1400645693226650474L;

    public TestException(final String errorMessage) {
        super(errorMessage);
    }

    public TestException(final String errorMessage, final Exception rootException) {
        super(errorMessage, rootException);
    }
}
