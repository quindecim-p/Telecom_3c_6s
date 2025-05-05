package telecom.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import telecom.entity.BillingAccount;
import telecom.entity.Subscriber;

import java.util.List;
import java.util.Optional;

@Repository
public interface BillingAccountRepository extends JpaRepository<BillingAccount, Integer> {

    Optional<BillingAccount> findBySubscriber(Subscriber subscriber);

    List<BillingAccount> findBySubscriberIdIn(List<Integer> subscriberIds);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT ba FROM BillingAccount ba WHERE ba.subscriberId = :subscriberId")
    Optional<BillingAccount> findByIdWithPessimisticLock(@Param("subscriberId") int subscriberId);

}