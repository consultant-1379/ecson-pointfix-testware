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
package com.ericsson.oss.services.eson.test.eccd;

import com.ericsson.cifwk.taf.data.DataHandler;
import com.ericsson.cifwk.taf.data.Host;
import com.ericsson.cifwk.taf.data.HostType;


public abstract class EccdHostGroup {

    private EccdHostGroup() { }

    public static EccdHost getEccdDrector() {
        final Host host = DataHandler.getHostByType(HostType.DIRECTOR);
        return new EccdHost(host);
    }

    public static String getByAttribute(final String attribute) {
        return DataHandler.getAttribute(attribute).toString();
    }
}
