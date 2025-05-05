package telecom.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import telecom.entity.Subscriber;
import telecom.entity.User;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubscriberRepository extends JpaRepository<Subscriber, Integer> {

    Optional<Subscriber> findByUser(User user);

    List<Subscriber> findByUserIn(List<User> users);

}