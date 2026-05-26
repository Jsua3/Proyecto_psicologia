package com.psychosim.domain.caso;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "preguntas")
@Getter @Setter @NoArgsConstructor
public class Pregunta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "escenario_id", nullable = false)
    private Escenario escenario;

    @Column(nullable = false)
    private Integer orden;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String enunciado;

    @Column(name = "puntos_correcta", nullable = false)
    private Integer puntosCorrecta = 10;

    @OneToMany(mappedBy = "pregunta", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Opcion> opciones = new ArrayList<>();
}
