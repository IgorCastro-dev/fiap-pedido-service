package com.fiap.pedido_receiver.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@AllArgsConstructor
@Getter
@Builder
public class ClienteRequest {
    private String cpf;
    private LocalDate dataNascimento;
    private String endereco;
}
