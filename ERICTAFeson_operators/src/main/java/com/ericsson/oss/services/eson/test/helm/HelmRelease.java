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

import java.util.Date;

/**
 * POJO for helm release information.<br>
 * Example:<br>
 * {@code
 * NAME           	REVISION	UPDATED                 	STATUS  	CHART                                     	NAMESPACE     
 * ausf-release   	1       	Fri Jun  1 08:53:28 2018	DEPLOYED	ausf-demo-0.0.7                           	5g-integration
 * heapster       	1       	Wed Mar 28 15:21:26 2018	DEPLOYED	heapster-0.2.1                            	kube-system   
 * nfs-provisioner	1       	Wed Mar 28 15:19:54 2018	DEPLOYED	nfs-provider-0.1.1                        	default       
 * nrf-release    	1       	Tue May 29 09:45:12 2018	DEPLOYED	ericsson-nrf-release-0.4.2-integrationtest	5g-integration
 * nssf-release   	1       	Fri Jun  1 19:06:22 2018	DEPLOYED	nssf-0.1.0                                	5g-integration
 * udm-release    	1       	Fri Jun  1 17:45:49 2018	DEPLOYED	udm-0.0.4                                 	5g-integration
 * udr-release    	1       	Wed May 30 17:01:40 2018	DEPLOYED	renegadedb-release-0.1.55                 	5g-integration
 *}
 */
public class HelmRelease {

    private String releaseName;
    private int revision;
    private Date updated;
    private HelmReleaseStatus status;
    private String chart;
    private String namespace;

    public String getReleaseName() {
        return releaseName;
    }

    public void setReleaseName(final String releaseName) {
        this.releaseName = releaseName;
    }

    public int getRevision() {
        return revision;
    }

    public void setRevision(final int revision) {
        this.revision = revision;
    }

    public Date getUpdated() {
        return updated;
    }

    public void setUpdated(final Date updated) {
        this.updated = updated;
    }

    public HelmReleaseStatus getStatus() {
        return status;
    }

    public void setStatus(final HelmReleaseStatus status) {
        this.status = status;
    }

    public String getChart() {
        return chart;
    }

    public void setChart(final String chart) {
        this.chart = chart;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(final String namespace) {
        this.namespace = namespace;
    }

}
