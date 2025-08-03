package com.fiap.pedido_receiver.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PedidoJson {
    private String clienteId;
    private List<ItemPedido> itens;
    private DadosPagamento dadosPagamento;
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ItemPedido {
        private String sku;
        private Integer quantidade;
    }
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DadosPagamento {
        private String numeroCartao;
    }
}
