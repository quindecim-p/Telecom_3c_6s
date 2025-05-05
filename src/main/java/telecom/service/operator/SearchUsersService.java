package telecom.service.operator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import telecom.dto.OperatorSubscriberViewDTO;
import telecom.entity.*;
import telecom.enums.RoleType;
import telecom.enums.ServiceStatus;
import telecom.repository.*;
import telecom.config.UserSpecification;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SearchUsersService {

    private final UserRepository userRepository;
    private final PersonDataRepository personDataRepository;
    private final SubscriberRepository subscriberRepository;
    private final ConnectedServiceRepository connectedServiceRepository;
    private final BillingAccountRepository billingAccountRepository;

    @Autowired
    public SearchUsersService(UserRepository userRepository,
                              PersonDataRepository personDataRepository,
                              SubscriberRepository subscriberRepository,
                              ConnectedServiceRepository connectedServiceRepository,
                              BillingAccountRepository billingAccountRepository) {
        this.userRepository = userRepository;
        this.personDataRepository = personDataRepository;
        this.subscriberRepository = subscriberRepository;
        this.connectedServiceRepository = connectedServiceRepository;
        this.billingAccountRepository = billingAccountRepository;
    }

    /**
     * Ищет абонентов с использованием спецификаций для гибкой фильтрации.
     */
    @Transactional(readOnly = true)
    public List<OperatorSubscriberViewDTO> searchSubscribers(String keyword, String address, Boolean isBanned, Integer minServices, Integer maxServices) {

        Specification<User> spec = Specification
                .where(UserSpecification.hasRole(RoleType.SUBSCRIBER))
                .and(UserSpecification.matchesKeyword(keyword))
                .and(UserSpecification.addressLike(address))
                .and(UserSpecification.isBanned(isBanned));

        List<User> foundUsers = userRepository.findAll(spec);

        if (foundUsers.isEmpty()) {
            return Collections.emptyList();
        }

        List<Integer> userIds = foundUsers.stream().map(User::getId).toList();

        Map<Integer, PersonData> personDataMap = personDataRepository.findByUserIdIn(userIds).stream()
                .collect(Collectors.toMap(PersonData::getUserId, pd -> pd));

        Map<Integer, Subscriber> subscriberMap = subscriberRepository.findByUserIn(foundUsers).stream()
                .collect(Collectors.toMap(sub -> sub.getUser().getId(), sub -> sub));

        List<Subscriber> subscribersFound = new ArrayList<>(subscriberMap.values());

        List<ConnectedService> allActiveServices = connectedServiceRepository
                .findBySubscriberInAndStatus(subscribersFound, ServiceStatus.ACTIVE);

        Map<Integer, List<ConnectedService>> servicesByUser = allActiveServices.stream()
                .filter(cs -> cs.getSubscriber() != null && cs.getSubscriber().getUser() != null)
                .collect(Collectors.groupingBy(cs -> cs.getSubscriber().getUser().getId()));

        List<Integer> subscriberIds = subscribersFound.stream().map(Subscriber::getId).toList();
        Map<Integer, BillingAccount> billingAccountMap = billingAccountRepository.findBySubscriberIdIn(subscriberIds).stream()
                .collect(Collectors.toMap(BillingAccount::getSubscriberId, ba -> ba));

        List<OperatorSubscriberViewDTO> results = new ArrayList<>();
        for (User user : foundUsers) {
            OperatorSubscriberViewDTO dto = new OperatorSubscriberViewDTO();
            dto.setUserId(user.getId());
            dto.setBanned(user.isBanned());

            PersonData pd = personDataMap.get(user.getId());
            if (pd != null) {
                dto.setFullName(pd.getFullName());
                dto.setPhone(pd.getPhone());
                dto.setEmail(pd.getEmail());
                dto.setAddress(pd.getAddress());
                dto.setBirthDate(pd.getBirthDate());
            } else {
                dto.setFullName("[Нет данных]");
            }

            List<ConnectedService> userActiveServices = servicesByUser.getOrDefault(user.getId(), Collections.emptyList());
            dto.setActiveServiceCount(userActiveServices.size());

            BigDecimal totalCost = userActiveServices.stream()
                    .map(service -> service.getTariffPlan() != null ? service.getTariffPlan().getMonthlyPayment() : BigDecimal.ZERO)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            dto.setTotalMonthlyCost(totalCost);

            Subscriber subscriber = subscriberMap.get(user.getId());
            if (subscriber != null) {
                BillingAccount ba = billingAccountMap.get(subscriber.getId());
                dto.setBalance(ba != null ? ba.getBalance() : BigDecimal.ZERO);
            } else {
                dto.setBalance(BigDecimal.ZERO);
            }

            results.add(dto);
        }

        if (minServices != null || maxServices != null) {
            results = results.stream()
                    .filter(dto -> (minServices == null || dto.getActiveServiceCount() >= minServices))
                    .filter(dto -> (maxServices == null || dto.getActiveServiceCount() <= maxServices))
                    .collect(Collectors.toList());
        }

        return results;
    }
}