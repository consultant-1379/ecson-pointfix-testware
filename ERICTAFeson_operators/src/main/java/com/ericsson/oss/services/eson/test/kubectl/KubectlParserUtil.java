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

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.commonlibrary.utilities.Utilities;

/**
 * Used as an parsing utility methods holder.<br>
 * Valid only in case of {@link KubeCtlCliImpl} implementation.
 */
public final class KubectlParserUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(KubectlParserUtil.class);

    /**
     * Private constructor to avoid instantiation of utility class.
     */
    private KubectlParserUtil() {

    }

    /**
     * Parses get pods command output into list of {@link Pod} objects.
     *
     * @param getPodsOutput
     *        raw command output.
     * @return list of Pod objects or an empty list in case of an error.
     */
    public static List<Deployment> parseDeploymentList(final String getDeploymentsOutput) {
        final List<Deployment> deployments = new ArrayList<>();
        if (Utilities.string().isNullOrEmpty(getDeploymentsOutput)) {
            return deployments;
        }

        try (Scanner scanner = new Scanner(getDeploymentsOutput);) {
            scanner.nextLine();
            while (scanner.hasNextLine()) {
                final Deployment deployment = processDeploymentLine(scanner.nextLine());
                if (deployment != null) {
                    deployments.add(deployment);
                }
            }
        }
        return deployments;
    }

    /**
     * Parses one line of get deployments output and creates single {@link Pod} object
     * from it.
     *
     * @param deploymentString
     *        raw line.
     * @return {@link Deployment} object or null in case of an empty line or an error.
     */
    private static Deployment processDeploymentLine(final String deloymentString) {

        if (Utilities.string().isNullOrEmpty(deloymentString)) {
            // Skip emtpy lines
            return null;
        }

        try(final Scanner scanner = new Scanner(deloymentString);){
            scanner.useDelimiter("\\s+");
           if (scanner.hasNext()) {
               final Deployment pod = new Deployment();
               pod.setName(scanner.next());
               String stringToSplit = scanner.next();
               String[] splitString = stringToSplit.split("/");
               pod.setCurrent(Integer.valueOf(splitString[0]));
               pod.setDesired(Integer.valueOf(splitString[1]));
               pod.setUpToDate(scanner.nextInt());
               pod.setAvailable(scanner.nextInt());
               pod.setAge(scanner.next());
               return pod;
           }
        }

        LOGGER.error("Empty or invalid line. Unable to process. Line: {}", deloymentString);
        return null;
    }


    /**
     * Parses get pods command output into list of {@link Pod} objects.
     *
     * @param getPodsOutput
     *        raw command output.
     * @return list of Pod objects or an empty list in case of an error.
     */
    public static List<Pod> parsePodList(final String getPodsOutput) {
        final List<Pod> pods = new ArrayList<>();
        if (Utilities.string().isNullOrEmpty(getPodsOutput)) {
            return pods;
        }

        try (Scanner scanner = new Scanner(getPodsOutput);
                Scanner scannerWithoutHeader = scanner.skip("NAME\\s+READY\\s+STATUS\\s+RESTARTS\\s+AGE");) {
            while (scannerWithoutHeader.hasNextLine()) {
                final Pod pod = processPodLine(scannerWithoutHeader.nextLine());
                if (pod != null) {
                    pods.add(pod);
                }
            }
        }
        return pods;
    }

    /**
     * Parses one line of get pods output and creates single {@link Pod} object
     * from it.
     *
     * @param podString
     *        raw line.
     * @return {@link Pod} object or null in case of an empty line or an error.
     */
    private static Pod processPodLine(final String podString) {

        if (Utilities.string().isNullOrEmpty(podString)) {
            // Skip emtpy lines
            return null;
        }

        try(final Scanner scanner = new Scanner(podString)){
            scanner.useDelimiter("\\s+");
            if (scanner.hasNext()) {
                final Pod pod = new Pod();
                pod.setName(scanner.next());
                pod.setReady(scanner.next());
                pod.setStatus(scanner.next());
                pod.setRestarts(scanner.nextInt());
                pod.setAge(scanner.next());
                return pod;
            }
        }
        LOGGER.error("Empty or invalid line. Unable to process. Line: {}", podString);
        return null;
    }
}
