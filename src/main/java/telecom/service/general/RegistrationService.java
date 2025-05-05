package telecom.service.general;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import telecom.dto.RegistrationFormDTO;
import telecom.entity.BillingAccount;
import telecom.entity.PersonData;
import telecom.entity.Subscriber;
import telecom.entity.User;
import telecom.enums.RoleType;
import telecom.repository.BillingAccountRepository;
import telecom.repository.PersonDataRepository;
import telecom.repository.SubscriberRepository;
import telecom.repository.UserRepository;

import java.math.BigDecimal;

@Service
public class RegistrationService {

    private final UserRepository userRepository;
    private final PersonDataRepository personDataRepository;
    private final SubscriberRepository subscriberRepository;
    private final BillingAccountRepository billingAccountRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public RegistrationService(UserRepository userRepository,
                               PersonDataRepository personDataRepository,
                               SubscriberRepository subscriberRepository,
                               BillingAccountRepository billingAccountRepository,
                               PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.personDataRepository = personDataRepository;
        this.subscriberRepository = subscriberRepository;
        this.billingAccountRepository = billingAccountRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Создает нового абонента.
     */
    @Transactional
    public void registerUser(RegistrationFormDTO form) {
        if (userRepository.existsByLogin(form.getLogin())) {
            throw new RuntimeException("Пользователь с таким логином уже существует!");
        }
        if (personDataRepository.existsByEmail(form.getEmail())) {
            throw new RuntimeException("Пользователь с таким email уже существует!");
        }
        if (personDataRepository.existsByPhone(form.getPhone())) {
            throw new RuntimeException("Пользователь с таким телефоном уже существует!");
        }

        User user = new User();
        user.setLogin(form.getLogin());
        user.setPassword(passwordEncoder.encode(form.getPassword()));
        user.setRole(RoleType.SUBSCRIBER);
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

        Subscriber subscriber = new Subscriber();
        subscriber.setUser(savedUser);
        Subscriber savedSubscriber = subscriberRepository.save(subscriber);

        BillingAccount billingAccount = new BillingAccount();
        billingAccount.setSubscriber(savedSubscriber);
        billingAccount.setNumber(generateBillingAccountNumber(savedSubscriber.getId()));
        billingAccount.setBalance(BigDecimal.ZERO);
        billingAccountRepository.save(billingAccount);
    }

    /**
     * Генерирует счет для нового абонента.
     */
    private String generateBillingAccountNumber(int subscriberId) {
        // 10 цифр, последние 6 - ID абонента с ведущими нулями
        return String.format("1000%06d", subscriberId);
    }
}
