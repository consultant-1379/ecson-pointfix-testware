/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2021
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.eson.test.operators;


public interface CmServiceOperator {

    /**
     * Return true if the REST response is OK.
     *
     * @param lbIp
     *        The IP address of the load balancer
     * @param ingressHost
     *        The host of the ingress
     * @param ptfData
     *        The Physical Topology Data
     * @param user
     *        TAF user
     * @return
     */
    boolean uploadPtfData(final String lbIp, final String ingressHost, final String ptfData, String user);

    /**
     * Return true if the REST response is OK.
     *
     * @param lbIp
     *        The IP address of the load balancer
     * @param ingressHost
     *        The host of the ingress
     * @param emfData
     *        The External Mapping Data
     * @param user
     *        TAF user
     *  @return
     */
    boolean uploadEmfData(final String lbIp, final String ingressHost, final String emfData, String user);

}