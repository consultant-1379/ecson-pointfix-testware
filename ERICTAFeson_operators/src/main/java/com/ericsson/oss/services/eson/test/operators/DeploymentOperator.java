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
package com.ericsson.oss.services.eson.test.operators;

import com.ericsson.oss.services.eson.test.exceptions.DeploymentException;
import com.ericsson.oss.services.eson.test.handlers.EccdCliHandler;
import com.ericsson.oss.services.eson.test.kubectl.Pod;

public interface DeploymentOperator {

    /**
     * Return true if the deployment is Successful.
     *
     * @param namespace
     *        The Name Space of the Deployment.
     * @return
     */
    boolean verifySuccessfulDeployment(final String namespace, final EccdCliHandler eccdHandler);

    /**
     * Return true if all Pods in the deployment are Running.
     *
     * @param namespace
     *        The Name Space of the Deployment.
     * @return
     */
    boolean verifyPodsRunning(final String namespace, final EccdCliHandler eccdHandler);

    /**
     * Return true if the deployment are Available.
     *
     * @param namespace
     *        The Name Space of the Deployment.
     * @return
     */
    boolean verifyDeploymentsAvailable(String namespace, final EccdCliHandler eccdHandler);

    /**
     * @param replicaCount
     *          The number of replicas for any pod
     * @param statefulset
     *          The statefulset name that controls the replication of requested pod
     * @param namespace
     *        The Name Space of the Deployment.*
     * @param eccdHandler
     *        The eccdHandler for this change
     * @return
     *
     * @throws DeploymentException
     */
    boolean rescalePod(final int replicaCount,final String statefulset, final String namespace, final EccdCliHandler eccdHandler) throws DeploymentException;

}