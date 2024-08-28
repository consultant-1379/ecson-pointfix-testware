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
package com.ericsson.oss.services.eson.test.helm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import hapi.release.StatusOuterClass.Status.Code;

/**
 * The <b>HelmReleaseStatus</b> is the abstraction for
 * <b>hapi.release.StatusOuterClass.Status.Code</b> with some utility methods and handling
 */

public class HelmReleaseStatus {

    public static final String DEPLOYED = Code.DEPLOYED.name();
    public static final String SUPERSEDED = Code.SUPERSEDED.name();

    /* Default UNKNOWN status code */
    public static final String DEFAULT_CODE_UNKNOWN = Code.UNKNOWN.name();

    private static final Logger LOGGER = LoggerFactory.getLogger(HelmReleaseStatus.class);

    private final String releaseStatus;
    private Code code = null;

    public HelmReleaseStatus(final String releaseStatus) {
        this.releaseStatus = releaseStatus;
        parseCode();
    }

    /**
     * Gets the helm release status code
     *
     * @return String code
     */
    public String getCode() {
        if (isEligibleForDefaultCodeUnknown()) {
            return DEFAULT_CODE_UNKNOWN;
        } else {
            return code.name();
        }
    }

    /**
     * Checks if the Status code is same as <b>DEPLOYED</b>
     */
    public boolean isDeployed() {
        return HelmReleaseStatus.DEPLOYED.equals(getCode());
    }

    /**
     * Parse the code
     */
    private void parseCode() {
        try {
            code = Code.valueOf(releaseStatus);
        } catch (Exception e) {
            LOGGER.error("Not able to parse release status : \n" + releaseStatus, e);
        }
    }

    private boolean isEligibleForDefaultCodeUnknown() {
        return releaseStatus == null || code == null;
    }

}