# Jolokia Aggregator

Why?  Sometimes you may have a network of brokers in Openshift, accessing the statistics of each broker is difficult.  
The Jolokia Aggregator provides a single endpoint where you are able to access a list of the jolokia statistics.

1.  Query the kubernetes api for pods in the kube-namespace which match the supplied kube-label
2.  For each pod name returned, the response will be aggregated into a big json.

![Aggregator Use Case](./puml/jolokia-aggregator-usecase.png)

## How?

Requires the following HTTP headers:

- kube-label
- Authorization (Bearer `$(oc whoami -t)`)
- kube-namespace

Requires a standard Jolokia POST request:

```
{
    "attribute": [
        "QueueSize",
        "ConsumerCount"
    ],
    "mbean": "org.apache.activemq:type=Broker,brokerName=broker-1-abcd,destinationType=*,destinationName=*",
    "type": "read"
}
```

In the request to the Jolokia Aggregator we substitute the broker name, for example, `broker-1-abcd`, for `kube-lookup`.

```
{
    "attribute": [
        "QueueSize",
        "ConsumerCount"
    ],
    "mbean": "org.apache.activemq:type=Broker,brokerName=kube-lookup,destinationType=*,destinationName=*",
    "type": "read"
}
```
The broker name will be replaced by the pod names that match the `kube-label` labels supplied in the HTTP headers.
And a request will be sent to each broker.
 
The service account this pod is running under will most likely be `default` and will require `view` permissions in order
to see the pods running in the namespaces specified by callers in the http headers. To give permission in a project type:

`oc policy add-role-to-user view -z default`

## Install

`mvn clean fabric8:deploy`

Remember the `application.properties`

- app.api.contextPath=/jolokia
- openshift.cluster.host=192.168.42.48

## Example


Request:
```
curl -X POST \
  http://jolokia-aggregator-myproject.192.168.42.48.nip.io/jolokia/aggregate \
  -H 'Accept: application/json' \
  -H 'Authorization: Bearer p7MAxL2gRQD8wRwWti9dq97eV8UNhWG2xWGcKwG7GyI' \
  -H 'Cache-Control: no-cache' \
  -H 'Content-Type: application/json' \
  -H 'kube-label: application=broker' \
  -H 'kube-namespace: myproject' \
  -d '{"type":"read","mbean":"org.apache.activemq:type=Broker,brokerName=kube-lookup,destinationType=*,destinationName=*", "attribute": ["QueueSize","ConsumerCount"]}'
  
  [{"broker":"broker-amq-1-vqs44","request":{"attribute":["QueueSize","ConsumerCount"],"mbean":"org.apache.activemq:brokerName=broker-amq-1-vqs44,destinationName=*,destinationType=*,type=Broker","type":"read"},"status":200,"timestamp":1521039353,"value":{"org.apache.activemq:brokerName=broker-amq-1-vqs44,destinationName=ActiveMQ.Advisory.MasterBroker,destinationType=Topic,type=Broker":{"ConsumerCount":0,"QueueSize":0}}},{"broker":"broker-amq-1-zcvdc","request":{"attribute":["QueueSize","ConsumerCount"],"mbean":"org.apache.activemq:brokerName=broker-amq-1-zcvdc,destinationName=*,destinationType=*,type=Broker","type":"read"},"status":200,"timestamp":1521039353,"value":{"org.apache.activemq:brokerName=broker-amq-1-zcvdc,destinationName=ActiveMQ.Advisory.MasterBroker,destinationType=Topic,type=Broker":{"ConsumerCount":0,"QueueSize":0}}}]
```

Response:
```$json
[
    {
        "broker": "broker-amq-1-vqs44",
        "request": {
            "attribute": [
                "QueueSize",
                "ConsumerCount"
            ],
            "mbean": "org.apache.activemq:brokerName=broker-amq-1-vqs44,destinationName=*,destinationType=*,type=Broker",
            "type": "read"
        },
        "status": 200,
        "timestamp": 1521039353,
        "value": {
            "org.apache.activemq:brokerName=broker-amq-1-vqs44,destinationName=ActiveMQ.Advisory.MasterBroker,destinationType=Topic,type=Broker": {
                "ConsumerCount": 0,
                "QueueSize": 0
            }
        }
    },
    {
        "broker": "broker-amq-1-zcvdc",
        "request": {
            "attribute": [
                "QueueSize",
                "ConsumerCount"
            ],
            "mbean": "org.apache.activemq:brokerName=broker-amq-1-zcvdc,destinationName=*,destinationType=*,type=Broker",
            "type": "read"
        },
        "status": 200,
        "timestamp": 1521039353,
        "value": {
            "org.apache.activemq:brokerName=broker-amq-1-zcvdc,destinationName=ActiveMQ.Advisory.MasterBroker,destinationType=Topic,type=Broker": {
                "ConsumerCount": 0,
                "QueueSize": 0
            }
        }
    }
]
```

## References

Please see [Jolokia JVM Monitoring in OpenShift](https://developers.redhat.com/blog/2016/03/30/jolokia-jvm-monitoring-in-openshift/).