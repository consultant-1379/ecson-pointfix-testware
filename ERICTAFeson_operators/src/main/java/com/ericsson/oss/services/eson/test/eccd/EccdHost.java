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

import com.ericsson.cifwk.taf.data.Host;
import com.ericsson.cifwk.taf.data.User;
import com.ericsson.cifwk.taf.data.UserType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class EccdHost extends Host {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public EccdHost(final Host host) {
        super();
        setUsers(host.getUsers());
        setHostname(host.getHostname());
        setType(host.getType());
        setIp(System.getProperty("eccd_director_ip",host.getIp()));
        setPort(host.getPort());
    }

    /**
     * Gets the eccd user configured in the host properties
     *
     * @return User
     */
    public User getEccdUser() {
        return getSpecificUser("eccd", UserType.OPER);
    }

    private User getSpecificUser(final String userName, final UserType userType) {
        final List<User> users = this.getUsers(userType);
        for (final User user : users) {
            if (userName.equals(user.getUsername())) {
                return user;
            }
        }
        logger.error("No user of type {} found with name {}", userType, userName);
        return new User();
    }

    @Override
    public boolean equals(final Object other) {
        return super.equals(other);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }
}