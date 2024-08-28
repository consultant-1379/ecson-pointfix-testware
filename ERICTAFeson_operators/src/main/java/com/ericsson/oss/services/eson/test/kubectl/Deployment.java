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
package com.ericsson.oss.services.eson.test.kubectl;

public class Deployment {

    private String name;

    private int desired;

    private int current;

    private int upToDate;

    private int available;

    private String age;

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name
     *        the name to set
     */
    public void setName(final String name) {
        this.name = name;
    }

    /**
     * @return the desired
     */
    public int getDesired() {
        return desired;
    }

    /**
     * @param desired
     *        the desired to set
     */
    public void setDesired(final int desired) {
        this.desired = desired;
    }

    /**
     * @return the current
     */
    public int getCurrent() {
        return current;
    }

    /**
     * @param current
     *        the current to set
     */
    public void setCurrent(final int current) {
        this.current = current;
    }

    /**
     * @return the upToDate
     */
    public int getUpToDate() {
        return upToDate;
    }

    /**
     * @param upToDate
     *        the upToDate to set
     */
    public void setUpToDate(final int upToDate) {
        this.upToDate = upToDate;
    }

    /**
     * @return the available
     */
    public int getAvailable() {
        return available;
    }

    /**
     * @param available
     *        the available number to set
     */
    public void setAvailable(final int available) {
        this.available = available;
    }

    /**
     * @return the age
     */
    public String getAge() {
        return age;
    }

    /**
     * @param age
     *        the age to set
     */
    public void setAge(final String age) {
        this.age = age;
    }

}