/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2018 - 2022
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.eson.test.operators;

import static com.ericsson.oss.services.eson.test.util.CronExpressionUtility.generateCronExpression;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.de.tools.cli.CliCommandResult;
import com.ericsson.oss.itpf.sdk.core.retry.RetriableCommand;
import com.ericsson.oss.itpf.sdk.core.retry.RetryContext;
import com.ericsson.oss.itpf.sdk.core.retry.RetryManager;
import com.ericsson.oss.itpf.sdk.core.retry.RetryPolicy;
import com.ericsson.oss.itpf.sdk.core.retry.classic.RetryManagerNonCDIImpl;
import com.ericsson.oss.services.eson.test.constants.common.DeploymentConstants;
import com.ericsson.oss.services.eson.test.exceptions.DeploymentException;
import com.ericsson.oss.services.eson.test.handlers.EccdCliHandler;
import com.ericsson.oss.services.eson.test.helm.HelmException;
import com.ericsson.oss.services.eson.test.helm.HelmRelease;
import com.ericsson.oss.services.eson.test.helm.HelmShellOutputParser;
import com.ericsson.oss.services.eson.test.kubectl.Deployment;
import com.ericsson.oss.services.eson.test.kubectl.KubectlParserUtil;
import com.ericsson.oss.services.eson.test.kubectl.Pod;
import com.ericsson.oss.services.eson.test.util.CheckResult;

public class DeploymentCliOperator implements DeploymentOperator {

    private static String message = "";

    private static final int MAX_RETRY_ATTEMPTS = 15;
    private static final int WAIT_INTERVAL = 1;
    private static final String FMKGBSUITE = "FlmKgbSuite.xml";
    private static final String AASKGBSUITE = "AasKgbSuite.xml";
    private static final String AASPIPELINESUITE = "AasPipelineSuite.xml";

    private static final Logger logger = LoggerFactory.getLogger(DeploymentCliOperator.class);

    private static final String KUBE_CONFIG = System.getProperty("kube_config");

    private static final String SUITE = System.getProperty("suites");

    public static String getLoadbalancerIP(final EccdCliHandler eccdHandler) {
        final String eccdLbIp = System.getProperty("eccd_lb_ip");
        if (!eccdLbIp.isEmpty()) {
            return eccdLbIp;
        } else {
            final String cmd = String.format(DeploymentConstants.KUBECTL_API_INGRESS_HOST, getKubeConfig());
            logger.info(cmd);
            final CliCommandResult helmResult = eccdHandler.execute(cmd);
            if (CheckResult.isReturningError(helmResult)) {
                message = "Failed to get host name for deployment";
                logger.error(message);
                return message;
            }
            logger.info(helmResult.getOutput());
            return getIpAddress(helmResult);
        }
    }

    private static String getIpAddress(final CliCommandResult helmResult) {
        final String hostname = getHostName(helmResult.getOutput());
        InetAddress ipAddress = null;
        try {
            ipAddress = InetAddress.getByName(hostname);
        } catch (final UnknownHostException e) {
            logger.error("Failed to get load balancer IP for deployment {}", e.getMessage());
        }
        if (ipAddress != null) {
            return ipAddress.getHostAddress();
        }
        return "IP Address could not be retrieved";
    }

    private static String getHostName(final String ingressString) {
        final String regex = "\\bapi\\..*se\\b";
        final Pattern p = Pattern.compile(regex);
        final Matcher m = p.matcher(ingressString);
        if (m.find()) {
            return m.group().trim();
        } else {
            return "Host could not be found";
        }
    }

    public static String decodeSecret(final String db_secret_name, final EccdCliHandler eccdHandler) {
        final String cmd = String.format(DeploymentConstants.KUBECTL_GET_DB_PW_FROM_SECRET, db_secret_name, System.getProperty("namespace", "sonom"),
                getKubeConfig());
        final CliCommandResult helmResult = eccdHandler.execute(cmd);
        if (CheckResult.isReturningError(helmResult)) {
            message = String.format("Failed to execute command %s", cmd);
            logger.error(message);
            return message;
        }
        return helmResult.getOutput();
    }

    @Override
    public boolean verifySuccessfulDeployment(final String namespace, final EccdCliHandler eccdHandler) {
        return checkSuccessfulDeployment(namespace, eccdHandler);

    }

    @Override
    public boolean verifyDeploymentsAvailable(final String namespace, final EccdCliHandler eccdHandler) {
        return checkDeploymentsAreAvailable(namespace, eccdHandler);
    }

    @Override
    public boolean verifyPodsRunning(final String namespace, final EccdCliHandler eccdHandler) {
        return checkPodsAreRunning(namespace, eccdHandler);
    }

    public static String getExternalViewLoadBalancer(final String service, final EccdCliHandler eccdHandler) {
        final String cmd = String.format(DeploymentConstants.KUBECTL_GET_EXTERNAL_VIEW_LB, service, System.getProperty("namespace", "sonom"),
                getKubeConfig());
        final CliCommandResult helmResult = eccdHandler.execute(cmd);
        if (CheckResult.isReturningError(helmResult)) {
            message = String.format("Failed to execute command %s", cmd);
            logger.error(message);
            return message;
        }
        return helmResult.getOutput();
    }

    private static boolean checkSuccessfulDeployment(final String namespace, final EccdCliHandler eccdHandler) {
        final String cmd = String.format(DeploymentConstants.HELM_LIST_DEPLOYMENT_AT_NAMESPACE, namespace, getKubeConfig());
        final CliCommandResult helmResult = eccdHandler.execute(cmd);
        if (CheckResult.isReturningError(helmResult)) {
            message = "Failed to execute helm command to list deployment";
            logger.error(message);
            return false;
        }

        logger.info(helmResult.getOutput());

        try {
            final List<HelmRelease> helmReleases = HelmShellOutputParser.parseHelmReleases(helmResult.getOutput());
            return isSuccessfullyDeployed(helmReleases);
        } catch (final HelmException e) {
            message = String.format("Error parsing Helm output %s", e);
            logger.error(message);
        }
        return false;
    }

    public static boolean setSchedulesOnDeploymentForPmStatsEvents(final String namespace, final int delay, final EccdCliHandler eccdHandler,
            final String installDir, final String releaseName) {
        final String pmStatsAndEventsCronSchedule = generateCronExpression(delay);
        final String pmEventsCronSchedule = "eric-pm-events-processor-er.schedule_expression.value=" + "'" + pmStatsAndEventsCronSchedule + "'";
        final String pmStatsCronSchedule = "eric-pm-stats-processor-er.pmStatsProcessorEnm.scheduleExpression=" + "'" + pmStatsAndEventsCronSchedule
                + "'";
        final String cmRefreshInitInterval = "eric-pm-events-processor-er.images.eric-pm-events-processor-er.cmRefreshInitialIntervalDurationInMs=180000";
        final String cmRefreshInterval = "eric-pm-events-processor-er.spark.cmStreamRefreshInterval=420000";

        final String sparkSqlMinBatchesToRetain = "eric-pm-events-processor-er.spark.sparkSqlMinBatchesToRetain=25";
        final String cellPipelineWatermarkDelay = "eric-pm-events-processor-er.spark.cellPipelineWatermarkDelay='28 minutes'";
        final String adjacencyPipelineWatermarkDelay = "eric-pm-events-processor-er.spark.adjacencyPipelineWatermarkDelay='40 minutes'";
        final String cmCellWatermarkDelay = "eric-pm-events-processor-er.spark.cmCellWatermarkDelay='30 minutes'";
        final String cmRelationWatermarkDelay = "eric-pm-events-processor-er.spark.cmRelationWatermarkDelay='40 minutes'";

        final String triggerProcessingTimeSeconds = "eric-pm-events-processor-er.spark.triggerProcessingTimeSeconds='0'";

        String sparkDriverMemory;
        if (SUITE.equalsIgnoreCase(AASKGBSUITE) || SUITE.equalsIgnoreCase(AASPIPELINESUITE)) {
            sparkDriverMemory = "eric-pm-events-processor-er.spark.driver.memory='4g'";
        } else {
            sparkDriverMemory = "eric-pm-events-processor-er.spark.driver.memory='7g'";
            sparkDriverMemory = sparkDriverMemory + ",eric-pm-events-processor-er.pmEventsProcessorEnm.resources.limits.memory='8Gi'";
            sparkDriverMemory = sparkDriverMemory + ",eric-pm-events-processor-er.pmEventsProcessorEnm.resources.requests.memory='7Gi'";
        }

        final String sparkExecutorMemory = "eric-pm-events-processor-er.spark.executor.memory='3g'";

        // new adjustments
        final String pipelineTimeout = "eric-pm-events-processor-er.spark.counterPipelineTimeout='3 minutes'";
        final String maxOffsetPerTrigger = "eric-pm-events-processor-er.spark.maxOffsetsPerTrigger='100000'";
        final String timeToLive = "eric-pm-events-processor-er.spark.checkpoint.timeToLive=0";

        // suite
        logger.info("Testing logger :suite picked up {}", SUITE);
        String enableAdjacencyPipeline = "eric-pm-events-processor-er.spark.sparkEnableAdjacency=true";
        if (SUITE.equalsIgnoreCase(FMKGBSUITE)) {
            enableAdjacencyPipeline = "eric-pm-events-processor-er.spark.sparkEnableAdjacency=false";
        }
        logger.info("Testing logger :enableAdjacencyPipeline {}", enableAdjacencyPipeline);

        final StringBuilder helmSetAllSchedules = new StringBuilder();
        if (SUITE.equalsIgnoreCase(AASPIPELINESUITE)) {
            helmSetAllSchedules.append(" --set ").append(pmEventsCronSchedule).append(",").append(pmStatsCronSchedule);
        } else {
            helmSetAllSchedules.append(" --set ").append(pmEventsCronSchedule).append(",").append(pmStatsCronSchedule).append(",")
                    .append(cmRefreshInitInterval).append(",").append(cmRefreshInterval).append(",").append(sparkSqlMinBatchesToRetain).append(",")
                    .append(cellPipelineWatermarkDelay).append(",").append(adjacencyPipelineWatermarkDelay).append(",").append(cmCellWatermarkDelay)
                    .append(",").append(cmRelationWatermarkDelay).append(",").append(pipelineTimeout).append(",").append(maxOffsetPerTrigger)
                    .append(",").append(sparkDriverMemory).append(",").append(sparkExecutorMemory).append(",").append(enableAdjacencyPipeline)
                    .append(",").append(triggerProcessingTimeSeconds).append(",").append(timeToLive);
        }

        return helmUpgrade(namespace, eccdHandler, helmSetAllSchedules.toString(), installDir, releaseName);
    }

    public static boolean setSchedulesOnDeploymentForPmEvents(final String namespace, final int delay, final EccdCliHandler eccdHandler,
            final String installDir, final String releaseName) {
        final String pmStatsAndEventsCronSchedule = generateCronExpression(delay);
        final String pmEventsCronSchedule = "eric-pm-events-processor-er.schedule_expression.value=" + "'" + pmStatsAndEventsCronSchedule + "'";
        final String cmRefreshInitInterval = "eric-pm-events-processor-er.images.eric-pm-events-processor-er.cmRefreshInitialIntervalDurationInMs=180000";
        final String cmRefreshInterval = "eric-pm-events-processor-er.spark.cmStreamRefreshInterval=420000";

        final String sparkSqlMinBatchesToRetain = "eric-pm-events-processor-er.spark.sparkSqlMinBatchesToRetain=25";
        final String cellPipelineWatermarkDelay = "eric-pm-events-processor-er.spark.cellPipelineWatermarkDelay='28 minutes'";
        final String adjacencyPipelineWatermarkDelay = "eric-pm-events-processor-er.spark.adjacencyPipelineWatermarkDelay='40 minutes'";
        final String cmCellWatermarkDelay = "eric-pm-events-processor-er.spark.cmCellWatermarkDelay='30 minutes'";
        final String cmRelationWatermarkDelay = "eric-pm-events-processor-er.spark.cmRelationWatermarkDelay='40 minutes'";

        final String triggerProcessingTimeSeconds = "eric-pm-events-processor-er.spark.triggerProcessingTimeSeconds='0'";

        final String sparkDriverMemory;
        if (SUITE.equalsIgnoreCase(AASKGBSUITE) || SUITE.equalsIgnoreCase(AASPIPELINESUITE)) {
            sparkDriverMemory = "eric-pm-events-processor-er.spark.driver.memory='4g'";
        } else {
            sparkDriverMemory = "eric-pm-events-processor-er.spark.driver.memory='2g'";
        }

        final String sparkExecutorMemory = "eric-pm-events-processor-er.spark.executor.memory='3g'";

        // new adjustments
        final String pipelineTimeout = "eric-pm-events-processor-er.spark.counterPipelineTimeout='3 minutes'";
        final String maxOffsetPerTrigger = "eric-pm-events-processor-er.spark.maxOffsetsPerTrigger='100000'";
        final String timeToLive = "eric-pm-events-processor-er.spark.checkpoint.timeToLive=0";

        // suite
        logger.info("Testing logger :suite picked up {}", SUITE);
        String enableAdjacencyPipeline = "eric-pm-events-processor-er.spark.sparkEnableAdjacency=true";
        if (SUITE.equalsIgnoreCase(FMKGBSUITE)) {
            enableAdjacencyPipeline = "eric-pm-events-processor-er.spark.sparkEnableAdjacency=false";
        }
        logger.info("Testing logger :enableAdjacencyPipeline {}", enableAdjacencyPipeline);

        final StringBuilder helmSetAllSchedules = new StringBuilder();
        if (SUITE.equalsIgnoreCase(AASPIPELINESUITE)) {
            helmSetAllSchedules.append(" --set ").append(pmEventsCronSchedule);
        } else {
            helmSetAllSchedules.append(" --set ").append(pmEventsCronSchedule).append(",")
                    .append(cmRefreshInitInterval).append(",").append(cmRefreshInterval).append(",").append(sparkSqlMinBatchesToRetain).append(",")
                    .append(cellPipelineWatermarkDelay).append(",").append(adjacencyPipelineWatermarkDelay).append(",").append(cmCellWatermarkDelay)
                    .append(",").append(cmRelationWatermarkDelay).append(",").append(pipelineTimeout).append(",").append(maxOffsetPerTrigger)
                    .append(",").append(sparkDriverMemory).append(",").append(sparkExecutorMemory).append(",").append(enableAdjacencyPipeline)
                    .append(",").append(triggerProcessingTimeSeconds).append(",").append(timeToLive);
        }

        return helmUpgrade(namespace, eccdHandler, helmSetAllSchedules.toString(), installDir, releaseName);
    }

    private static boolean helmUpgrade(final String namespace, final EccdCliHandler eccdHandler, final String helmSetAllSchedules,
            final String installDir, final String releaseName) {
        final String cmd = String.format(DeploymentConstants.HELM_UPGRADE_DEPLOYMENT, releaseName, installDir, namespace, helmSetAllSchedules,
                getKubeConfig());
        logger.info(cmd);
        final CliCommandResult helmResult = eccdHandler.execute(cmd);
        if (CheckResult.isReturningError(helmResult)) {
            message = "Failed to execute helm command to list deployment";
            logger.error(message);
            return false;
        }
        logger.info(helmResult.getOutput());
        return true;
    }

    private static boolean checkDeploymentsAreAvailable(final String namespace, final EccdCliHandler eccdHandler) {
        final RetriableCommand<Boolean> command = checkDeploymentStatus(namespace, eccdHandler);
        final RetryManager retryManager = new RetryManagerNonCDIImpl();
        final RetryPolicy retryPolicy = RetryPolicy.builder().attempts(MAX_RETRY_ATTEMPTS).waitInterval(WAIT_INTERVAL, TimeUnit.MINUTES).build();
        return retryManager.executeCommand(retryPolicy, command);
    }

    public static Boolean verifyStringWithKubectlPodLoop(final String namespace, final EccdCliHandler eccdHandler, final String containerName,
            final String searchString) {
        return verifyStringWithKubectlPodLoop(namespace, eccdHandler, containerName, searchString, null, 25);
    }

    public static Boolean verifyStringWithKubectlPodLoop(final String namespace, final EccdCliHandler eccdHandler, final String containerName,
            final String searchString, final Timestamp afterTimestamp, final int delayMinutes) {
        final long start = System.currentTimeMillis();
        final long end = start + TimeUnit.MINUTES.toMillis(delayMinutes);
        if (afterTimestamp == null) {
            logger.info("Starting log search for '{}', waiting for '{}' minutes", searchString, delayMinutes);
        } else {
            logger.info("Starting log search for '{}', occurring after timestamp '{}', waiting for '{}' minutes", searchString, afterTimestamp,
                    delayMinutes);
        }
        while (true) {
            try {
                // Need to check the pod names each time in case they were terminating/restarting
                final String cmd = String.format(DeploymentConstants.KUBECTL_GET_SPECIFIC_PODS_AT_NAMESPACE, namespace, getKubeConfig(),
                        containerName);
                final CliCommandResult kubectlResult = eccdHandler.execute(cmd);
                final String[] pods = kubectlResult.getOutput().split("\n");

                if (System.currentTimeMillis() > end) {
                    logger.error("Kubectl log search exceeded time allowed for string {}.", searchString);
                    message = String.format("Kubectl log search exceeded time allowed for string  %s.", searchString);
                    logger.error(kubectlResult.getOutput());
                    logger.error(message);
                    return false;
                }

                for (final String pod : pods) {
                    final String cmdPod = String.format(DeploymentConstants.KUBECTL_GET_LOGS_FOR_POD, pod, containerName, namespace, getKubeConfig(),
                            searchString);
                    final CliCommandResult kubectlResultPod = eccdHandler.execute(cmdPod);
                    final String logOutput = kubectlResultPod.getOutput();
                    final String[] outputArray = logOutput.split("\\n");

                    if (kubectlResultPod.getOutput().replaceAll("\\n", "").contains(searchString)) {
                        if (afterTimestamp == null) {
                            message = String.format("Kubectl log search found occurrences of: %s.", kubectlResultPod.getOutput());
                            logger.info(message);
                            return true;
                        } else {
                            for (final String line : outputArray) {
                                try {
                                    final String logTime = line.substring(1, 24).replace("T", " ");
                                    final Timestamp logTimestamp = Timestamp.valueOf(logTime);
                                    if (logTimestamp.after(afterTimestamp)) {
                                        message = String.format("Kubectl log search found occurrences (after timestamp %s) of: %s.", afterTimestamp,
                                                line);
                                        logger.info(message);
                                        return true;
                                    }
                                } catch (final Exception e) {
                                    logger.info("Log output that does not start with a logger timestamp: {}", line);
                                }
                            }
                        }
                    }
                }
                final int SLEEP_TIME = 15;
                TimeUnit.SECONDS.sleep(SLEEP_TIME);
            } catch (final InterruptedException e) {
                logger.error("Error finding value in log '{}'", e.getMessage());
                message = String.format("Error finding string '%s' in log.", searchString);
                Thread.currentThread().interrupt();
                return false;
            }
        }
    }

    public static boolean setEnvForDeployment(final String namespace, final EccdCliHandler eccdHandler, final String deployName,
            final Map<String, String> envPAVariables) {
        /// "usr/local/bin/kubectl set env deploy %s %s --namespace %s --kubeconfig=%s"
        final StringBuilder envVarVal = new StringBuilder();
        for (final Map.Entry<String, String> envEntry : envPAVariables.entrySet()) {
            envVarVal.append(envEntry.getKey());
            envVarVal.append("=");
            envVarVal.append(envEntry.getValue());
            envVarVal.append(" ");

        }
        final String cmdPod = String.format(DeploymentConstants.KUBECTL_ADD_ENV_TO_FLM_DEPLOYMENT_AT_NAMESPACE, deployName, envVarVal.toString(),
                namespace, getKubeConfig());

        final CliCommandResult kubectlResultPod = eccdHandler.execute(cmdPod);
        logger.info("PA Environment settings setup: " + cmdPod + "\n" + kubectlResultPod.getOutput());
        return true;
    }

    @Override
    public boolean rescalePod(final int replicaCount, final String statefulset, final String namespace, final EccdCliHandler eccdHandler)
            throws DeploymentException {
        final String cmd = String.format(DeploymentConstants.KUBECTL_SCALE_POD, replicaCount, statefulset, namespace);
        logger.info("Re-scaling spark pod with command: '{}' ", cmd);
        final CliCommandResult kubectlResult = eccdHandler.execute(cmd);
        if (CheckResult.isReturningError(kubectlResult)) {
            message = "Failed to execute kubectl command to scale pod";
            logger.error(message);
            throw new DeploymentException(message);
        }
        logger.info(kubectlResult.getOutput());
        return true;
    }

    private static List<Deployment> getDeployments(final String namespace, final EccdCliHandler eccdHandler) throws DeploymentException {
        final String cmd = String.format(DeploymentConstants.KUBECTL_GET_DEPLOYMENTS_AT_NAMESPACE, namespace, getKubeConfig());
        final CliCommandResult kubectlResult = eccdHandler.execute(cmd);
        if (CheckResult.isReturningError(kubectlResult)) {
            message = "Failed to execute kubectl command to get deployment";
            logger.error(message);
            throw new DeploymentException(message);
        }

        logger.info(kubectlResult.getOutput());
        return KubectlParserUtil.parseDeploymentList(kubectlResult.getOutput());
    }

    private static RetriableCommand<Boolean> checkDeploymentStatus(final String namespace, final EccdCliHandler eccdHandler) {
        return new RetriableCommand<Boolean>() {
            @Override
            public Boolean execute(final RetryContext retryContext) throws DeploymentException {
                logger.info("Attempt {} of {}, checking if all deployments are available", retryContext.getCurrentAttempt(), MAX_RETRY_ATTEMPTS);

                final List<Deployment> deployments = getDeployments(namespace, eccdHandler);
                if (areDeploymentsAvailable(deployments)) {
                    return true;
                }

                if (MAX_RETRY_ATTEMPTS == retryContext.getCurrentAttempt()) {
                    return false;
                }
                throw new DeploymentException("Not all deployments are available");
            }
        };
    }

    private static boolean checkPodsAreRunning(final String namespace, final EccdCliHandler eccdHandler) {
        final String cmd = String.format(DeploymentConstants.KUBECTL_GET_PODS_AT_NAMESPACE, namespace, getKubeConfig());
        final CliCommandResult kubectlResult = eccdHandler.execute(cmd);
        if (CheckResult.isReturningError(kubectlResult)) {
            message = "Failed to execute kubectl command to get pods";
            logger.error(message);
            return false;
        }

        logger.info(kubectlResult.getOutput());

        final List<Pod> pods = KubectlParserUtil.parsePodList(kubectlResult.getOutput());
        return arePodsRunning(pods);
    }

    private static boolean isSuccessfullyDeployed(final List<HelmRelease> helmReleases) {
        for (final HelmRelease helmRelease : helmReleases) {
            if (!helmRelease.getStatus().isDeployed()) {
                message = String.format("Deployment Name %s Failed to Deploy", helmRelease.getReleaseName());
                logger.error(message);
                return false;
            }
            logger.info("Deployed : {}", helmRelease.getReleaseName());
        }
        return true;

    }

    private static boolean areDeploymentsAvailable(final List<Deployment> deployments) {
        for (final Deployment deployment : deployments) {
            if (deployment.getDesired() != deployment.getAvailable()) {
                message = String.format("Not all deployments for %s are Available, Desired:%s  Available:%s", deployment.getName(),
                        deployment.getDesired(), deployment.getAvailable());
                logger.error(message);
                return false;
            }
            logger.info("All deployments for {} are Available", deployment.getName());
        }
        return true;

    }

    private static boolean arePodsRunning(final List<Pod> pods) {
        for (final Pod pod : pods) {
            if (!(pod.getStatus().contains("Running") || pod.getStatus().contains("Completed"))) {
                message = String.format("Pod %s is Not Running. STATUS:%s", pod.getName(), pod.getStatus());
                logger.error(message);
                return false;
            }
            logger.info("Pod {} is {}", pod.getName(), pod.getStatus());
        }
        return true;
    }

    public static String getKubeConfig() {
        if (KUBE_CONFIG == null || KUBE_CONFIG.isEmpty()) {
            return "$HOME/.kube/config";
        } else {
            return "/tmp/" + KUBE_CONFIG + "/.kube/config";
        }
    }

    public static String getMessage() {
        return message;
    }

}
