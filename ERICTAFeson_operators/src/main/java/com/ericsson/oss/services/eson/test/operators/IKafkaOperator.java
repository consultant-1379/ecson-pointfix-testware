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
package com.ericsson.oss.services.eson.test.operators;

import com.google.common.base.Predicate;

public interface IKafkaOperator {

    /**
     * Returns {@code true} if a Kafka message matches the {@code messagePredicate} supplied, otherwise {@code false}.
     * 
     * @param pod
     *            The name of the Kafka pod
     * @param container
     *            The name of the Kafka container
     * @param namespace
     *            The namespace the pod is in
     * @param topic
     *            The name of the Kafka topic
     * @param messagePredicate
     *            The predicate to compare the Kafka message with
     * @return {@code true} if a Kafka message matches the {@code messagePredicate} supplied, otherwise {@code false}
     */
    boolean verifyMessagesOnTopic(final String pod, final String container, final String namespace, final String topic,
            final Predicate<String> messagePredicate);
}
