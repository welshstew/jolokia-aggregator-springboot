package org.swinchester.metrics.jolokia.api.buildconfig;

import io.fabric8.kubernetes.api.model.IntOrString;
import io.fabric8.openshift.api.model.TemplateBuilder;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by swinchester on 30/08/16.
 */
public class ServiceKubernetesModelProcessor {

    public void on(TemplateBuilder builder) {
        builder.addNewServiceObject()
                .withNewMetadata()
                .withLabels(getLabels())
                .withName("jolokia-aggregator-svc")
                .endMetadata()
                .withNewSpec()
                .addNewPort()
                    .withProtocol("TCP")
                    .withPort(ConfigParameters.CONTAINER_PORT)
                    .withTargetPort(new IntOrString(ConfigParameters.CONTAINER_PORT))
                .endPort()
                .withSelector(getSelectors())
                .endSpec()
                .endServiceObject().build();
    }

    private Map<String, String> getLabels() {
        Map<String, String> selectors = new HashMap<>();
        selectors.put("project", "jolokia-aggregator-api");
        selectors.put("provider", "org.swinchester.metrics");
        selectors.put("version", "1.0-SNAPSHOT");
        selectors.put("group", "api");
        return selectors;
    }

    private Map<String, String> getSelectors() {
        Map<String, String> selectors = new HashMap<>();
        selectors.put("deploymentconfig", "jolokia-aggregator-api");
        return selectors;
    }
}
