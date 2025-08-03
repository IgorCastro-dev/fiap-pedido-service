package com.fiap.pedido_receiver.usecase;

import com.fiap.pedido_receiver.domain.Pedido;
import com.fiap.pedido_receiver.gateway.PedidoGateway;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class BuscaPedidoPorIdUsecase {
    @Autowired
    private PedidoGateway pedidoGateway;

    public Pedido buscarPedidoPorId(Long pedidoId){
        return pedidoGateway.buscaPedido(pedidoId);
    }
}
