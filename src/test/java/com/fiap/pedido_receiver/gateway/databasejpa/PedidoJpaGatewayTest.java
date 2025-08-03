package com.fiap.pedido_receiver.gateway.databasejpa;

import com.fiap.pedido_receiver.domain.ClienteRequest;
import com.fiap.pedido_receiver.domain.PagamentoStatus;
import com.fiap.pedido_receiver.domain.Pedido;
import com.fiap.pedido_receiver.domain.PedidoJson;
import com.fiap.pedido_receiver.domain.ProdutoRequest;
import com.fiap.pedido_receiver.domain.StatusPedido;
import com.fiap.pedido_receiver.gateway.databasejpa.entity.ItemPedidoEntity;
import com.fiap.pedido_receiver.gateway.databasejpa.entity.PedidoEntity;
import com.fiap.pedido_receiver.gateway.databasejpa.repository.PedidoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.*;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PedidoJpaGatewayTest {

    @Mock
    private PedidoRepository pedidoRepository;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private PedidoJpaGateway pedidoJpaGateway;

    private PedidoJson pedidoJson;
    private PedidoEntity pedidoEntity;
    private final String clienteId = "12345678900";
    private final String pagamentoId = "PAG-123";

    @BeforeEach
    void setUp() {
        pedidoJpaGateway.pagamentoApiUrl = "http://pagamento-api";
        pedidoJpaGateway.estoqueApiUrl = "http://estoque-api";
        pedidoJpaGateway.clienteApiUrl = "http://cliente-api";
        pedidoJpaGateway.produtoApiUrl = "http://produto-api";

        PedidoJson.ItemPedido itemPedido = new PedidoJson.ItemPedido();
        itemPedido.setSku("SKU-123");
        itemPedido.setQuantidade(2);

        PedidoJson.DadosPagamento dadosPagamento = new PedidoJson.DadosPagamento();
        dadosPagamento.setNumeroCartao("1234567812345678");

        pedidoJson = new PedidoJson();
        pedidoJson.setClienteId(clienteId);
        pedidoJson.setItens(List.of(itemPedido));
        pedidoJson.setDadosPagamento(dadosPagamento);

        ItemPedidoEntity itemPedidoEntity = new ItemPedidoEntity();
        itemPedidoEntity.setSku("SKU-123");
        itemPedidoEntity.setQuantidade(2);

        pedidoEntity = new PedidoEntity();
        pedidoEntity.setId(1L);
        pedidoEntity.setClienteId(clienteId);
        pedidoEntity.setItens(List.of(itemPedidoEntity));
        pedidoEntity.setStatus(StatusPedido.ABERTO.name());
        pedidoEntity.setValorTotal(BigDecimal.valueOf(100));
        pedidoEntity.setEndereco("Rua Teste, 123");
        pedidoEntity.setDataCriacao(LocalDateTime.now());
        pedidoEntity.setPagamentoId(pagamentoId);
    }

    @Test
    void realizarPedido_deveCriarPedidoComSucesso() {
        // Arrange
        ClienteRequest clienteRequest = new ClienteRequest(clienteId, null, "Rua Teste, 123");
        ProdutoRequest produtoRequest = new ProdutoRequest(UUID.randomUUID(), "Produto Teste", BigDecimal.valueOf(50));

        when(restTemplate.getForEntity(anyString(), eq(ClienteRequest.class)))
                .thenReturn(ResponseEntity.ok(clienteRequest));
        when(restTemplate.getForEntity(anyString(), eq(ProdutoRequest.class)))
                .thenReturn(ResponseEntity.ok(produtoRequest));
        when(restTemplate.exchange(anyString(), any(HttpMethod.class), any(), eq(String.class)))
                .thenReturn(ResponseEntity.ok("OK"));
        when(restTemplate.postForEntity(anyString(), any(), eq(String.class)))
                .thenReturn(ResponseEntity.ok(pagamentoId));
        when(pedidoRepository.save(any(PedidoEntity.class))).thenReturn(pedidoEntity);

        // Act
        Pedido result = pedidoJpaGateway.realizarPedido(pedidoJson);

        // Assert
        assertNotNull(result);
        assertEquals(BigDecimal.valueOf(100), result.getValor());
        assertEquals("Rua Teste, 123", result.getEndereco());
        assertEquals(1, result.getItens().size());
        assertEquals(StatusPedido.ABERTO.name(), result.getStatusPedido());

        // Verificação corrigida - espera apenas 1 chamada
        verify(pedidoRepository, times(1)).save(any(PedidoEntity.class));
    }

    @Test
    void realizarPedido_deveLancarExcecaoQuandoClienteNaoEncontrado() {
        // Arrange
        when(restTemplate.getForEntity(anyString(), eq(ClienteRequest.class)))
                .thenReturn(ResponseEntity.notFound().build());

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            pedidoJpaGateway.realizarPedido(pedidoJson);
        });
    }

    @Test
    void realizarPedido_deveMarcarComoSemEstoqueQuandoFalharProcessamentoEstoque() {
        // Arrange
        ClienteRequest clienteRequest = new ClienteRequest(clienteId, null, "Rua Teste, 123");
        ProdutoRequest produtoRequest = new ProdutoRequest(UUID.randomUUID(), "Produto Teste", BigDecimal.valueOf(50));

        when(restTemplate.getForEntity(anyString(), eq(ClienteRequest.class)))
                .thenReturn(ResponseEntity.ok(clienteRequest));
        when(restTemplate.getForEntity(anyString(), eq(ProdutoRequest.class)))
                .thenReturn(ResponseEntity.ok(produtoRequest));
        when(restTemplate.exchange(anyString(), any(HttpMethod.class), any(), eq(String.class)))
                .thenThrow(new RestClientException("Erro estoque"));

        // Act
        Pedido result = pedidoJpaGateway.realizarPedido(pedidoJson);

        // Assert
        assertEquals(StatusPedido.FECHADO_SEM_ESTOQUE.name(), result.getStatusPedido());
    }

    @Test
    void realizarPedido_deveMarcarComoSemCreditoQuandoFalharProcessamentoPagamento() {
        // Arrange
        ClienteRequest clienteRequest = new ClienteRequest(clienteId, null, "Rua Teste, 123");
        ProdutoRequest produtoRequest = new ProdutoRequest(UUID.randomUUID(), "Produto Teste", BigDecimal.valueOf(50));

        when(restTemplate.getForEntity(anyString(), eq(ClienteRequest.class)))
                .thenReturn(ResponseEntity.ok(clienteRequest));
        when(restTemplate.getForEntity(anyString(), eq(ProdutoRequest.class)))
                .thenReturn(ResponseEntity.ok(produtoRequest));
        when(restTemplate.exchange(anyString(), any(HttpMethod.class), any(), eq(String.class)))
                .thenReturn(ResponseEntity.ok("OK"));
        when(restTemplate.postForEntity(anyString(), any(), eq(String.class)))
                .thenThrow(new RestClientException("Erro pagamento"));

        // Act
        Pedido result = pedidoJpaGateway.realizarPedido(pedidoJson);

        // Assert
        assertEquals(StatusPedido.FECHADO_SEM_CREDITO.name(), result.getStatusPedido());
    }

    @Test
    void buscaPedido_deveRetornarPedidoComStatusSucessoQuandoPagamentoAprovado() {
        // Arrange
        when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedidoEntity));
        when(restTemplate.getForEntity(anyString(), eq(PagamentoStatus.class)))
                .thenReturn(ResponseEntity.ok(PagamentoStatus.SUCESSO));

        // Act
        Pedido result = pedidoJpaGateway.buscaPedido(1L);

        // Assert
        assertEquals(StatusPedido.FECHADO_COM_SUCESSO.name(), result.getStatusPedido());
        verify(pedidoRepository).save(pedidoEntity);
    }

    @Test
    void buscaPedido_deveRetornarPedidoComStatusFalhaQuandoPagamentoReprovado() {
        // Arrange
        when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedidoEntity));
        when(restTemplate.getForEntity(anyString(), eq(PagamentoStatus.class)))
                .thenReturn(ResponseEntity.ok(PagamentoStatus.FALHA));

        // Act
        Pedido result = pedidoJpaGateway.buscaPedido(1L);

        // Assert
        assertEquals(StatusPedido.FECHADO_SEM_CREDITO.name(), result.getStatusPedido());
        verify(pedidoRepository).save(pedidoEntity);
    }

    @Test
    void buscaPedido_deveRetornarPedidoComStatusFalhaQuandoErroAoConsultarPagamento() {
        // Arrange
        when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedidoEntity));
        when(restTemplate.getForEntity(anyString(), eq(PagamentoStatus.class)))
                .thenThrow(new RestClientException("Erro API"));

        // Act
        Pedido result = pedidoJpaGateway.buscaPedido(1L);

        // Assert
        assertEquals(StatusPedido.FECHADO_SEM_CREDITO.name(), result.getStatusPedido());
        verify(pedidoRepository).save(pedidoEntity);
    }

    @Test
    void buscaPedido_deveLancarExcecaoQuandoPedidoNaoEncontrado() {
        // Arrange
        when(pedidoRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            pedidoJpaGateway.buscaPedido(1L);
        });
    }

    @Test
    void pegaValorTotal_deveCalcularCorretamente() {
        // Arrange
        ProdutoRequest produtoRequest = new ProdutoRequest(UUID.randomUUID(), "Produto Teste", BigDecimal.valueOf(50));
        when(restTemplate.getForEntity(anyString(), eq(ProdutoRequest.class)))
                .thenReturn(ResponseEntity.ok(produtoRequest));

        // Act
        BigDecimal result = pedidoJpaGateway.pegaValorTotal(pedidoJson);

        // Assert
        assertEquals(BigDecimal.valueOf(100), result); // 50 * 2 itens
    }

    @Test
    void pegaValorTotal_deveLancarExcecaoQuandoProdutoNaoEncontrado() {
        // Arrange
        when(restTemplate.getForEntity(anyString(), eq(ProdutoRequest.class)))
                .thenReturn(ResponseEntity.notFound().build());

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            pedidoJpaGateway.pegaValorTotal(pedidoJson);
        });
    }

    @Test
    void buscarCliente_deveRetornarClienteQuandoEncontrado() {
        // Arrange
        ClienteRequest clienteRequest = new ClienteRequest(clienteId, null, "Rua Teste, 123");
        when(restTemplate.getForEntity(anyString(), eq(ClienteRequest.class)))
                .thenReturn(ResponseEntity.ok(clienteRequest));

        // Act
        ClienteRequest result = pedidoJpaGateway.buscarCliente(clienteId);

        // Assert
        assertNotNull(result);
        assertEquals(clienteId, result.getCpf());
    }

    @Test
    void buscarCliente_deveLancarExcecaoQuandoClienteNaoEncontrado() {
        // Arrange
        when(restTemplate.getForEntity(anyString(), eq(ClienteRequest.class)))
                .thenReturn(ResponseEntity.notFound().build());

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            pedidoJpaGateway.buscarCliente(clienteId);
        });
    }

    @Test
    void processarPagamento_deveRetornarPagamentoIdQuandoSucesso() {
        // Arrange
        when(restTemplate.postForEntity(anyString(), any(), eq(String.class)))
                .thenReturn(ResponseEntity.ok(pagamentoId));

        // Act
        String result = pedidoJpaGateway.processarPagamento(pedidoEntity, pedidoJson);

        // Assert
        assertEquals(pagamentoId, result);
    }

}