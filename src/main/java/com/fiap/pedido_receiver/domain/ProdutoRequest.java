package com.fiap.pedido_receiver.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.UUID;

@AllArgsConstructor
@Getter
@Builder
public class ProdutoRequest {
    private UUID sku;
    private String nome;
    private BigDecimal preco;
}
