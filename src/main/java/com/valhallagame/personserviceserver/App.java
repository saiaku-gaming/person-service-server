package com.valhallagame.personserviceserver;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.embedded.EmbeddedServletContainerCustomizer;
import org.springframework.context.annotation.Bean;

import com.valhallagame.common.DefaultServicePortMappings;

@SpringBootApplication
public class App {

	private static final Logger logger = LoggerFactory.getLogger(App.class);

	public static void main(String[] args) {
		if (args.length > 0) {
			logger.info("Args passed in: " + Arrays.asList(args).toString());
			// override system properties with local properties

			for (String arg : args) {
				String[] split = arg.split("=");

				if (split.length == 2) {
					System.getProperties().setProperty(split[0], split[1]);
				} else {
					try (InputStream inputStream = new FileInputStream(args[0])) {
						System.getProperties().load(inputStream);
					} catch (IOException e) {
						logger.error("Failed to read input.", e);
					}
				}
			}

		} else {
			logger.info("No args passed to main");
		}

		SpringApplication.run(App.class, args);
	}

	@Bean
	public EmbeddedServletContainerCustomizer containerCustomizer() {
		return (container -> container.setPort(DefaultServicePortMappings.PERSON_SERVICE_PORT));
	}
}
