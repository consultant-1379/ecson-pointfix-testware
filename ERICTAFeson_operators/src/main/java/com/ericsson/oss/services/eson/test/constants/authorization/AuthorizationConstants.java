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
package com.ericsson.oss.services.eson.test.constants.authorization;

/**
 * These constants are used for authorization related tests.
 */
public class AuthorizationConstants {
    public static final String AUTHORIZATION_FILE_NAME = "authorization";
    public static final String INGRESS_HOST = "cd-server.sonom.com";
    public static final String PUT = "PUT";
    public static final String GET = "GET";
    public static final String POST = "POST";
    public static final String CM_FILE = "CmChanges.json";
    public static final String NM_SERVICE_DATA = "{\"type\":\"enm\",\"name\":\"stubbed-enm\",\"content\":\"{\\\"enm_ui_host\\\":\\\"stubbed-enm\\\",\\\"enm_ui_username\\\":\\\"eson_user\\\",\\\"enm_ui_password\\\": \\\"enm12admin\\\",\\\"enm_cm_data_loading_cron\\\":\\\"\\\"}\"}";
    public static final String RET_SERVICE = "ret-service";
    public static final String NM_SERVICE = "nm-service";
    public static final String CM_SERVICE = "cm-service";
    public static final String FLM_SERVICE = "flm-service";
    public static final String EMPTY_HTTP_DATA = "{}";
}