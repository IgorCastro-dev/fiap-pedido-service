package com.fiap.pedido_receiver.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum PagamentoStatus {
    PENDENTE,
    SUCESSO,
    FALHA
}