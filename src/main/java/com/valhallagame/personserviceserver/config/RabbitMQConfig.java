package com.valhallagame.personserviceserver.config;

import org.springframework.amqp.core.DirectExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.valhallagame.common.rabbitmq.RabbitMQRouting;

@Configuration
public class RabbitMQConfig {

	@Bean
	public DirectExchange personExchange() {
		return new DirectExchange(RabbitMQRouting.Exchange.PERSON.name());
	}

}
