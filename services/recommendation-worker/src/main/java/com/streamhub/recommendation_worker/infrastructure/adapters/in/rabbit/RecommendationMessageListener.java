package com.streamhub.recommendation_worker.infrastructure.adapters.in.rabbit;

import com.rabbitmq.client.Channel;
import com.streamhub.recommendation_worker.application.ports.in.RecommendationWorkerUseCase;
import com.streamhub.recommendation_worker.domain.model.PingReceivedEvent;
import com.streamhub.recommendation_worker.domain.model.RatingAddedEvent;
import com.streamhub.recommendation_worker.infrastructure.config.RabbitMqConfig;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

@Component
public class RecommendationMessageListener {

    private final RecommendationWorkerUseCase useCase;

    public RecommendationMessageListener(RecommendationWorkerUseCase useCase) {
        this.useCase = useCase;
    }

    @RabbitListener(queues = RabbitMqConfig.PING_QUEUE)
    public void onPingReceived(PingReceivedEvent event,
                              Channel channel,
                              @Header(AmqpHeaders.DELIVERY_TAG) long tag) throws Exception {
        try {
            useCase.handlePing(event);
            channel.basicAck(tag, false);
        } catch (Exception exception) {
            channel.basicNack(tag, false, true);
        }
    }

    @RabbitListener(queues = RabbitMqConfig.RATING_QUEUE)
    public void onRatingAdded(RatingAddedEvent event,
                             Channel channel,
                             @Header(AmqpHeaders.DELIVERY_TAG) long tag) throws Exception {
        try {
            useCase.handleRating(event);
            channel.basicAck(tag, false);
        } catch (Exception exception) {
            channel.basicNack(tag, false, true);
        }
    }
}
