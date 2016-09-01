package org.swinchester.metrics.jolokia.http

import groovy.json.JsonOutput
import groovyx.net.http.HTTPBuilder
import org.apache.camel.Exchange
import org.apache.camel.Processor
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

/**
 * Created by swinchester on 5/07/16.
 */
@Component(value="jolokiaRequestProcessor")
class JolokiaRequestProcessor implements Processor{

    Logger log = LoggerFactory.getLogger(JolokiaRequestProcessor.class)

    @Value('${openshift.cluster.host}')
    String clusterHost;

    @Override
    void process(Exchange exchange) throws Exception {

        String httpRequestHost = "https://${clusterHost}:8443"
        String namespace = exchange.in.headers.'kube-namespace' ?: "jolokia"
        String token = exchange.in.headers.'Authorization'
        token = token.replace("Bearer ", "")
        def http = new HTTPBuilder(httpRequestHost)
        http.ignoreSSLIssues()
        def broker = exchange.in.body

        def uriPath = "/api/v1/namespaces/${namespace}/pods/https:${broker}:8778/proxy/jolokia/?maxDepth=7&maxCollectionSize=500&ignoreErrors=true&canonicalNaming=false"


        def postBody =[:]
        postBody << exchange.properties['originalBody']

        def mbean = postBody['mbean'] as String
        String newValues = mbean.replaceAll("kube-lookup",broker)
        postBody['mbean'] = newValues


//        def postBody = [type:"read",
//                        mbean: "org.apache.activemq:type=Broker,brokerName=${broker},destinationType=Queue,destinationName=queue.sample",
//                        attribute: ["QueueSize", "ConsumerCount"]]

//                def uriPath = "/api/v1/namespaces/${configMap.namespace}/pods/https:${broker}:8778/proxy/jolokia/" +
//                        "read/org.apache.activemq:type=Broker,brokerName=${broker},destinationType=Queue,destinationName=queue.sample/${queueName}"

        log.debug("http request host :" + httpRequestHost)
        log.debug("http request path :" + uriPath)

        http.request( groovyx.net.http.Method.POST, groovyx.net.http.ContentType.JSON) {

            uri.path = uriPath
            body =  JsonOutput.toJson(postBody)
            headers = [Authorization: "Bearer ${token}",
                       Accept: groovyx.net.http.ContentType.JSON]

            log.debug("Authorization: Bearer ${token}")

            response.success = { resp, json ->
                log.debug("POST response status: ${resp.statusLine}")
                assert resp.statusLine.statusCode == 200
                log.debug( "Got response: ${resp.statusLine}")
                log.debug( "Content-Type: ${resp.headers.'Content-Type'}")
                json << [broker:broker]

                //make it a list here, just so it is easier later...
                exchange.in.body = json
                log.debug("response: ${json}")
            }


            response.'404' = {
                println 'Not found'
                log.error("unable to call jolokia")
            }

            response.'403' = { resp, json ->
                log.error("unauthorized to call jolokia")
                log.debug("POST response status: ${resp.statusLine}")
                log.debug( "Got response: ${resp.statusLine}")
                log.debug( "Content-Type: ${resp.headers.'Content-Type'}")
                json << [broker:broker]
                exchange.in.body = json
                log.debug("response: ${json}")
            }

            response.failure = { resp ->
                log.debug(resp.data)
            }
        }
    }
}
