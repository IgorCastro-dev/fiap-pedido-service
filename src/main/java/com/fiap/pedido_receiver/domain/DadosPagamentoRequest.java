package com.fiap.pedido_receiver.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class DadosPagamentoRequest {
    private BigDecimal valor;
    private String clienteCpf;
    private String clienteCartao;
}