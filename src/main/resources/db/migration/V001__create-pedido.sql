CREATE TABLE pedido (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    cliente_id VARCHAR(255),
    data_criacao DATETIME,
    valor_total DECIMAL(19,2),
    status VARCHAR(255),
    pagamento_id VARCHAR(255),
    endereco VARCHAR(255)
);

CREATE TABLE itens_pedido (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    sku VARCHAR(255),
    quantidade INT,
    pedido_id BIGINT,
    CONSTRAINT fk_pedido FOREIGN KEY (pedido_id) REFERENCES pedido(id)
);

