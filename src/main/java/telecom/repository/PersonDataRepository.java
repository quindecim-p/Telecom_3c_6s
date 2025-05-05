package telecom.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import telecom.entity.PersonData;
import telecom.entity.User;

import java.util.List;
import java.util.Optional;

@Repository
public interface PersonDataRepository extends JpaRepository<PersonData, Integer> {

    Optional<PersonData> findByUser(User user);

    List<PersonData> findByUserIdIn(List<Integer> userIds);

    boolean existsByEmail(String email);

    boolean existsByPhone(String phone);

    boolean existsByEmailAndUserIdNot(String email, int userId);

    boolean existsByPhoneAndUserIdNot(String phone, int userId);

}