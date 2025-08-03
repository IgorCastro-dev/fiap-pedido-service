package com.fiap.pedido_receiver.gateway.databasejpa;


import com.fiap.pedido_receiver.domain.ClienteRequest;
import com.fiap.pedido_receiver.domain.DadosPagamentoRequest;
import com.fiap.pedido_receiver.domain.EstoqueRequest;
import com.fiap.pedido_receiver.domain.PagamentoStatus;
import com.fiap.pedido_receiver.domain.Pedido;
import com.fiap.pedido_receiver.domain.PedidoJson;

import com.fiap.pedido_receiver.domain.ProdutoRequest;
import com.fiap.pedido_receiver.domain.StatusPedido;
import com.fiap.pedido_receiver.gateway.PedidoGateway;
import com.fiap.pedido_receiver.gateway.databasejpa.entity.ItemPedidoEntity;
import com.fiap.pedido_receiver.gateway.databasejpa.entity.PedidoEntity;

import com.fiap.pedido_receiver.gateway.databasejpa.repository.PedidoRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


@Component
public class PedidoJpaGateway implements PedidoGateway {

    @Value("${api.pagamento.url}")
    String pagamentoApiUrl;

    @Value("${api.estoque.url}")
    String estoqueApiUrl;

    @Value("${api.cliente.url}")
    String clienteApiUrl;

    @Value("${api.produto.url}")
    String produtoApiUrl;

    @Autowired
    private RestTemplate restTemplate;
    @Autowired
    private PedidoRepository pedidoRepository;

    @Transactional
    public Pedido realizarPedido(PedidoJson pedidoJson) {
        buscarCliente(pedidoJson.getClienteId());
        PedidoEntity pedido = criarPedido(pedidoJson);

        pedido.setValorTotal(pegaValorTotal(pedidoJson));

        processarEstoque(pedido);
        pedido.setPagamentoId(processarPagamento(pedido, pedidoJson));
        pedidoRepository.save(pedido);
        return mapToDto(pedido);
    }

    @Transactional
    public Pedido buscaPedido(Long pedidoId) {
        PedidoEntity pedidoEntity = pedidoRepository.findById(pedidoId)
                .orElseThrow(() -> new RuntimeException("Pedido não encontrado"));

        try {
            ResponseEntity<PagamentoStatus> response = restTemplate.getForEntity(
                    pagamentoApiUrl + "/pagamento/status/" + pedidoEntity.getPagamentoId(),
                    PagamentoStatus.class
            );

            PagamentoStatus statusPagamento = response.getBody();

            if (statusPagamento == PagamentoStatus.SUCESSO) {
                pedidoEntity.setStatus(String.valueOf(StatusPedido.FECHADO_COM_SUCESSO));
                pedidoRepository.save(pedidoEntity);
            } else if (statusPagamento == PagamentoStatus.FALHA) {
                pedidoEntity.setStatus(String.valueOf(StatusPedido.FECHADO_SEM_CREDITO));
                pedidoRepository.save(pedidoEntity);
            }


        } catch (RestClientException e) {
            pedidoEntity.setStatus(String.valueOf(StatusPedido.FECHADO_SEM_CREDITO));
            pedidoRepository.save(pedidoEntity);
        }

        return mapToDto(pedidoEntity);
    }



    private Pedido mapToDto(PedidoEntity pedido) {
        return Pedido.builder()
                .itens(toDtoItemList(pedido.getItens()))
                .valor(pedido.getValorTotal())
                .endereco(pedido.getEndereco())
                .statusPedido(pedido.getStatus())
                .build();
    }

    String processarPagamento(PedidoEntity pedido, PedidoJson pedidoJson) {
        try{
            if(pedido.getStatus().equals(StatusPedido.ABERTO.name())){
                DadosPagamentoRequest pagamentoRequest = new DadosPagamentoRequest(
                        pedido.getValorTotal(),
                        pedido.getClienteId(),
                        pedidoJson.getDadosPagamento().getNumeroCartao()
                );

                ResponseEntity<String> response = restTemplate.postForEntity(
                        pagamentoApiUrl + "/pagamento",
                        pagamentoRequest,
                        String.class
                );
                return response.getBody();

            }
        } catch (RestClientException e) {
            pedido.setStatus(String.valueOf(StatusPedido.FECHADO_SEM_CREDITO));
            pedidoRepository.save(pedido);
        }
        return pedido.getPagamentoId();
    }



    void processarEstoque(PedidoEntity pedido) {
        List<EstoqueRequest> requests = new ArrayList<>();

        for (ItemPedidoEntity item : pedido.getItens()) {
            EstoqueRequest estoqueRequest = new EstoqueRequest(
                    item.getSku(),
                    item.getQuantidade()
            );
            requests.add(estoqueRequest);
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<List<EstoqueRequest>> requestEntity = new HttpEntity<>(requests, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    estoqueApiUrl + "estoque",
                    HttpMethod.DELETE,
                    requestEntity,
                    String.class
            );


            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException("Falha ao processar estoque: " + response.getBody());
            }

        } catch (RestClientException e) {
            pedido.setStatus(String.valueOf(StatusPedido.FECHADO_SEM_ESTOQUE));
            pedidoRepository.save(pedido);
        }
    }


    ClienteRequest buscarCliente(String clienteId) {
        try {
            ResponseEntity<ClienteRequest> response = restTemplate.getForEntity(
                    clienteApiUrl + "/cliente/" + clienteId,
                    ClienteRequest.class
            );

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                throw new RuntimeException("Cliente não encontrado ou inválido");
            }
            return response.getBody();
        } catch (RestClientException e) {
            throw new RuntimeException("Erro ao validar cliente: " + e.getMessage());
        }
    }



    BigDecimal pegaValorTotal(PedidoJson pedidoJson) {
        BigDecimal valorTotal = BigDecimal.ZERO;

        for (PedidoJson.ItemPedido item : pedidoJson.getItens()) {
            try {
                ResponseEntity<ProdutoRequest> response = restTemplate.getForEntity(
                        produtoApiUrl + item.getSku(),
                        ProdutoRequest.class
                );

                if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                    throw new RuntimeException("Produto com SKU " + item.getSku() + " não encontrado");
                }
                BigDecimal valorItem = response.getBody().getPreco()
                        .multiply(BigDecimal.valueOf(item.getQuantidade()));

                valorTotal = valorTotal.add(valorItem);

            } catch (RestClientException e) {
                throw new RuntimeException("Erro ao buscar produto SKU " + item.getSku() + ": " + e.getMessage());
            }
        }

        return valorTotal;
    }

    private PedidoEntity criarPedido(PedidoJson pedidoJson) {
        PedidoEntity pedidoEntity = new PedidoEntity();
        pedidoEntity.setEndereco(buscarCliente(pedidoJson.getClienteId()).getEndereco());
        pedidoEntity.setClienteId(pedidoJson.getClienteId());
        pedidoEntity.setItens(toEntityList(pedidoJson.getItens(), pedidoEntity));
        pedidoEntity.setStatus(String.valueOf(StatusPedido.ABERTO));
        pedidoEntity.setDataCriacao(LocalDateTime.now());
        return pedidoEntity;
    }


    private List<ItemPedidoEntity> toEntityList(List<PedidoJson.ItemPedido> itens, PedidoEntity pedidoEntity) {
        return itens.stream()
                .map(item -> toEntity(item, pedidoEntity))
                .collect(Collectors.toList());
    }

    private ItemPedidoEntity toEntity(PedidoJson.ItemPedido item, PedidoEntity pedidoEntity) {
        ItemPedidoEntity entity = new ItemPedidoEntity();
        entity.setSku(item.getSku());
        entity.setQuantidade(item.getQuantidade());
        entity.setPedido(pedidoEntity);
        return entity;
    }

    private List<PedidoJson.ItemPedido> toDtoItemList(List<ItemPedidoEntity> itens) {
        return itens.stream()
                .map(item -> mapToDto(item))
                .collect(Collectors.toList());
    }

    private PedidoJson.ItemPedido mapToDto(ItemPedidoEntity itemPedidoEntity){
        return PedidoJson.ItemPedido.builder()
                .sku(itemPedidoEntity.getSku())
                .quantidade(itemPedidoEntity.getQuantidade())
                .build();
    }

}
