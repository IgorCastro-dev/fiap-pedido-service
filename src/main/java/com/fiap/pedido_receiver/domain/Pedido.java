package com.fiap.pedido_receiver.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Pedido {
    private BigDecimal valor;
    private String endereco;
    private List<PedidoJson.ItemPedido> itens;
    private String statusPedido;
}
