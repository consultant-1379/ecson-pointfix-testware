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

public interface ApiOperator {

    /**
     * Return true if the REST response is OK.
     *
     * @param restURL
     *            The REST URL.
     * @param eccdLbIp
     *            Load balancer IP The REST URL.
     * @param ingressHost
     *            The hostname of the ingress.
     * @param user
     *            The name of the KeyCloak user.
     * @return
     */
    boolean isGetRestResponseCodeOK(final String restURL, final String eccdLbIp, final String ingressHost, final String user);

    /**
     * Return true if the REST response is OK.
     *
     * @param restURL
     *            The REST URL.
     * @param eccdLbIp
     *            Load balancer IP The REST URL.
     * @param ingressHost
     *            The hostname of the ingress.
     * @param httpPutData
     *            The httpPutData of the request.
     * @param user
     *            The name of the KeyCloak user.
     *
     * @return
     */
    boolean isPutRestResponseCodeBadRequest(final String restURL, final String eccdLbIp, final String ingressHost, final String httpPutData, final String user);

}
