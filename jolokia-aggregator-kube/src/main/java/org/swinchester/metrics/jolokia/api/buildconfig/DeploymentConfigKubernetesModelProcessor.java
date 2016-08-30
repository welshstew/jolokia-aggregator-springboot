package org.swinchester.metrics.jolokia.api.buildconfig;

import io.fabric8.kubernetes.api.model.*;
import io.fabric8.openshift.api.model.DeploymentConfigBuilder;
import io.fabric8.openshift.api.model.DeploymentTriggerImageChangeParams;
import io.fabric8.openshift.api.model.DeploymentTriggerPolicy;
import io.fabric8.utils.Lists;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DeploymentConfigKubernetesModelProcessor {



    public void on(DeploymentConfigBuilder builder) {
        builder.withSpec(builder.getSpec())
                .withNewMetadata()
                    .withName(ConfigParameters.APP_NAME)
                    .withLabels(getSelectors())
                .endMetadata()
                .editSpec()
                    .withReplicas(1)
                    .withSelector(getSelectors())
                    .withNewStrategy()
                        .withType("Recreate")
                    .endStrategy()
                    .editTemplate()
                        .editSpec()
                            .withContainers(getContainers())
                            .withRestartPolicy("Always")
                            .withVolumes(getVolumes())
                        .endSpec()
                    .endTemplate()
                    .withTriggers(getTriggers())
                .endSpec()
            .build();
    }


    private List<DeploymentTriggerPolicy> getTriggers() {
        DeploymentTriggerPolicy configChange = new DeploymentTriggerPolicy();
        configChange.setType("ConfigChange");

        ObjectReference from = new ObjectReference();
        from.setName(ConfigParameters.APP_NAME + ":${IS_TAG}");
        from.setKind("ImageStreamTag");
        from.setNamespace("${IS_PULL_NAMESPACE}");

        DeploymentTriggerImageChangeParams imageChangeParms = new DeploymentTriggerImageChangeParams();
        imageChangeParms.setFrom(from);
        imageChangeParms.setAutomatic(true);

        DeploymentTriggerPolicy imageChange = new DeploymentTriggerPolicy();
        imageChange.setType("ImageChange");
        imageChange.setImageChangeParams(imageChangeParms);
        imageChangeParms.setContainerNames(Lists.newArrayList(ConfigParameters.APP_NAME));

        List<DeploymentTriggerPolicy> triggers = new ArrayList<DeploymentTriggerPolicy>();
        triggers.add(configChange);
        triggers.add(imageChange);

        return triggers;
    }

    private List<ContainerPort> getPorts() {
        List<ContainerPort> ports = new ArrayList<ContainerPort>();

        ContainerPort http = new ContainerPort();
        http.setContainerPort(ConfigParameters.CONTAINER_PORT);
        http.setProtocol("TCP");
        http.setName("http");

        ContainerPort jolokia = new ContainerPort();
        jolokia.setContainerPort(8787);
        jolokia.setProtocol("TCP");
        jolokia.setName("jolokia");

        ports.add(http);
        ports.add(jolokia);

        return ports;
    }

    private List<EnvVar> getEnvironmentVariables() {
        List<EnvVar> envVars = new ArrayList<EnvVar>();

        EnvVarSource namespaceSource = new EnvVarSource();
        namespaceSource.setFieldRef(new ObjectFieldSelector(null, "metadata.namespace"));

        EnvVar namespace = new EnvVar();
        namespace.setName("KUBERNETES_NAMESPACE");
        namespace.setValueFrom(namespaceSource);

        EnvVar masterURI = new EnvVar();
        masterURI.setName("SPRING_CONFIG_LOCATION");
        masterURI.setValue("file:///etc/config/application.yml");

        envVars.add(masterURI);
        envVars.add(namespace);
        return envVars;
    }

    private List<Volume> getVolumes(){

        Volume configMap = new Volume();
        configMap.setConfigMap(new ConfigMapVolumeSource(null, ConfigParameters.CONFIGMAP_NAME));
        configMap.setName(ConfigParameters.CONFIGMAP_NAME);

        List<Volume> volList = new ArrayList<>();
        volList.add(configMap);

        return volList;
    }

    private List<VolumeMount> getVolumeMounts(){
        ArrayList<VolumeMount> avm = new ArrayList<>();
        avm.add(new VolumeMount(ConfigParameters.CONFIGMAP_MOUNT,ConfigParameters.CONFIGMAP_NAME,true));
        return avm;
    }


    private Container getContainers() {
        Container container = new Container();
        container.setImage("${IS_PULL_NAMESPACE}/"+ ConfigParameters.APP_NAME +  ":${IS_TAG}");
        container.setImagePullPolicy("Always");
        container.setName(ConfigParameters.APP_NAME);
        container.setPorts(getPorts());
        container.setEnv(getEnvironmentVariables());
        container.setLivenessProbe(getProbe(new Integer(30), new Integer(60)));
        container.setReadinessProbe(getProbe(new Integer(30), new Integer(1)));
        container.setVolumeMounts(getVolumeMounts());
        return container;
    }


    private Map<String, String> getSelectors() {
        Map<String, String> selectors = new HashMap<>();
        selectors.put("project", "jolokia-aggregator-api");
        selectors.put("provider", "org.swinchester.metrics");
        selectors.put("version", "1.0-SNAPSHOT");
        selectors.put("group", "api");
        return selectors;
    }

    private Probe getProbe(Integer initialDelaySeconds, Integer timeoutSeconds) {
        TCPSocketAction jettyAction = new TCPSocketAction();
        jettyAction.setPort(new IntOrString(ConfigParameters.CONTAINER_PORT));

        Probe probe = new Probe();
        probe.setInitialDelaySeconds(initialDelaySeconds);
        probe.setTimeoutSeconds(timeoutSeconds);
        probe.setTcpSocket(jettyAction);

        return probe;
    }


}
