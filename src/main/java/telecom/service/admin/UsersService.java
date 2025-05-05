package telecom.service.admin;

import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import telecom.dto.UserCreationDTO;
import telecom.dto.UserPersonDataDTO;
import telecom.enums.RoleType;
import telecom.entity.*;
import telecom.repository.*;
import telecom.entity.Subscriber;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class UsersService {

    private final UserRepository userRepository;
    private final PersonDataRepository personDataRepository;
    private final SubscriberRepository subscriberRepository;
    private final BillingAccountRepository billingAccountRepository;
    private final ConnectedServiceRepository connectedServiceRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public UsersService(UserRepository userRepository,
                        PersonDataRepository personDataRepository,
                        SubscriberRepository subscriberRepository,
                        BillingAccountRepository billingAccountRepository,
                        ConnectedServiceRepository connectedServiceRepository,
                        PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.personDataRepository = personDataRepository;
        this.subscriberRepository = subscriberRepository;
        this.billingAccountRepository = billingAccountRepository;
        this.connectedServiceRepository = connectedServiceRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Находит пользователей с деталями PersonData, учитывая поиск и фильтр по роли.
     */
    @Transactional(readOnly = true)
    public Page<UserPersonDataDTO> findUsersWithDetails(String keyword, RoleType role, Pageable pageable) {

        Specification<User> spec = (root, query, criteriaBuilder) -> {

            List<Predicate> mainPredicates = new ArrayList<>();

            if (role != null) {
                mainPredicates.add(criteriaBuilder.equal(root.get("role"), role));
            }

            if (StringUtils.hasText(keyword)) {
                String lowerKeyword = "%" + keyword.toLowerCase() + "%";

                Subquery<Integer> subquery;
                if (query != null) {
                    subquery = query.subquery(Integer.class);
                } else {
                    throw new IllegalStateException("CriteriaQuery не инициализирован");
                }

                Root<PersonData> personDataRoot = subquery.from(PersonData.class);
                Predicate personDataPredicate = criteriaBuilder.or(
                        criteriaBuilder.like(criteriaBuilder.lower(personDataRoot.get("fullName")), lowerKeyword),
                        criteriaBuilder.like(criteriaBuilder.lower(personDataRoot.get("email")), lowerKeyword)
                );
                subquery.select(personDataRoot.get("user").get("id"))
                        .where(personDataPredicate);

                Predicate combinedKeywordPredicate = criteriaBuilder.or(
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("login")), lowerKeyword),
                        root.get("id").in(subquery)
                );
                mainPredicates.add(combinedKeywordPredicate);
            }

            return criteriaBuilder.and(mainPredicates.toArray(new Predicate[0]));
        };

        Page<User> userPage = userRepository.findAll(spec, pageable);

        List<Integer> userIds = userPage.getContent().stream().map(User::getId).collect(Collectors.toList());
        Map<Integer, PersonData> personDataMap = Map.of();
        if (!userIds.isEmpty()) {
            personDataMap = personDataRepository.findAllById(userIds).stream()
                    .collect(Collectors.toMap(PersonData::getUserId, Function.identity()));
        }
        final Map<Integer, PersonData> finalPersonDataMap = personDataMap;
        return userPage.map(user -> new UserPersonDataDTO(user, finalPersonDataMap.get(user.getId())));
    }

    /**
     * Получает логин текущего аутентифицированного пользователя.
     */
    private String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof org.springframework.security.core.userdetails.UserDetails) {
            return ((org.springframework.security.core.userdetails.UserDetails) principal).getUsername();
        } else if (principal instanceof String) {
            return (String) principal;
        }
        return null;
    }

    /**
     * Переключает статус блокировки пользователя.
     */
    @Transactional
    public User toggleBanStatus(int userId) {
        User userToModify = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Пользователь с ID " + userId + " не найден."));

        String currentUsername = getCurrentUsername();
        if (currentUsername == null) {
            throw new IllegalStateException("Не удалось определить текущего пользователя для проверки самобана.");
        }
        if (userToModify.getLogin().equals(currentUsername)) {
            throw new IllegalArgumentException("Администратор не может изменить статус блокировки для своей учетной записи.");
        }

        userToModify.setBanned(!userToModify.isBanned());
        return userRepository.save(userToModify);
    }

    /**
     * Удаляет пользователя и все связанные с ним данные.
     */
    @Transactional
    public void deleteUserAndRelatedData(int userId) {
        User userToDelete = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Пользователь с ID " + userId + " не найден для удаления."));

        String currentUsername = getCurrentUsername();
        if (currentUsername == null) {
            throw new IllegalStateException("Не удалось определить текущего пользователя для проверки самоудаления.");
        }
        if (userToDelete.getLogin().equals(currentUsername)) {
            throw new IllegalArgumentException("Администратор не может удалить свою учетную запись.");
        }

        try {
            Optional<Subscriber> subscriberOpt = subscriberRepository.findByUser(userToDelete);
            if (subscriberOpt.isPresent()) {
                Subscriber subscriber = subscriberOpt.get();
                int subscriberId = subscriber.getId();

                billingAccountRepository.findById(subscriberId).ifPresent(billingAccountRepository::delete);

                List<ConnectedService> services = connectedServiceRepository.findBySubscriber(subscriber);
                if (!services.isEmpty()) {
                    connectedServiceRepository.deleteAllInBatch(services);
                }

                subscriberRepository.delete(subscriber);
            }

            personDataRepository.findById(userId).ifPresent(personDataRepository::delete);

            userRepository.delete(userToDelete);

        } catch (Exception e) {
            throw new RuntimeException("Ошибка при удалении пользователя ID " + userId + " и его связанных данных: " + e.getMessage(), e);
        }
    }

    /**
     * Обновляет роль пользователя.
     */
    @Transactional
    public User updateUserRole(int userId, RoleType newRole) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Пользователь с ID " + userId + " не найден."));
        user.setRole(newRole);
        return userRepository.save(user);
    }

    /**
     * Создает нового пользователя.
     */
    @Transactional
    public User createUser(UserCreationDTO form) {
        if (userRepository.existsByLogin(form.getLogin())) {
            throw new DataIntegrityViolationException("Пользователь с таким логином уже существует!");
        }
        if (personDataRepository.existsByEmail(form.getEmail())) {
            throw new DataIntegrityViolationException("Пользователь с таким email уже существует!");
        }
        if (personDataRepository.existsByPhone(form.getPhone())) {
            throw new DataIntegrityViolationException("Пользователь с таким телефоном уже существует!");
        }

        User user = new User();
        user.setLogin(form.getLogin());
        user.setPassword(passwordEncoder.encode(form.getPassword()));
        user.setRole(form.getRole());
        user.setBanned(false);
        User savedUser = userRepository.save(user);

        PersonData personData = new PersonData();
        personData.setUser(savedUser);
        personData.setFullName(form.getFullName());
        personData.setPhone(form.getPhone());
        personData.setEmail(form.getEmail());
        personData.setAddress(form.getAddress());
        personData.setBirthDate(form.getBirthDate());
        personDataRepository.save(personData);

        if (savedUser.getRole() == RoleType.SUBSCRIBER) {

            Subscriber subscriber = new Subscriber();
            subscriber.setUser(savedUser);
            Subscriber savedSubscriber = subscriberRepository.save(subscriber);

            BillingAccount billingAccount = new BillingAccount();
            billingAccount.setSubscriber(savedSubscriber);
            billingAccount.setNumber(generateBillingAccountNumber(savedSubscriber.getId()));
            billingAccount.setBalance(BigDecimal.ZERO);
            billingAccountRepository.save(billingAccount);

        }

        return savedUser;
    }

    /**
     * Вспомогательный метод для генерации номера счета.
     */
    private String generateBillingAccountNumber(int subscriberId) {
        // Пример: 10 цифр, последние 6 - ID абонента с ведущими нулями
        return String.format("1000%06d", subscriberId);
    }
}