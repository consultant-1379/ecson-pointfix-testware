/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2018 - 2021
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.eson.test.operators;

import java.io.IOException;

public interface IamAccessOperator {

    /**
     * Returns a client access token.
     *
     * @param hostname - KeyCloak hostname.
     * @param eccdLbIp - Load balancer IP of the cluster.
     * @param user - KeyCloak user.
     *
     * @return the client access token.
     * @throws java.io.UnsupportedEncodingException
     */
    String getClientAccessToken(final String hostname, String eccdLbIp, String user) throws IOException;
}
