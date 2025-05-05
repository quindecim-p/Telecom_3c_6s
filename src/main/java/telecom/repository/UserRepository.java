package telecom.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import telecom.entity.User;
import telecom.enums.RoleType;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Integer>, JpaSpecificationExecutor<User> {

    Optional<User> findByLogin(String login);

    boolean existsByLogin(String login);

    boolean existsByLoginAndIdNot(String login, int id);

    long countByRole(RoleType role);

    long countByIsBannedTrue();

    @Query("SELECT u.role as roleType, COUNT(u) as count FROM User u GROUP BY u.role")
    List<Map<String, Object>> countUsersByRole();

}