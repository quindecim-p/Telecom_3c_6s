package telecom.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.math.BigDecimal;

@NoArgsConstructor
@Setter
@Getter
@Entity
@Table(name = "billingaccount")
public class BillingAccount implements Serializable {

    @Id
    @Column(name = "subscriber_id")
    private int subscriberId;

    @Column(length = 50, nullable = false, unique = true)
    private String number;

    @Column(nullable = false)
    private BigDecimal balance;

    @OneToOne
    @MapsId
    @JoinColumn(name = "subscriber_id")
    private Subscriber subscriber;

    public BillingAccount(String number, BigDecimal balance, Subscriber subscriber) {
        this.number = number;
        this.balance = balance;
        this.subscriber = subscriber;
    }

}