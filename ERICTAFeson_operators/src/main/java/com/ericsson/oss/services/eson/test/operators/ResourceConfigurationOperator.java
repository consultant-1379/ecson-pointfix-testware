/*
 *------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson  2021
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *------------------------------------------------------------------------------
 */

package com.ericsson.oss.services.eson.test.operators;

import static com.ericsson.oss.services.eson.test.custom.report.CustomReportHelper.CUSTOM_RESOURCE_CONFIGURATION;
import static com.ericsson.oss.services.eson.test.custom.report.CustomReportHelper.setCustomAttributes;
import static com.ericsson.oss.services.eson.test.custom.report.CustomReportStatus.valueOf;

import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.commonlibrary.utilities.Utilities;
import com.ericsson.de.tools.cli.CliCommandResult;
import com.ericsson.oss.services.eson.test.constants.common.DeploymentConstants;
import com.ericsson.oss.services.eson.test.eccd.EccdHostGroup;
import com.ericsson.oss.services.eson.test.handlers.EccdCliHandler;
import com.ericsson.oss.services.eson.test.util.CheckResult;

public class ResourceConfigurationOperator {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResourceConfigurationOperator.class);

    private final EccdCliHandler cliHandler = new EccdCliHandler(EccdHostGroup.getEccdDrector());
    private static final String DIMENSION_REGEX = "((?<=[0-9])(?=[a-zA-Z]))";
    private static final double BYTE_TO_GIB = 1073741824;
    private static final String BREAK = "<br>";
    private static final String NAMESPACE = EccdHostGroup.getByAttribute("eccd.namespace");
    private static final String DOCUMENT = "Document :";
    private static final String DEPLOYMENT = "Deployment :";
    private String podName = "";
    private String testMessage = "";

    /**
     * Verify CSAR Resource Dimensions.
     * @param filter filter
     * @param containerName containerName
     * @param baselineRequestCpu baselineRequestCpu
     * @param baselineLimitCpu baselineLimitCpu
     * @param baselineRequestMemory baselineRequestMemory
     * @param baselineLimitMemory baselineLimitMemory
     * @param baselinePersistentStorage baselinePersistentStorage
     * @return overallTestResult
     */
    public boolean verifyResourceConfiguration(final String filter, final String containerName,
            final String baselineRequestCpu, final String baselineLimitCpu,
            final String baselineRequestMemory, final String baselineLimitMemory,
            final String baselinePersistentStorage) {

        final CliCommandResult result = cliHandler.executeCommand(
                String.format(DeploymentConstants.KUBECTL_GET_POD_FROM_LABEL, NAMESPACE, filter));
        final List<String> podNameListFromLabel = Arrays.asList(result.getOutput().split("\n"));
        LOGGER.info("Running command {} to find all pods in release {}",String.format(DeploymentConstants.KUBECTL_GET_POD_FROM_LABEL, NAMESPACE, filter),filter);
        LOGGER.info("***************Pod list size {} in release {} from command {}****************",podNameListFromLabel.size(),filter, String.format(DeploymentConstants.KUBECTL_GET_POD_FROM_LABEL, NAMESPACE, filter));


        final List<String> podNameList = Arrays.asList(containerName.split(":"));
        for (String pod : podNameListFromLabel) {
            LOGGER.info("Pods from release label command {} {} ",String.format(DeploymentConstants.KUBECTL_GET_POD_FROM_LABEL, NAMESPACE, filter),pod);
        }
        for (String pod : podNameList) {
            if (!podNameListFromLabel.toString().contains(pod))
                LOGGER.warn("Pod {} missing label in helm chart ",pod);
        }
        final StringBuilder errorMessage = new StringBuilder();
        final DecimalFormat decimalFormat = new DecimalFormat("####.##");
        boolean testResult = true;
        boolean overallTestResult = true;

        final StringBuilder requestCpu = new StringBuilder();
        final StringBuilder limitCpu = new StringBuilder();
        final StringBuilder requestMemory = new StringBuilder();
        final StringBuilder limitMemory = new StringBuilder();
        final StringBuilder persistenceStorage = new StringBuilder();

        long actualRequestCpu = 0L;
        long actualLimitCpu = 0L;
        double actualRequestMemory = 0L;
        double actualLimitMemory = 0L;
        double actualPersistentStorage = 0L;
        int actualStorageReplica = 0;
        int totalPods = 0;

        for (final String podNameData : podNameList) {

            podName = podNameData;
            final String podResourceOutput = getPodResourceData();

            if (Utilities.string().isNotBlank(podResourceOutput)) {

                String pvcResourceOutput = getPvcResourceData();
                if (StringUtils.isBlank(pvcResourceOutput)) {
                    pvcResourceOutput = podNameData + ",0Mi";
                } else {
                    actualStorageReplica = getPvcReplicas();
                }
                final int actualReplica = getPodReplicas();
                final String[] podResource = podResourceOutput.split(",");
                final String[] pvcResource = pvcResourceOutput.split(",");
                long requestCpuPerPod = parseCpuValue(podResource[1].trim()) * actualReplica;
                actualRequestCpu += requestCpuPerPod;
                long actualLimitCpuPerPod = parseCpuValue(podResource[2].trim()) * actualReplica;
                actualLimitCpu += actualLimitCpuPerPod;
                double actualRequestMemoryPerPod = parseMemoryValue(podResource[3].trim()) * actualReplica;
                actualRequestMemory += actualRequestMemoryPerPod;
                double actualLimitMemoryPerPod = parseMemoryValue(podResource[4].trim()) * actualReplica;
                actualLimitMemory += actualLimitMemoryPerPod;
                double actualPersistentStoragePerPod = parseMemoryValue(pvcResource[1].trim()) * actualStorageReplica;
                actualPersistentStorage += actualPersistentStoragePerPod;
                LOGGER.info("{}:({}) Request CPU: {} Limit CPU: {} Request Memory: {}GiB Limit Memory: {}GiB Storage: {}GiB",podName,actualReplica,requestCpuPerPod,actualLimitCpuPerPod,decimalFormat.format(actualRequestMemoryPerPod),decimalFormat.format(actualLimitMemoryPerPod),decimalFormat.format(actualPersistentStoragePerPod));
                totalPods = totalPods + actualReplica;
            } else {
                testMessage = String.format("No data available for %s", podName);
                errorMessage.append(testMessage).append(BREAK);
                testResult = false; overallTestResult = false;
            }
        }
        LOGGER.info("**************Pod list size {} in release {} from csv file*******************",totalPods,filter);
        requestCpu.append(DOCUMENT).append(parseCpuValue(baselineRequestCpu)).append("m").append(BREAK).append(DEPLOYMENT).append(actualRequestCpu).append("m").append(BREAK);
        limitCpu.append(DOCUMENT).append(parseCpuValue(baselineLimitCpu)).append("m").append(BREAK).append(DEPLOYMENT).append(actualLimitCpu).append("m").append(BREAK);
        requestMemory.append(DOCUMENT).append(parseMemoryValue(baselineRequestMemory)).append("GiB").append(BREAK).append(DEPLOYMENT).append(decimalFormat.format(actualRequestMemory)).append("GiB").append(BREAK);
        limitMemory.append(DOCUMENT).append(parseMemoryValue(baselineLimitMemory)).append("GiB").append(BREAK).append(DEPLOYMENT).append(decimalFormat.format(actualLimitMemory)).append("GiB").append(BREAK);
        persistenceStorage.append(DOCUMENT).append(parseMemoryValue(baselinePersistentStorage)).append("GiB").append(BREAK).append(DEPLOYMENT).append(decimalFormat.format(actualPersistentStorage)).append("GiB").append(BREAK);

        if (Double.parseDouble(decimalFormat.format(actualRequestCpu)) != parseCpuValue(baselineRequestCpu)) {
            testMessage = String.format("CPU Request Dimension in Document: %s & in Deployment: %sm", baselineRequestCpu, actualRequestCpu);
            errorMessage.append(testMessage).append(BREAK);
            LOGGER.warn("{}", testMessage);
            testResult = false;
        }
        if (Double.parseDouble(decimalFormat.format(actualLimitCpu)) != parseCpuValue(baselineLimitCpu)) {
            testMessage = String.format("CPU Limit Dimension in Document: %s & in Deployment: %sm", baselineLimitCpu, actualLimitCpu);
            errorMessage.append(testMessage).append(BREAK);
            LOGGER.warn("{}", testMessage);
            testResult = false;
        }
        if (Double.parseDouble(decimalFormat.format(actualRequestMemory)) != parseMemoryValue(baselineRequestMemory)) {
            testMessage = String.format("Memory Request Dimension in Document: %s & in Deployment: %sGiB", baselineRequestMemory, decimalFormat.format(actualRequestMemory));
            errorMessage.append(testMessage).append(BREAK);
            LOGGER.warn("{}", testMessage);
            testResult = false;
        }
        if (Double.parseDouble(decimalFormat.format(actualLimitMemory)) != parseMemoryValue(baselineLimitMemory)) {
            testMessage = String.format("Memory limit Dimension in Document: %s & in Deployment: %sGiB", baselineLimitMemory, decimalFormat.format(actualLimitMemory));
            errorMessage.append(testMessage).append(BREAK);
            LOGGER.warn("{}", testMessage);
            testResult = false;
        }
        if (Double.parseDouble(decimalFormat.format(actualPersistentStorage)) != parseMemoryValue(baselinePersistentStorage)) {
            testMessage = String.format("Persistent Storage Dimension in Document: %s & in Deployment: %sGiB", baselinePersistentStorage, decimalFormat.format(actualPersistentStorage));
            errorMessage.append(testMessage).append(BREAK);
            LOGGER.warn("{}", testMessage);
            testResult = false;
        }
        final String[] customAttributes = {filter, requestCpu.toString(), limitCpu.toString(), requestMemory.toString(), limitMemory.toString(), persistenceStorage.toString(), valueOf(testResult),  errorMessage.toString()};
        setCustomAttributes(CUSTOM_RESOURCE_CONFIGURATION, testResult, customAttributes);
        return overallTestResult;
    }

    private String getPodResourceData() {

        final CliCommandResult result = cliHandler.executeCommand(
                String.format(DeploymentConstants.KUBECTL_GET_POD_RESOURCES, podName, NAMESPACE));
        if (CheckResult.isReturningError(result)) {
            LOGGER.error("Failed to get {} pod resources output", podName);
        }
        return result.getOutput();
    }

    private String getPvcResourceData() {

        final CliCommandResult result = cliHandler.executeCommand(
                String.format(DeploymentConstants.KUBECTL_GET_PVC_RESOURCES, podName, NAMESPACE));

        return result.getOutput();
    }

    private int getPodReplicas() {

        String command = null;
        int podReplicas = 0;

        /* This case is required as grep on eric-nm-repository and eric-sec-access-mgmt needs to exclude other pods
        as they don't have unique filter */
        if (podName.equals("eric-nm-repository") || podName.equals("eric-sec-access-mgmt")) {
            command = String.format(DeploymentConstants.KUBECTL_GET_POD_RESOURCES_COUNT_INVERT,
                    podName, NAMESPACE);
        } else {
            command = String.format(DeploymentConstants.KUBECTL_GET_POD_RESOURCES_COUNT, podName, NAMESPACE);
        }
        final CliCommandResult result = cliHandler.executeCommand(
                command);
        if (CheckResult.isReturningError(result)) {
            LOGGER.error("Failed to get {} pod replicas output", podName);
        } else {
            podReplicas = Integer.parseInt(result.getOutput());
        }
        return podReplicas;
    }

    private int getPvcReplicas() {

        int pvcReplicas = 0;
        final CliCommandResult result = cliHandler.executeCommand(
                String.format(DeploymentConstants.KUBECTL_GET_PVC_RESOURCES_COUNT, podName, NAMESPACE));
        if (CheckResult.isReturningError(result)) {
            LOGGER.error("Failed to get {} pvc replicas output", podName);
        } else {
            pvcReplicas = Integer.parseInt(result.getOutput());
        }
        return pvcReplicas;
    }

    private long parseCpuValue(final String cpuValue) {

        long value = 0;
        if (cpuValue.contains("m")) {
            final String[] cpuArray = cpuValue.trim().split(DIMENSION_REGEX);
            value = Long.parseLong(cpuArray[0]);
        } else {
            final String[] cpuArray = cpuValue.trim().split(DIMENSION_REGEX);
            value = Long.parseLong(cpuArray[0]) * 1000;
        }
        return value;
    }

    private double parseMemoryValue(final String memoryValue) {

        double value = 0;
        if (memoryValue.contains("Mi") || memoryValue.contains("M")) {
            final String[] memoryArray = memoryValue.trim().split(DIMENSION_REGEX);
            value = Double.parseDouble(memoryArray[0]) / 1024;
        } else if (memoryValue.contains("GiB") || memoryValue.contains("Gi")) {
            final String[] memoryArray = memoryValue.trim().split(DIMENSION_REGEX);
            value = Double.parseDouble(memoryArray[0]);
        } else if (memoryValue.contains("m")) {
            final String[] memoryArray = memoryValue.trim().split(DIMENSION_REGEX);
            final double valueInByte = Double.parseDouble(memoryArray[0])/1000;
            value = valueInByte / BYTE_TO_GIB;
        }
        return value;
    }

    /**
     * Error Test Message.
     * @return message
     */
    public String getTestMessage() {
        return testMessage;
    }
}
