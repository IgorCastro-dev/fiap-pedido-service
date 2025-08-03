package com.fiap.pedido_receiver.gateway;

import com.fiap.pedido_receiver.domain.Pedido;
import com.fiap.pedido_receiver.domain.PedidoJson;



public interface PedidoGateway {

    Pedido realizarPedido(PedidoJson pedidoJson);
    Pedido buscaPedido(Long pedidoId);

}
