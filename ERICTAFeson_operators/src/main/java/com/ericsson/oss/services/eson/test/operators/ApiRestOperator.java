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

import com.ericsson.oss.services.eson.test.handlers.EccdResthandler;

import io.netty.handler.codec.http.HttpResponseStatus;

public class ApiRestOperator implements ApiOperator {

    @Override
    public boolean isGetRestResponseCodeOK(final String restURL, final String eccdLbIp, final String ingressHost,
                                           String user) {
        return EccdResthandler.executeGetCommand(restURL, ingressHost, eccdLbIp, HttpResponseStatus.OK.code(), user);
    }

    @Override
    public boolean isPutRestResponseCodeBadRequest(final String restURL, final String ingressHost, final String eccdLbIp, final String httpPutData, String user) {
        return EccdResthandler.executePutCommand(restURL, ingressHost, eccdLbIp, httpPutData, HttpResponseStatus.BAD_REQUEST.code(), user);
    }
}