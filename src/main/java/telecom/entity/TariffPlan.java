package telecom.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import telecom.enums.TariffType;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.io.Serializable;
import java.math.BigDecimal;

@NoArgsConstructor
@Setter
@Getter
@Entity
@Table(name = "tariffplan")
public class TariffPlan implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @NotBlank(message = "Заполните название услуги")
    @Column(length = 50, nullable = false)
    private String name;

    @NotNull(message = "Укажите тип тарифа")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TariffType type;

    @NotNull(message = "Укажите ежемесячную оплату за услугу")
    @Column(name = "monthly_payment", nullable = false)
    private BigDecimal monthlyPayment;

    @NotBlank(message = "Заполните описание услуги")
    @Column(length = 500, nullable = false)
    private String description;

    public TariffPlan(String name, TariffType type, BigDecimal monthlyPayment, String description) {
        this.name = name;
        this.type = type;
        this.monthlyPayment = monthlyPayment;
        this.description = description;
    }

}