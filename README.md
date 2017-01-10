# jolokia aggregator

Used for aggregating amq broker statistics

Requires the following HTTP headers:

- kube-label
- Authorization
- kube-namespace

Requires a standard Jolokia POST request:

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

Exposes a RESTful API which will

1.  Query the kubernetes api for pods in the kube-namespace which match the supplied kube-label
2.  For each pod name returned, the response will be aggregated into a big json.

```
curl -X POST -H "Authorization: Bearer CAEQO0xb6NsyoeURnH5-86UGDdcdzo4AaS_30JZjtG0" -H "Accept: application/json" -H "kube-namespace: origin-metrics" -H "kube-label: application=broker" -H "Content-Type: application/json" -H "Cache-Control: no-cache" -H "Postman-Token: 781f9db0-e6a1-83eb-a41e-539009050657" -d '{"type":"read","mbean":"org.apache.activemq:type=Broker,brokerName=kube-lookup,destinationType=*,destinationName=*", "attribute": ["QueueSize","ConsumerCount"]}' "http://jolokia-aggregator-svc-origin-metrics.rhel-cdk.10.1.2.2.xip.io/jolokia/aggregate"
```
