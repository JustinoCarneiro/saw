package com.sawhub.hub.mentorado;

public enum Plano {
    GRATUITO,
    BASICO,
    ESSENCIAL,
    PROFISSIONAL;

    // Único ponto do backend que compara Plano por ordinal() (achado de revisão de segurança,
    // M08 e depois M11) — correto porque a ordem declarada acima já é a hierarquia de negócio,
    // mas reordenar este enum muda silenciosamente quem vê o quê. Centralizado aqui de propósito
    // depois do M11 introduzir um segundo cálculo independente do mesmo critério.
    public boolean atendePlanoMinimo(Plano minimo) {
        return minimo.ordinal() <= this.ordinal();
    }
}
