package com.Laxaruz.Cajero.Entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.Builder;
import java.time.LocalDateTime;
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Movimiento {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private LocalDateTime fecha;
    @Enumerated(EnumType.STRING) //Se usa la anotación enum porque se trabaja con enums que sería el tipo de mov
    private TipoMovimiento tipo;
    private double monto;
    @ManyToOne
    @JoinColumn(name = "cuenta_id")
    private Cuenta cuenta;

}
