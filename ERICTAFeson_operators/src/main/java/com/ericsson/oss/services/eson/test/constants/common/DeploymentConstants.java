/*
 *------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2018 - 2020
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *------------------------------------------------------------------------------
 */
package com.ericsson.oss.services.eson.test.constants.common;

public final class DeploymentConstants {

    private DeploymentConstants() {
    }

    // Kubernetes Constants
    public static final String KUBECTL_GET_DEPLOYMENTS_AT_NAMESPACE = "/usr/local/bin/kubectl get deployments --namespace %s --kubeconfig=%s";

    public static final String KUBECTL_GET_PODS_AT_NAMESPACE = "/usr/local/bin/kubectl get pods --namespace %s --kubeconfig=%s";

    public static final String KUBECTL_GET_LOGS_FOR_POD = "/usr/local/bin/kubectl logs %1$s -c %2$s --namespace %3$s --kubeconfig=%4$s| grep '%5$s'";

    public static final String KUBECTL_GET_SPECIFIC_PODS_AT_NAMESPACE = "/usr/local/bin/kubectl get pods --namespace %1$s --kubeconfig=%2$s|cut -d ' ' -f 1 | grep '%3$s'";

    public static final String KUBECTL_API_INGRESS_HOST = "/usr/local/bin/kubectl get ing --all-namespaces --kubeconfig=%s | grep api";

    public static final String KUBECTL_GET_INGRESS_BY_NAME = "/usr/local/bin/kubectl -n %1$s get ingress %2$s --kubeconfig=%3$s";

    public static final String KUBECTL_GET_SERVICE = "/usr/local/bin/kubectl -n %1$s get service %2$s --kubeconfig=%3$s";

    public static final String KUBECTL_MULTI_POD_LOG_SEARCH_WITH_NAMESPACE_CONTAINER_QUERY = "/usr/local/bin/kubectl logs -n %1$s -l %2$s -c %3$s --kubeconfig=%4$4 --tail=100000| grep '%5$s'";

    public static final String KUBECTL_GET_DB_PW_FROM_SECRET = "/usr/local/bin/kubectl get secret %s -o yaml -n %s --kubeconfig=%s | grep password|cut -d ' ' -f 4|base64 --decode";

    public static final String KUBECTL_DECODE_EVENTS_STATS_POD_DB_PW = "echo '%s' | base64 --decode";

    public static final String KUBECTL_GET_EXTERNAL_VIEW_LB = "/usr/local/bin/kubectl get svc %s -n %s --kubeconfig=%s -o jsonpath={.status.loadBalancer.ingress..ip}";

    public static final String KUBECTL_EXECUTE_COMMAND_ON_POD = "/usr/local/bin/kubectl --kubeconfig=%1$s -n %2$s exec -i -t %3$s -c %4$s -- /bin/bash -c '%5$s'";

    public static final String KUBECTL_SCALE_POD = "/usr/local/bin/kubectl scale --replicas=%1$s statefulset/%2$s -n %3$s";

    public static final String KUBECTL_GET_POD_RESOURCES = "/usr/local/bin/kubectl describe nodes | grep -i \"%s\" | grep -i \"%s\" |  awk '{print $2,\",\",$3,\",\"$5,\",\"$7,\",\"$9}' | sort | head -1";

    public static final String KUBECTL_GET_POD_RESOURCES_COUNT = "/usr/local/bin/kubectl describe nodes | grep -i \"%s\" | grep -i \"%s\" | wc -l";

    public static final String KUBECTL_GET_POD_FROM_LABEL = "/usr/local/bin/kubectl get pods --namespace %s -l=app.kubernetes.io/instance=%s|grep -vi completed|awk '{print $1}'|awk '{if(NR>1)print}'";

    public static final String KUBECTL_GET_POD_RESOURCES_COUNT_INVERT = "/usr/local/bin/kubectl describe nodes | grep -i \"%s\" | grep -i \"%s\" | grep -v -e 'data' -e 'noti' | wc -l";

    public static final String KUBECTL_GET_PVC_RESOURCES = "/usr/local/bin/kubectl get pv | grep -i \"%s\" | grep -E \"%s\" | grep -E 'Bound'| awk '{print $6,\",\"$2}' | awk '{split($0,a,\"/\"); print a[2]}' | sort | head -1";

    public static final String KUBECTL_GET_PVC_RESOURCES_COUNT = "/usr/local/bin/kubectl get pv | grep -i \"%s\" | grep -E \"%s\" | grep -E 'Bound'| awk '{print $6,\",\"$2}' | awk '{split($0,a,\"/\"); print a[2]}' | wc -l";

    public static final String KUBECTL_ADD_ENV_TO_FLM_DEPLOYMENT_AT_NAMESPACE = "/usr/local/bin/kubectl set env deploy %s %s --namespace %s --kubeconfig=%s";

    // Helm Constants
    public static final String HELM_LIST_DEPLOYMENT_AT_NAMESPACE = "/usr/local/bin/helm list --namespace %s --kubeconfig=%s";

    public static final String HELM_UPGRADE_DEPLOYMENT = "/usr/local/bin/helm upgrade --install %s %s --reuse-values --namespace %s %s --kubeconfig=%s";

    public static final String HELM_CREATE_RESOURCE = "/usr/local/bin/kubectl create -f <(echo '%s') --namespace %s --kubeconfig=%s";

    public static final String HELM_DELETE_RESOURCE = "/usr/local/bin/kubectl delete -f <(echo '%s') --namespace %s --kubeconfig=%s";

    // REST End Points OSS Repository Service
    public static final String GET_POST_DELETE_OSS = "son-om/nm-repository/v1/oss";

    // REST End Points CM Service Topology Type Calculation Service
    public static final String PHYSICAL_TOPOLOGY = "son-om/cm-topology/v2/physical-topology";

    public static final String EXTERNAL_MAPPING = "son-om/cm-topology/v2/external-mapping";
    
    public static final String GET_TOPOLOGY_OBJECT = "son-om/cm-topology/v2/topology-objects/%s";

    public static final String REQUIRED_CM_ELEMENTS = "son-om/cm-topology/v2/model";

    public static final String CM_COLLECTIONS_V2 = "son-om/cm-topology/v2/collections";

    // REST End Points Change Service
    public static final String GET_PUT_PROPOSED_CHANGES = "/son-om/cm-change/v1/changes";

    // REST End Points Kpi Service
    public static final String POST_PUT_KPI_DEFINITIONS = "/son-om/kpi/v1/kpis/definitions";

    public static final String POST_KPI_CALCULATION = "/son-om/kpi/v1/kpis/calculation";

    public static final String GET_KPI_CALCULATION_STATE = "/son-om/kpi/v1/kpis/calculation/%s";

    public static final String POST_COUNTER_DEFINITIONS = "/son-om/kpi/v1/model/counters";

}