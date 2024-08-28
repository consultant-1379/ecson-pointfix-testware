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
package com.ericsson.oss.services.eson.test.handlers;

import com.ericsson.de.tools.cli.CliCommandResult;
import com.ericsson.oss.services.eson.test.eccd.EccdCommandExecutor;
import com.ericsson.oss.services.eson.test.eccd.EccdHost;

public class EccdCliHandler {

    private final EccdCommandExecutor handler;

    public EccdCliHandler(final EccdHost eccdHost) {
        this.handler = new EccdCommandExecutor(eccdHost);
    }

    public CliCommandResult execute(final String cmd) {
        final StringBuilder cmdToExec = new StringBuilder();
        cmdToExec.append(cmd);
        return handler.execute(cmdToExec.toString());
    }

    public CliCommandResult executeCommand(final String cmd) {
        return handler.executeCommand(cmd);
    }
}
