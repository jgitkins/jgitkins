//package io.jgitkins.runner.infrastructure.config;
//
//import io.jgitkins.runner.presentation.consumer.JobExecutionConsumer;
//import org.springframework.amqp.rabbit.connection.ConnectionFactory;
//import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
//import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter;
//import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//
//@Configuration
//public class RabbitConsumerConfig {
//
//    // 메세지 1개씩만 소비
//    @Bean
//    public SimpleMessageListenerContainer simpleMessageListenerContainer(ConnectionFactory connectionFactory,
//            MessageListenerAdapter messageListener, Jackson2JsonMessageConverter jsonMessageConverter) {
//
//        messageListener.setMessageConverter(jsonMessageConverter);
//
//        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
//        container.setConnectionFactory(connectionFactory);
//        container.setQueueNames("jgitkins.task.queue");
//        container.setMessageListener(messageListener);
//        container.setPrefetchCount(1); // TODO: 추후 환경변수처리
//
//        return container;
//    }
//
//    @Bean
//    public MessageListenerAdapter listenerAdapter(JobExecutionConsumer jobExecutionConsumer) {
//        return new MessageListenerAdapter(jobExecutionConsumer, "receiveTask");
//    }
//
//    @Bean
//    public Jackson2JsonMessageConverter jsonMessageConverter() {
//        return new Jackson2JsonMessageConverter();
//    }
//}
