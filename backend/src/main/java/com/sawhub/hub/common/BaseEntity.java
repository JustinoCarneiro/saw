package com.sawhub.hub.common;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;

@MappedSuperclass
public abstract class BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @CreationTimestamp
    @Column(name = "criado_em", updatable = false)
    private Instant criadoEm;

    // Lock otimista em toda entidade de propósito (achado da revisão de segurança do E14):
    // sem isto, duas requisições concorrentes sobre a mesma linha (ex.: duplo-clique em
    // "Liquidar conta") corrompem silenciosamente o dado — cada uma lê o estado antigo, ambas
    // passam a validação e ambas gravam, gerando efeito duplicado (ex.: dois lançamentos
    // REALIZADO pra uma única liquidação, inflando o DRE). Com @Version, a segunda gravação
    // falha com OptimisticLockException em vez de silenciosamente corromper o dado.
    @Version
    private Long versao;

    public UUID getId() {
        return id;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }

    public Long getVersao() {
        return versao;
    }
}
