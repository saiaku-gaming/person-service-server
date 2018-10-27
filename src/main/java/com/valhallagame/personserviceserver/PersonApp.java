package com.valhallagame.personserviceserver;

import com.valhallagame.common.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.embedded.EmbeddedServletContainerCustomizer;
import org.springframework.context.annotation.Bean;

import com.valhallagame.common.DefaultServicePortMappings;

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
}
