package com.nullendpoint.jolokia.aggregator.routebuilder

import com.nullendpoint.jolokia.aggregator.http.JsonAggregationStrategy
import groovy.json.JsonOutput
import org.apache.camel.Exchange
import org.apache.camel.Processor
import org.apache.camel.builder.RouteBuilder
import org.apache.camel.model.rest.RestBindingMode
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component(value="apiRouteBuilder")
class APIRouteBuilder extends RouteBuilder {

    @Value('${app.api.contextPath}')
    public String apiContextPath

    @Override
    void configure() throws Exception {

        restConfiguration()
                .contextPath(apiContextPath).apiContextPath("/api-doc")
                .apiProperty("api.title", "Jolokia Aggregating Service")
                .apiProperty("api.version", "1.0")
                .apiProperty("cors", "true")
                .apiContextRouteId("doc-api")
                .component("servlet")
                .bindingMode(RestBindingMode.json)

        rest("/")
                .get("/ping").to("direct:ping")
                .post("/aggregate").consumes("application/json").to("direct:proxyAggregate")

        onException(Exception.class)
                .handled(true)
                .to("log:com.nullendpoint.jolokia?level=ERROR")
                .process(new Processor() {
            @Override
            void process(Exchange exchange) throws Exception {
                def outputMap = [:]
                outputMap.put('exception', exchange.properties['CamelExceptionCaught'])
                exchange.in.setBody(JsonOutput.toJson(outputMap))
                exchange.in.setHeader(Exchange.HTTP_RESPONSE_CODE, 400)
            }
        })

        from('direct:proxyAggregate')
                .to("log:com.nullendpoint.jolokia?showAll=true")
                .enrich('direct:getPodNames')
                .setProperty('originalBody', simple('${body}'))
                .split(header('podNames'), new JsonAggregationStrategy()).parallelProcessing()
                .process("jolokiaRequestProcessor")
                .end()
                .to("log:com.nullendpoint.jolokia?showAll=true")

        from("direct:getPodNames").process("kubeDiscoveryProcessor")

        from("direct:ping").setBody(constant([ping: 'hello']))
    }
}