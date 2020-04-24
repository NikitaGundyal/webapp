package com.csye6225.beanconfiguration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.timgroup.statsd.NoOpStatsDClient;
import com.timgroup.statsd.NonBlockingStatsDClient;
import com.timgroup.statsd.StatsDClient;

@Configuration
public class StatsDClientBeanConfiguration {
	
	   @Value("${publish.metrics}")
	    private boolean publishMetrics;

	    @Value("${metrics.server.hostname}")
	    private String metricsServerHost;

	    @Value("${metrics.server.port}")
	    private int metricsServerPort;

	    @Bean
	    public StatsDClient statsDClient() {

	        if (publishMetrics) {
	            return new NonBlockingStatsDClient("csye6225-spring2020", metricsServerHost, metricsServerPort);
	        }

	        return new NoOpStatsDClient();
	    }

}
