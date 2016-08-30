package org.swinchester.metrics.jolokia.routebuilder

import groovy.json.JsonOutput
import org.apache.camel.Exchange
import org.apache.camel.Processor
import org.apache.camel.builder.RouteBuilder
import org.apache.camel.model.rest.RestBindingMode
import org.springframework.stereotype.Component
import org.swinchester.metrics.jolokia.http.JsonAggregationStrategy

/**
 * Created by swinchester on 5/07/16.
 */
@Component(value="apiRouteBuilder")
class APIRouteBuilder extends RouteBuilder {

    @Override
    void configure() throws Exception {

        restConfiguration().component("restlet").host("localhost").port(8080).bindingMode(RestBindingMode.json);

        rest("/jolokia")
                .get("/ping").to("direct:ping")
                .post("/aggregate").consumes("application/json").to("direct:proxyAggregate")

        onException(Exception.class)
                .handled(true)
                .to("log:exceptions?level=ERROR")
                .process(new Processor() {
            @Override
            void process(Exchange exchange) throws Exception {
                def outputMap = [:]
                outputMap.put('exception',exchange.properties['CamelExceptionCaught'])
                exchange.in.setBody(JsonOutput.toJson(outputMap))
                exchange.in.setHeader(Exchange.HTTP_RESPONSE_CODE, 400)
            }
        })

        from('direct:proxyAggregate')
            .enrich('direct:getPodNames')
            .setProperty('originalBody', simple('${body}'))
            .split(header('podNames'), new JsonAggregationStrategy()).parallelProcessing()
                .processRef("jolokiaRequestProcessor")
            .end()
            .to("log:stuff?showAll=true")

        from("direct:getPodNames")
            .processRef("kubeDiscoveryProcessor")


        from("direct:ping").setBody(constant([ping:'hello']))


    }
}


