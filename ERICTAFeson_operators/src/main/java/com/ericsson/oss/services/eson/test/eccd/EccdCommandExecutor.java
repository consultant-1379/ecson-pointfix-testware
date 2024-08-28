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
package com.ericsson.oss.services.eson.test.eccd;

import com.ericsson.cifwk.taf.data.Host;
import com.ericsson.cifwk.taf.data.User;
import com.ericsson.de.tools.cli.CliCommandResult;
import com.ericsson.de.tools.cli.CliTool;
import com.ericsson.de.tools.cli.CliTools;

public class EccdCommandExecutor {

    private static final String BASH_SHELL = "bash -c";

    protected static final long DEFAULT_TIMEOUT = 90;

    private final CliTool simpleExecutor;

    public EccdCommandExecutor(final Host host) {
        final EccdHost eccdHost = new EccdHost(host);
        final User eccd = eccdHost.getEccdUser();
        simpleExecutor = CliTools.simpleExecutor(eccdHost.getIp())
                .withUsername(eccd.getUsername())
                .withPassword(eccd.getPassword())
                .withStrictHostKeyChecking(false)
                .withDefaultTimeout(DEFAULT_TIMEOUT)
                .build();
    }

    /**
     * Method to execute a command on a shell.
     *
     * @param cmd
     *            The command to be executed.
     * @return The stdout from the command.
     */
    public CliCommandResult execute(final String cmd) {
        final String fullCmd = " \"" + cmd + "\" ";
        try {
            return simpleExecutor.execute(BASH_SHELL + fullCmd);
        } finally {
            simpleExecutor.close();
        }
    }

    /**
     * Method to execute command
     *
     * @param cmd
     *            The command to be executed.
     * @return The stdout from the command.
     */
    public CliCommandResult executeCommand(final String command) {
        try {
            return simpleExecutor.execute(command);
        } finally {
            simpleExecutor.close();
        }
    }
}
