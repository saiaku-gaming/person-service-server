package com.valhallagame.personserviceserver.config;

import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.valhallagame.common.rabbitmq.RabbitMQRouting;

@Configuration
public class RabbitMQConfig {

	@Bean
	public DirectExchange personExchange() {
		return new DirectExchange(RabbitMQRouting.Exchange.PERSON.name());
	}
	
	@Bean
	public Jackson2JsonMessageConverter jacksonConverter() {
		return new Jackson2JsonMessageConverter();
	}
	
	@Bean
	public SimpleRabbitListenerContainerFactory containerFactory() {
		SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
		factory.setMessageConverter(jacksonConverter());
		return factory;
	}

}
