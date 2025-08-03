package com.fiap.pedido_receiver.gateway.databasejpa.repository;

import com.fiap.pedido_receiver.gateway.databasejpa.entity.PedidoEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PedidoRepository extends JpaRepository<PedidoEntity,Long> {

}
