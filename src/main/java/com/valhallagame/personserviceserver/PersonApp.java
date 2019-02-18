package com.valhallagame.personserviceserver;

import com.valhallagame.common.DefaultServicePortMappings;
import com.valhallagame.common.Properties;
import com.valhallagame.common.filter.ServiceRequestFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.embedded.EmbeddedServletContainerCustomizer;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

import javax.servlet.Filter;

@EnableScheduling
@SpringBootApplication
public class PersonApp {

	private static final Logger logger = LoggerFactory.getLogger(PersonApp.class);

	public static void main(String[] args) {
		Properties.load(args, logger);
		SpringApplication.run(PersonApp.class, args);
	}

	@Bean
	public EmbeddedServletContainerCustomizer containerCustomizer() {
		return (container -> container.setPort(DefaultServicePortMappings.PERSON_SERVICE_PORT));
	}

	@Bean
	public FilterRegistrationBean serviceRequestFilterRegistration() {
		FilterRegistrationBean registration = new FilterRegistrationBean();
		registration.setFilter(getServiceRequestFilter());
		registration.addUrlPatterns(
				"/*",
				"/**"
		);
		registration.setName("serviceRequestFilter");
		registration.setOrder(1);
		return registration;
	}

	@Bean(name = "serviceRequestFilter")
	public Filter getServiceRequestFilter() {
		return new ServiceRequestFilter();
	}
}
