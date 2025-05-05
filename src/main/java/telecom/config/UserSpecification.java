package telecom.config;

import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;
import telecom.entity.PersonData;
import telecom.entity.User;
import telecom.enums.RoleType;

public class UserSpecification {

    /**
     * Фильтр по роли пользователя.
     */
    public static Specification<User> hasRole(RoleType role) {
        if (role == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("role"), role);
    }

    /**
     * Фильтр по статусу бана.
     */
    public static Specification<User> isBanned(Boolean isBanned) {
        if (isBanned == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("isBanned"), isBanned);
    }

    /**
     * Фильтр по адресу (частичное совпадение, регистронезависимое).
     * Требует JOIN с PersonData.
     */
    public static Specification<User> addressLike(String address) {
        if (!StringUtils.hasText(address)) {
            return null;
        }
        return (root, query, cb) -> {
            Join<User, PersonData> personDataJoin = root.join("personData", JoinType.LEFT);
            return cb.like(cb.lower(personDataJoin.get("address")), "%" + address.toLowerCase() + "%");
        };
    }

    /**
     * Фильтр по ключевому слову (логин, ФИО, email, телефон).
     * Регистронезависимый поиск, требует JOIN с PersonData.
     */
    public static Specification<User> matchesKeyword(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return null;
        }
        String lowerKeyword = "%" + keyword.toLowerCase() + "%";
        return (root, query, cb) -> {
            Join<User, PersonData> personDataJoin = root.join("personData", JoinType.LEFT);

            Predicate loginMatch = cb.like(cb.lower(root.get("login")), lowerKeyword);
            Predicate fullNameMatch = cb.like(cb.lower(personDataJoin.get("fullName")), lowerKeyword);
            Predicate emailMatch = cb.like(cb.lower(personDataJoin.get("email")), lowerKeyword);
            Predicate phoneMatch = cb.like(cb.lower(personDataJoin.get("phone")), lowerKeyword);

            return cb.or(loginMatch, fullNameMatch, emailMatch, phoneMatch);
        };
    }
}