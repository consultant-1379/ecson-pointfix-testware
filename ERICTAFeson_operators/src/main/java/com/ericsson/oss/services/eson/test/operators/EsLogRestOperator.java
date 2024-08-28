/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2019 - 2021
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.eson.test.operators;

import static com.ericsson.oss.services.eson.test.constants.common.DeploymentConstants.KUBECTL_GET_INGRESS_BY_NAME;
import static com.ericsson.oss.services.eson.test.constants.common.DeploymentConstants.KUBECTL_GET_SERVICE;
import static com.ericsson.oss.services.eson.test.operators.DeploymentCliOperator.getKubeConfig;
import static com.ericsson.oss.services.eson.test.util.Resources.getClasspathResourceAsString;

import java.io.IOException;
import java.util.Iterator;

import org.apache.commons.lang.StringEscapeUtils;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.de.tools.cli.CliCommandResult;
import com.ericsson.oss.services.eson.test.constants.common.DeploymentConstants;
import com.ericsson.oss.services.eson.test.handlers.EccdCliHandler;
import com.ericsson.oss.services.eson.test.util.CheckResult;

public final class EsLogRestOperator {

    private static final Logger LOGGER = LoggerFactory.getLogger(EsLogRestOperator.class);

    private static final String KIND = "kind";
    private static final String INGRESS = "Ingress";
    private static final String useAlternativeConfig = System.getProperty("useAlternativeConfig", "false");

    private static String message = "";

    private enum OperationMode {
        CREATE
    }

    private EsLogRestOperator() {
    }

    public static void createIngressAndServices(final EccdCliHandler eccdHandler, final String namespace) {
        LOGGER.info("Start execution of ingress command");
        executeResourceCommand(eccdHandler, namespace, OperationMode.CREATE);
    }

    private static void executeResourceCommand(final EccdCliHandler eccdHandler, final String namespace, final OperationMode mode) {
        try {
            final JSONParser parser = new JSONParser();
            final org.json.simple.JSONArray resources;
            if (useAlternativeConfig.equals("true")) {
                resources = (org.json.simple.JSONArray) parser.parse(getClasspathResourceAsString("ingress/ingressSetupAlternative.json"));
            } else {
                resources = (org.json.simple.JSONArray) parser.parse(getClasspathResourceAsString("ingress/ingressSetup.json"));
            }
            final Iterator<org.json.simple.JSONObject> iterator = resources.iterator();
            while (iterator.hasNext()) {
                final org.json.simple.JSONObject jsonObject = iterator.next();
                if (jsonObject.containsKey(KIND)) {
                    final String kind = jsonObject.get(KIND).toString();
                    final String resourceName = getMetaDataName(jsonObject);
                    final String resourceCmd = String.format(getHelmCommandByResourceKind(kind), namespace, resourceName, getKubeConfig());

                    if (OperationMode.CREATE == mode && existsInHelm(resourceName, resourceCmd, eccdHandler)) {
                        continue;
                    }

                    final String ingressCommandString = StringEscapeUtils.escapeJava(jsonObject.toJSONString());
                    final String cmd = String.format(getHelmResourceCommand(mode), ingressCommandString, namespace, getKubeConfig());
                    final CliCommandResult helmResult = eccdHandler.execute(cmd);
                    if (CheckResult.isReturningError(helmResult)) {
                        message = String.format("Failed to %s resource, reason: '%s'", mode.toString(), helmResult.getOutput());
                        LOGGER.error(message);
                        // Failing fast here, no point going further into the tests for them to fail for an "unknown" reason
                        throw new IllegalStateException(message);
                    } else {
                        LOGGER.info("{} success: {}", mode, helmResult.getOutput());
                    }
                }
            }
        } catch (final ParseException | IOException e) {
            LOGGER.error("Error executing command ingress for ES logging {}", e.getMessage());
        }
        LOGGER.info("Successfully executed ingress command");
    }

    public static String getMessage() {
        return message;
    }

    private static String getMetaDataName(final org.json.simple.JSONObject jsonObject) {
        return ((org.json.simple.JSONObject) jsonObject.get("metadata")).get("name").toString();
    }

    private static boolean existsInHelm(final String resource, final String command, final EccdCliHandler eccdHandler) {
        final CliCommandResult helmResult = eccdHandler.execute(command);
        if (CheckResult.isReturningError(helmResult)) {
            message = String.format("'%s' does not exist, reason: %s", resource, helmResult.getOutput());
            return false;
        }

        if (helmResult.getOutput().matches("(?s).*" + resource + ".*")) {
            LOGGER.info("'{}' already exists", resource);
            return true;
        }
        return false;
    }

    private static String getHelmCommandByResourceKind(final String resource) {
        return resource.equalsIgnoreCase(INGRESS) ? KUBECTL_GET_INGRESS_BY_NAME : KUBECTL_GET_SERVICE;
    }

    private static String getHelmResourceCommand(final OperationMode mode) {
        return OperationMode.CREATE == mode ? DeploymentConstants.HELM_CREATE_RESOURCE : DeploymentConstants.HELM_DELETE_RESOURCE;
    }

}
