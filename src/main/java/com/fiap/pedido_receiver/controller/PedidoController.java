package com.fiap.pedido_receiver.controller;

import com.fiap.pedido_receiver.domain.Pedido;
import com.fiap.pedido_receiver.usecase.BuscaPedidoPorIdUsecase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/pedido")
@Tag(name = "Busca pedido por id", description = "Um endpoint de busca de pedido")
public class PedidoController {

    @Autowired
    public BuscaPedidoPorIdUsecase buscaPedidoPorIdUsecase;

    @Operation(description = "Buscar pedido por id")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Pedido encontrado"),
            @ApiResponse(responseCode = "400", description = "Não existe pedido com esse id"),
            @ApiResponse(responseCode = "500", description = "Erro no servidor ao buscar o pedido")
    })
    @GetMapping("/{pedido_id}")
    public ResponseEntity<Pedido> buscarPorId(@PathVariable Long pedido_id){
        return ResponseEntity.status(HttpStatus.OK).body(buscaPedidoPorIdUsecase.buscarPedidoPorId(pedido_id));
    }
}
