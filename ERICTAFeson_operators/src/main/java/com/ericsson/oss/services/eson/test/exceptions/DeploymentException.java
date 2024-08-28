/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2019
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.eson.test.exceptions;

/**
 * A {@code DeploymentException} is thrown to indicate there was an issue when executing a <code>kubectl deployment</code> command.
 */
public class DeploymentException extends Exception {

    private static final long serialVersionUID = -7502593062608800771L;

    /**
     * Default constructor.
     * 
     * @param message
     *            The reason for the exception.
     */
    public DeploymentException(final String message) {
        super(message);
    }
}
