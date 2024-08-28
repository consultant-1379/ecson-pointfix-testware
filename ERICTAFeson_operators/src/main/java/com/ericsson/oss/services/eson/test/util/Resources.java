/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2018 - 2019
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/

package com.ericsson.oss.services.eson.test.util;

import java.io.IOException;
import java.io.InputStream;

import static com.ericsson.oss.itpf.sdk.core.util.StreamUtils.convertStreamToString;

public class Resources {

    private Resources() { }

    public static String getClasspathResourceAsString(final String path) throws IOException {
        try (final InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(path)) {
            return convertStreamToString(inputStream);
        }
    }
}
