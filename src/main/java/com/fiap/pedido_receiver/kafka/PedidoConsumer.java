package com.fiap.pedido_receiver.kafka;

import com.fiap.pedido_receiver.domain.PedidoJson;
import com.fiap.pedido_receiver.gateway.PedidoGateway;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.messaging.handler.annotation.Payload;


@Component
public class PedidoConsumer {

    @Autowired
    public PedidoGateway pedidoGateway;

    @KafkaListener(
            topics = "novo-pedido",
            groupId = "pagamento-group"
    )
    public void consumirPedido(@Payload PedidoJson pedido) {
        pedidoGateway.realizarPedido(pedido);
    }
}