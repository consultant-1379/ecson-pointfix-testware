/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2018
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.eson.test.helm;

public class HelmException extends Exception {

    private static final long serialVersionUID = 1L;

    public HelmException() {
        super();
    }

    public HelmException(final String message, final Throwable cause, final boolean enableSuppression, final boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public HelmException(final String message, final Throwable cause) {
        super(message, cause);
    }


    public HelmException(final String message) {
        super(message);
    }


    public HelmException(final Throwable cause) {
        super(cause);
    }

}
