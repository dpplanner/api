//package com.dp.dplanner.rabbitMQ;
//
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
//import org.springframework.amqp.core.Binding;
//import org.springframework.amqp.core.BindingBuilder;
//import org.springframework.amqp.core.DirectExchange;
//import org.springframework.amqp.core.Queue;
//import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
//import org.springframework.amqp.rabbit.connection.ConnectionFactory;
//import org.springframework.amqp.rabbit.core.RabbitTemplate;
//import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
//import org.springframework.amqp.support.converter.MessageConverter;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.context.annotation.Profile;
//
////@Configuration
//@Profile("!test")
//public class RabbitMQConfig {
//
//    @Value("${spring.rabbitmq.host}")
//    private String rabbitmqHost;
//
//    @Value("${spring.rabbitmq.port}")
//    private int rabbitmqPort;
//
//    @Value("${spring.rabbitmq.username}")
//    private String rabbitmqUsername;
//
//    @Value("${spring.rabbitmq.password}")
//    private String rabbitmqPassword;
//
//    @Value("${rabbitmq.exchange.name}")
//    private String exchangeName;
//
//    @Value("${rabbitmq.queue.name.1}")
//    private String queueName1;
//
//    @Value("${rabbitmq.routing.key.1}")
//    private String routingKey1;
//
//    @Value("${rabbitmq.queue.name.2}")
//    private String queueName2;
//
//    @Value("${rabbitmq.routing.key.2}")
//    private String routingKey2;
//
//    @Value("${rabbitmq.queue.name.3}")
//    private String queueName3;
//
//    @Value("${rabbitmq.routing.key.3}")
//    private String routingKey3;
//
//    /**
//     * 지정된 큐 이름으로 Queue 빈을 생성
//     *
//     * @return Queue 빈 객체
//     */
//    @Bean
//    public Queue queue1() {
//        return new Queue(queueName1);
//    }
//
//    @Bean
//    public Queue queue2() {
//        return new Queue(queueName2);
//    }
//
//
//    @Bean
//    public Queue queue3() {
//        return new Queue(queueName3);
//    }
//
//
//    /**
//     * 지정된 익스체인지 이름으로 DirectExchange 빈을 생성
//     *
//     * @return TopicExchange 빈 객체
//     */
//    @Bean
//    public DirectExchange exchange() {
//        return new DirectExchange(exchangeName);
//    }
//
//    /**
//     * 주어진 큐와 익스체인지를 바인딩하고 라우팅 키를 사용하여 Binding 빈을 생성
//     *
//     * @param queue   바인딩할 Queue
//     * @param exchange 바인딩할 TopicExchange
//     * @return Binding 빈 객체
//     */
//    @Bean
//    public Binding binding1(Queue queue1, DirectExchange exchange) {
//        return BindingBuilder.bind(queue1).to(exchange).with(routingKey1);
//    }
//
//    @Bean
//    public Binding binding2(Queue queue2, DirectExchange exchange) {
//        return BindingBuilder.bind(queue2).to(exchange).with(routingKey2);
//    }
//
//    @Bean
//    public Binding binding3(Queue queue3, DirectExchange exchange) {
//        return BindingBuilder.bind(queue3).to(exchange).with(routingKey3);
//    }
//
//    /**
//     * RabbitMQ 연결을 위한 ConnectionFactory 빈을 생성하여 반환
//     *
//     * @return ConnectionFactory 객체
//     */
//    @Bean
//    public ConnectionFactory connectionFactory() {
//        CachingConnectionFactory connectionFactory = new CachingConnectionFactory();
//        connectionFactory.setHost(rabbitmqHost);
//        connectionFactory.setPort(rabbitmqPort);
//        connectionFactory.setUsername(rabbitmqUsername);
//        connectionFactory.setPassword(rabbitmqPassword);
//        return connectionFactory;
//    }
//
//    /**
//     * RabbitTemplate을 생성하여 반환
//     *
//     * @param connectionFactory RabbitMQ와의 연결을 위한 ConnectionFactory 객체
//     * @return RabbitTemplate 객체
//     */
//    @Bean
//    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
//        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
//        // JSON 형식의 메시지를 직렬화하고 역직렬할 수 있도록 설정
//        rabbitTemplate.setMessageConverter(jackson2JsonMessageConverter());
//        return rabbitTemplate;
//    }
//
//    /**
//     * Jackson 라이브러리를 사용하여 메시지를 JSON 형식으로 변환하는 MessageConverter 빈을 생성
//     *
//     * @return MessageConverter 객체
//     */
//    @Bean
//    public MessageConverter jackson2JsonMessageConverter() {
//
//        ObjectMapper objectMapper = new ObjectMapper();
//        objectMapper.registerModule(new JavaTimeModule()); // JavaTimeModule 추가
//        return new Jackson2JsonMessageConverter(objectMapper);
//    }
//
//}
