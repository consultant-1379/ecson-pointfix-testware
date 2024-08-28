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
package com.ericsson.oss.services.eson.test.handlers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.cifwk.taf.tools.http.HttpResponse;
import com.ericsson.cifwk.taf.tools.http.RequestBuilder;
import com.ericsson.cifwk.taf.tools.http.constants.ContentType;
import com.ericsson.de.tools.http.BasicHttpToolBuilder;
import com.ericsson.de.tools.http.impl.HttpToolImpl;
import com.ericsson.oss.services.eson.test.operators.IamAccessRestOperator;

public class EccdResthandler {

    private EccdResthandler() {
    }

    public static boolean isAuthEnabled = false;
    private static String message = "";
    private static HttpResponse response;
    private static final Logger logger = LoggerFactory.getLogger(EccdResthandler.class);
    private static final IamAccessRestOperator iamAccessRestOperator = new IamAccessRestOperator();

    /**
     * Executes a POST request.
     *
     * @param url - URL path of the REST endpoint.
     * @param hostname - Cluster host name.
     * @param httpPostData - POST data of the request.
     * @param eccdLbIp - Load balancer IP of the cluster.
     * @param expectedResponse - The expected response of the REST call.
     * @param user - KeyCloak user.
     *
     * @return True if the expected response equals to the actual response, otherwise it returns false.
     */
    public static Boolean executePostCommand(final String url, final String hostname, final String httpPostData, final String eccdLbIp, final int expectedResponse, final String user) {
        final HttpToolImpl tool = (HttpToolImpl) BasicHttpToolBuilder.newBuilder(eccdLbIp)
                .withPort(443)
                .withProtocol("https")
                .trustSslCertificates(true)
                .build();

        RequestBuilder requestBuilder = tool.request()
                .contentType(ContentType.APPLICATION_JSON)
                .header("Accept", ContentType.APPLICATION_JSON)
                .header("Host", hostname);
        if (isAuthEnabled) {
            String clientAccessToken = iamAccessRestOperator.getClientAccessToken(hostname, eccdLbIp, user);
            requestBuilder = requestBuilder.header("Authorization", "Bearer " + clientAccessToken);
        }
        response = requestBuilder
                .body(httpPostData)
                .post(url);
        tool.close();
        return checkHttpResponseCode(url, expectedResponse, response);
    }

    /**
     * Executes a PUT request.
     *
     * @param url - URL path of the REST endpoint.
     * @param hostname - Cluster host name.
     * @param httpPutData - PUT data of the request.
     * @param eccdLbIp - Load balancer IP of the cluster.
     * @param expectedResponse - The expected response of the REST call.
     * @param user - KeyCloak user.
     *
     * @return True if the expected response equals to the actual response, otherwise it returns false.
     */
    public static Boolean executePutCommand(final String url, final String hostname, final String httpPutData, final String eccdLbIp, final int expectedResponse, final String user) {
        final HttpToolImpl tool = (HttpToolImpl) BasicHttpToolBuilder.newBuilder(eccdLbIp)
                .withPort(443)
                .withProtocol("https")
                .trustSslCertificates(true)
                .build();

        RequestBuilder requestBuilder = tool.request()
                .contentType(ContentType.APPLICATION_JSON)
                .header("Accept", ContentType.APPLICATION_JSON)
                .header("Host", hostname);
        if (isAuthEnabled) {
            String clientAccessToken = iamAccessRestOperator.getClientAccessToken(hostname, eccdLbIp, user);
            requestBuilder = requestBuilder.header("Authorization", "Bearer " + clientAccessToken);
        }
        response = requestBuilder
                .body(httpPutData)
                .put(url);
        tool.close();
        return checkHttpResponseCode(url, expectedResponse, response);
    }

    /**
     * Executes a DELETE request.
     *
     * @param url - URL path of the REST endpoint.
     * @param hostname - Cluster host name.
     * @param eccdLbIp - Load balancer IP of the cluster.
     * @param expectedResponse - The expected response of the REST call.
     * @param user - KeyCloak user.
     *
     * @return True if the expected response equals to the actual response, otherwise it returns false.
     */
    public static Boolean executeDeleteCommand(final String url, final String hostname, final String eccdLbIp, final int expectedResponse, final String user) {
        final HttpToolImpl tool = (HttpToolImpl) BasicHttpToolBuilder.newBuilder(eccdLbIp)
                .withPort(443)
                .withProtocol("https")
                .trustSslCertificates(true)
                .build();

        RequestBuilder requestBuilder = tool.request()
                .contentType(ContentType.APPLICATION_JSON)
                .header("Accept", ContentType.APPLICATION_JSON)
                .header("Host", hostname);
        if (isAuthEnabled) {
            String clientAccessToken = iamAccessRestOperator.getClientAccessToken(hostname, eccdLbIp, user);
            requestBuilder = requestBuilder.header("Authorization", "Bearer " + clientAccessToken);
        }
        response = requestBuilder
                .delete(url);
        tool.close();
        return checkHttpResponseCode(url, expectedResponse, response);
    }

    /**
     * Executes a GET request.
     *
     * @param url - URL path of the REST endpoint.
     * @param hostname - Cluster host name.
     * @param eccdLbIp - Load balancer IP of the cluster.
     * @param expectedResponse - The expected response of the REST call.
     * @param user - KeyCloak user.
     *
     * @return True if the expected response equals to the actual response, otherwise it returns false.
     */
    public static Boolean executeGetCommand(final String url, final String hostname, final String eccdLbIp, final int expectedResponse, final String user) {
        final HttpToolImpl tool = (HttpToolImpl) BasicHttpToolBuilder.newBuilder(eccdLbIp)
                .withPort(443)
                .withProtocol("https")
                .trustSslCertificates(true)
                .build();

        RequestBuilder requestBuilder = tool.request()
                .header("Host", hostname);
        if (isAuthEnabled) {
            String clientAccessToken = iamAccessRestOperator.getClientAccessToken(hostname, eccdLbIp, user);
            requestBuilder = requestBuilder.header("Authorization", "Bearer " + clientAccessToken);
        }
        response = requestBuilder
                .get(url);
        tool.close();
        return checkHttpResponseCode(url, expectedResponse, response);
    }

    /**
     * Executes a GET request.
     *
     * @param url - URL path of the REST endpoint.
     * @param hostname - Cluster host name.
     * @param eccdLbIp - Load balancer IP of the cluster.
     * @param expectedResponse - The expected response of the REST call.
     * @param user - KeyCloak user.
     *
     * @return the reponse of the GET request.
     */
    public static HttpResponse executeGetCommandAndReturnHttpResponse(final String url, final String hostname, final String eccdLbIp, final int expectedResponse, final String user) {
        final HttpToolImpl tool = (HttpToolImpl) BasicHttpToolBuilder.newBuilder(eccdLbIp)
                .withPort(443)
                .withProtocol("https")
                .trustSslCertificates(true)
                .build();
        
        RequestBuilder requestBuilder = tool.request()
                .header("Host", hostname);
        if (isAuthEnabled) {
            String clientAccessToken = iamAccessRestOperator.getClientAccessToken(hostname, eccdLbIp, user);
            requestBuilder = requestBuilder.header("Authorization", "Bearer " + clientAccessToken);
        }
        response = requestBuilder
                .get(url);
        tool.close();
        return response;
    }

    private static Boolean checkHttpResponseCode(final String url, final int expectedResponse, final HttpResponse response) {
        if (Integer.valueOf(response.getResponseCode().getCode()) != expectedResponse) {
            message = String.format("REST end point returned HTTP response code %s for command %s", response.getResponseCode().getCode(), url);
            logger.error(message);
            return false;
        }
        return true;
    }

    public static String getMessage() {
        return message;
    }

    public static HttpResponse getHttpResponse() {
        return response;
    }
}