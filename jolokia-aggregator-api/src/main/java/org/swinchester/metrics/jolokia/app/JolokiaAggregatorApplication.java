package org.swinchester.metrics.jolokia.app;

import org.apache.camel.spring.boot.FatJarRouter;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ImportResource;

/**
 * Created by swinchester on 18/08/16.
 */
@SpringBootApplication
@ComponentScan(basePackages = {"org.swinchester.metrics.jolokia"})
@ImportResource({"META-INF/spring/camel-context.xml"})
public class JolokiaAggregatorApplication extends FatJarRouter{
}
