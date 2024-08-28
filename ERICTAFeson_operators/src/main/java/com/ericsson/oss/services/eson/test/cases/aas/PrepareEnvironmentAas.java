/*
 *------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2022
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *------------------------------------------------------------------------------
 */

package com.ericsson.oss.services.eson.test.cases.aas;

import static com.ericsson.oss.services.eson.test.operators.DeploymentCliOperator.getKubeConfig;

import java.io.IOException;
import java.util.Iterator;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.de.tools.cli.CliCommandResult;
import com.ericsson.oss.services.eson.test.constants.common.DeploymentConstants;
import com.ericsson.oss.services.eson.test.eccd.EccdHostGroup;
import com.ericsson.oss.services.eson.test.handlers.EccdCliHandler;
import com.ericsson.oss.services.eson.test.util.CheckResult;
import com.ericsson.oss.services.eson.test.util.Resources;

/**
 * Prepare test server Environment for AAS.
 */
public class PrepareEnvironmentAas {

    private static final Logger LOGGER = LoggerFactory.getLogger(PrepareEnvironmentAas.class);
    private static final String NAMESPACE = System.getProperty("namespace", "sonom");
    private static final String AAS_SERVICE_DATA = "ingress/aas/serviceSetup.json";

    private static final EccdCliHandler eccdHandler = new EccdCliHandler(EccdHostGroup.getEccdDrector());

    /**
     * Create Services for AAS
     */
    public static void createServiceAas() {
        LOGGER.info("Starting execution of AAS service command");
        executeResourceCommand(AAS_SERVICE_DATA);
    }

    private static void executeResourceCommand(final String dataset) {
        try {
            final JSONParser parser = new JSONParser();
            final JSONArray resources = (JSONArray) parser.parse(Resources.getClasspathResourceAsString(dataset));

            final Iterator<JSONObject> iterator = resources.iterator();
            while (iterator.hasNext()) {
                final JSONObject resource = iterator.next();
                final String cmd = String.format(DeploymentConstants.HELM_CREATE_RESOURCE, resource, NAMESPACE, getKubeConfig());
                final CliCommandResult helmResult = eccdHandler.executeCommand(cmd);
                if (CheckResult.isReturningError(helmResult)) {
                    final String message = String.format("Resource creation failed, reason: '%s'", helmResult.getOutput());
                    LOGGER.error(message);
                } else {
                    LOGGER.info("Resource creation successful: {}", helmResult.getOutput());
                }
            }
        } catch (final ParseException | IOException e) {
            LOGGER.error("Error executing command for creating resource {}", e.getMessage());
        }
    }

    private PrepareEnvironmentAas() {
    }
}