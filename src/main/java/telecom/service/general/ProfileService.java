package telecom.service.general;

import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ValidationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import telecom.dto.AccountEditFormDTO;
import telecom.dto.PersonDataEditFormDTO;
import telecom.dto.ProfileViewDTO;
import telecom.entity.*;
import telecom.repository.*;

import java.util.Objects;
import java.util.Optional;

@Service
public class ProfileService {

    private final UserRepository userRepository;
    private final PersonDataRepository personDataRepository;
    private final PasswordEncoder passwordEncoder;
    private final SubscriberRepository subscriberRepository;
    private final BillingAccountRepository billingAccountRepository;

    @Autowired
    public ProfileService(UserRepository userRepository,
                          PersonDataRepository personDataRepository,
                          PasswordEncoder passwordEncoder,
                          SubscriberRepository subscriberRepository,
                          BillingAccountRepository billingAccountRepository) {
        this.userRepository = userRepository;
        this.personDataRepository = personDataRepository;
        this.passwordEncoder = passwordEncoder;
        this.subscriberRepository = subscriberRepository;
        this.billingAccountRepository = billingAccountRepository;
    }

    /**
     * Получает данные пользователя.
     */
    @Transactional(readOnly = true)
    public ProfileViewDTO getProfileViewData(String login) {
        User user = findUserByLogin(login);
        PersonData personData = personDataRepository.findByUser(user).orElse(null);

        ProfileViewDTO dto = new ProfileViewDTO();
        dto.setUser(user);
        dto.setPersonData(personData);

        Optional<Subscriber> subscriberOpt = subscriberRepository.findByUser(user);
        if (subscriberOpt.isPresent()) {
            Subscriber subscriber = subscriberOpt.get();
            Optional<BillingAccount> billingAccountOpt = billingAccountRepository.findBySubscriber(subscriber);
            billingAccountOpt.ifPresent(dto::setBillingAccount);
        }
        return dto;
    }

    /**
     * Получает данные для редактирования персональных данных пользователя.
     */
    @Transactional(readOnly = true)
    public PersonDataEditFormDTO getPersonDataForEdit(String login) {
        User user = findUserByLogin(login);
        PersonData personData = personDataRepository.findByUser(user)
                .orElseThrow(() -> new EntityNotFoundException("Персональные данные для пользователя " + login + " не найдены."));

        PersonDataEditFormDTO form = new PersonDataEditFormDTO();
        form.setFullName(personData.getFullName());
        form.setPhone(personData.getPhone());
        form.setEmail(personData.getEmail());
        form.setAddress(personData.getAddress());
        form.setBirthDate(personData.getBirthDate());
        return form;
    }

    /**
     * Получает данные для редактирования данных аккаунта пользователя.
     */
    @Transactional(readOnly = true)
    public AccountEditFormDTO getAccountDataForEdit(String login) {
        User user = findUserByLogin(login);
        AccountEditFormDTO form = new AccountEditFormDTO();
        form.setLogin(user.getLogin());
        return form;
    }

    /**
     * Обновляет персональные данные пользователя.
     */
    @Transactional
    public void updatePersonData(String currentLogin, PersonDataEditFormDTO form) {
        User user = findUserByLogin(currentLogin);
        PersonData personData = personDataRepository.findByUser(user)
                .orElseThrow(() -> new EntityNotFoundException("Персональные данные для пользователя " + currentLogin + " не найдены."));

        if (!Objects.equals(personData.getEmail(), form.getEmail()) && personDataRepository.existsByEmailAndUserIdNot(form.getEmail(), user.getId())) {
            throw new DataIntegrityViolationException("Пользователь с таким email уже существует!");
        }
        if (!Objects.equals(personData.getPhone(), form.getPhone()) && personDataRepository.existsByPhoneAndUserIdNot(form.getPhone(), user.getId())) {
            throw new DataIntegrityViolationException("Пользователь с таким телефоном уже существует!");
        }

        personData.setFullName(form.getFullName());
        personData.setPhone(form.getPhone());
        personData.setEmail(form.getEmail());
        personData.setAddress(form.getAddress());
        personData.setBirthDate(form.getBirthDate());

        personDataRepository.save(personData);
    }

    /**
     * Обновляет данные аккаунта пользователя.
     */
    @Transactional
    public void updateAccountData(String currentLogin, AccountEditFormDTO form) {
        User user = findUserByLogin(currentLogin);

        if (!Objects.equals(user.getLogin(), form.getLogin())) {
            if (userRepository.existsByLoginAndIdNot(form.getLogin(), user.getId())) {
                throw new DataIntegrityViolationException("Произошла непредвиденная ошибка!");
            }
            user.setLogin(form.getLogin());
        }

        if (StringUtils.hasText(form.getNewPassword())) {
            if (!Objects.equals(form.getNewPassword(), form.getConfirmNewPassword())) {
                throw new ValidationException("Новый пароль и подтверждение не совпадают.");
            }
            if (StringUtils.hasText(form.getCurrentPassword())) {
                if (!passwordEncoder.matches(form.getCurrentPassword(), user.getPassword())) {
                    throw new ValidationException("Текущий пароль введен неверно.");
                }
            } else {
                throw new ValidationException("Для смены пароля необходимо ввести текущий пароль.");
            }

            user.setPassword(passwordEncoder.encode(form.getNewPassword()));
        } else {
            if (StringUtils.hasText(form.getCurrentPassword()) || StringUtils.hasText(form.getConfirmNewPassword())) {
                throw new ValidationException("Для смены пароля необходимо ввести новый пароль и подтверждение.");
            }
        }

        userRepository.save(user);
    }

    /**
     * Вспомогательный метод для поиска пользователя
     */
    private User findUserByLogin(String login) {
        return userRepository.findByLogin(login)
                .orElseThrow(() -> new UsernameNotFoundException("Пользователь не найден: " + login));
    }
}