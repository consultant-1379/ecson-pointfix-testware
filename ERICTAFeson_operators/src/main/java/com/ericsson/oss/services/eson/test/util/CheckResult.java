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
package com.ericsson.oss.services.eson.test.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.de.tools.cli.CliCommandResult;

public abstract class CheckResult {

    private CheckResult() { }

    private static final Logger logger = LoggerFactory.getLogger(CheckResult.class);

    public static boolean isReturningError(final CliCommandResult result) {
        if (result.getExitCode()!= 0) {
            logger.error("Result Exit Code-: {}", result.getExitCode());
            logger.error("Result Output-: {}", result.getOutput());
            return true;
        }
        return false;
    }
}

