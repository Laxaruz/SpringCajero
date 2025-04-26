package Entity;
import lombok.*;
import jakarta.persistence.*;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class Cliente {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    private String nombre;
    private  String identificacion;
    private String pin;
    private boolean bloqueado;
    private int intentosFallidos;
    @OneToMany(mappedBy= "Cliente",cascade = CascadeType.ALL)
    private List<Cuenta> cuentas;

    //Métodos que necesitabas para intentos
    public int getIntentos(){
        return this.intentosFallidos;
    }
    public void setIntentos(int intentos){
        this.intentosFallidos = intentos;
    }
    public void incrementarIntento()
    {
        this.intentosFallidos++;
    }
    public void reiniciarIntentos(){
        this.intentosFallidos = 0;
    }

}
