package com.fiap.pedido_receiver.gateway.databasejpa.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "itens_pedido")
@Getter
@Setter
@NoArgsConstructor
public class ItemPedidoEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String sku;
    private Integer quantidade;

    @ManyToOne
    @JoinColumn(name = "pedido_id")
    private PedidoEntity pedido;
}
