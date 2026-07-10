package com.sawhub.hub.perfil;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class NivelJornadaTest {

    @Test
    void paraXpResolveOPatamarCorretoNasFronteiras() {
        assertThat(NivelJornada.paraXp(0)).isEqualTo(NivelJornada.BRONZE);
        assertThat(NivelJornada.paraXp(1499)).isEqualTo(NivelJornada.BRONZE);
        assertThat(NivelJornada.paraXp(1500)).isEqualTo(NivelJornada.PRATA);
        assertThat(NivelJornada.paraXp(3999)).isEqualTo(NivelJornada.PRATA);
        assertThat(NivelJornada.paraXp(4000)).isEqualTo(NivelJornada.OURO);
        assertThat(NivelJornada.paraXp(7999)).isEqualTo(NivelJornada.OURO);
        assertThat(NivelJornada.paraXp(8000)).isEqualTo(NivelJornada.DIAMANTE);
        assertThat(NivelJornada.paraXp(999_999)).isEqualTo(NivelJornada.DIAMANTE);
    }

    @Test
    void proximoRetornaONivelSeguinteOuNullNoTopo() {
        assertThat(NivelJornada.BRONZE.proximo()).isEqualTo(NivelJornada.PRATA);
        assertThat(NivelJornada.PRATA.proximo()).isEqualTo(NivelJornada.OURO);
        assertThat(NivelJornada.OURO.proximo()).isEqualTo(NivelJornada.DIAMANTE);
        assertThat(NivelJornada.DIAMANTE.proximo()).isNull();
    }
}
