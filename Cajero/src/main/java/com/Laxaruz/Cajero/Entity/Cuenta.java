package com.Laxaruz.Cajero.Entity;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class Cuenta {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(unique = true)
    private String numeroCuenta;
    private double saldo;
    @Enumerated(EnumType.STRING)
    private TipoCuenta tipo;
    @ManyToOne
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;
    private List<Movimiento> movimientos;

}
