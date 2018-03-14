package com.nullendpoint.jolokia.aggregator.http

import io.fabric8.kubernetes.api.model.PodList
import io.fabric8.kubernetes.client.DefaultKubernetesClient
import org.apache.camel.Exchange
import org.apache.camel.Processor
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

/**
 * Created by swinchester on 6/07/16.
 */
@Component(value="kubeDiscoveryProcessor")
class KubeDiscoveryProcessor implements Processor{

    DefaultKubernetesClient client;

    Logger log = LoggerFactory.getLogger(this.class)

    @Value('${openshift.cluster.host}')
    String clusterHost;

    @Override
    void process(Exchange exchange) throws Exception {

        String serverUrl = "https://${clusterHost}:8443"
        String namespace = exchange.in.getHeader("kube-namespace") ?: "jolokia"
        String labelFilter = exchange.in.getHeader("kube-label") ?: "type=amq"

        def labelList = labelFilter.split("=")
        String labelName = labelList[0]
        String labelValue = labelList[1]

//        def serverUrl = exchange.in.headers.'RequestHost' ?:"https://kubernetes.default.svc"
        log.debug("about to call kubernetes to look for ${labelFilter} in ${namespace}")
        client = new DefaultKubernetesClient(serverUrl)
        PodList kubePods = client.inNamespace(namespace).pods().withLabel(labelName,labelValue).list()
        log.debug("called kubernetes and got a podlist")
        def podNames = kubePods.items.collect { it.metadata.name }
        log.debug("pod names matching label (${labelName}=${labelValue}) are : ${podNames}")
        exchange.in.headers.put("podNames", podNames)

    }
}
