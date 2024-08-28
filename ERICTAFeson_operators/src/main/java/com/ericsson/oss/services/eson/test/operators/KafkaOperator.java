/*
 *------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2020 - 2022
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *------------------------------------------------------------------------------
 */
package com.ericsson.oss.services.eson.test.operators;

import static com.ericsson.oss.services.eson.test.constants.common.DeploymentConstants.KUBECTL_EXECUTE_COMMAND_ON_POD;
import static com.ericsson.oss.services.eson.test.constants.common.DeploymentConstants.KUBECTL_GET_SPECIFIC_PODS_AT_NAMESPACE;
import static com.ericsson.oss.services.eson.test.operators.DeploymentCliOperator.getKubeConfig;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.de.tools.cli.CliCommandResult;
import com.ericsson.oss.services.eson.test.handlers.EccdCliHandler;
import com.ericsson.oss.services.eson.test.util.CheckResult;
import com.google.common.base.Predicate;

public class KafkaOperator implements IKafkaOperator {

    private static final Logger LOGGER = LoggerFactory.getLogger(KafkaOperator.class);

    private static final Long KAFKA_CONSUMER_TIMEOUT = 10000L;

    private static final int FIRST_POD_INDEX = 0;

    private static final String OUTPUT_NEWLINE = "\n";
    private static final String FAILED_TO_EXECUTE_KUBECTL_MSG = "Failed to execute kubectl command '{}'";
    private static final String KAFKA_CONSOLE_CONSUMER_CMD = "/usr/bin/kafka-console-consumer.sh --bootstrap-server %1$s:9092 --topic %2$s --from-beginning --timeout-ms %3$d";
    private static final String JSON_IDENTIFIER = "{";

    private final EccdCliHandler eccdCliHandler;

    public KafkaOperator(final EccdCliHandler eccdCliHandler) {
        this.eccdCliHandler = eccdCliHandler;
    }

    @Override
    public boolean verifyMessagesOnTopic(final String pod, final String container, final String namespace, final String topic,
            final Predicate<String> messagePredicate) {
        final String specificPodsCmd = String.format(KUBECTL_GET_SPECIFIC_PODS_AT_NAMESPACE, namespace, getKubeConfig(), pod);
        final CliCommandResult specificPodsCommandResult = eccdCliHandler.execute(specificPodsCmd);

        if (CheckResult.isReturningError(specificPodsCommandResult)) {
            LOGGER.error(FAILED_TO_EXECUTE_KUBECTL_MSG, specificPodsCmd);
            return false;
        }

        final String podFullName = getPodFullName(specificPodsCommandResult.getOutput());
        final String kafkaConsoleConsumerCommand = String.format(KAFKA_CONSOLE_CONSUMER_CMD, pod, topic, KAFKA_CONSUMER_TIMEOUT);
        final String podCommand = String.format(KUBECTL_EXECUTE_COMMAND_ON_POD, getKubeConfig(), namespace, podFullName, container,
                kafkaConsoleConsumerCommand);
        final CliCommandResult podCommandCliResult = eccdCliHandler.execute(podCommand);

        if (CheckResult.isReturningError(podCommandCliResult)) {
            LOGGER.error(FAILED_TO_EXECUTE_KUBECTL_MSG, podCommand);
            return false;
        }
        return isMessageReceived(podCommandCliResult.getOutput(), messagePredicate);
    }

    private static String getPodFullName(final String output) {
        // In the case of multiple pods returned we only want the first one
        return output.split(OUTPUT_NEWLINE)[FIRST_POD_INDEX];
    }

    private static boolean isMessageReceived(final String output, final Predicate<String> messagePredicate) {
        final String[] lines = output.split(OUTPUT_NEWLINE);
        for (final String line : lines) {
            if (line.startsWith(JSON_IDENTIFIER) && messagePredicate.apply(line)) {
                LOGGER.info("Found message on topic '{}'", line);
                return true;
            }
        }
        return false;
    }
}