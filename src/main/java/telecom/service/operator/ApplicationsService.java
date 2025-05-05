package telecom.service.operator;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import telecom.dto.OperatorServiceViewDTO;
import telecom.entity.ConnectedService;
import telecom.entity.PersonData;
import telecom.enums.ServiceStatus;
import telecom.repository.ConnectedServiceRepository;
import telecom.repository.PersonDataRepository;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ApplicationsService {

    private final ConnectedServiceRepository connectedServiceRepository;
    private final PersonDataRepository personDataRepository;

    @Autowired
    public ApplicationsService(ConnectedServiceRepository connectedServiceRepository,
                               PersonDataRepository personDataRepository) {
        this.connectedServiceRepository = connectedServiceRepository;
        this.personDataRepository = personDataRepository;
    }

    /**
     * Получает список услуг по статусу.
     */
    @Transactional(readOnly = true)
    public List<OperatorServiceViewDTO> getServiceViewsByStatus(ServiceStatus status) {
        List<ConnectedService> services = connectedServiceRepository.findByStatusOrderByIdAsc(status);

        if (services.isEmpty()) {
            return Collections.emptyList();
        }

        Set<Integer> userIds = services.stream()
                .map(service -> {
                    try {
                        return service.getSubscriber().getUser().getId();
                    } catch (NullPointerException e) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        if (userIds.isEmpty()) {
            return mapServicesToDTOWithoutUserData(services);
        }

        Map<Integer, PersonData> personDataMap = personDataRepository.findByUserIdIn(new ArrayList<>(userIds)).stream()
                .collect(Collectors.toMap(PersonData::getUserId, pd -> pd));

        return services.stream()
                .map(service -> {
                    OperatorServiceViewDTO dto = new OperatorServiceViewDTO();
                    dto.setServiceId(service.getId());
                    dto.setStartDate(service.getStartDate());
                    dto.setStatus(service.getStatus());
                    dto.setTariffName(service.getTariffPlan() != null ? service.getTariffPlan().getName() : "[Тариф не найден]");

                    Integer userId = null;
                    try {
                        userId = service.getSubscriber().getUser().getId();
                    } catch (NullPointerException e) {
                        System.err.println("Не удалось получить userId: " + e.getMessage());
                    }

                    if (userId != null) {
                        PersonData pd = personDataMap.get(userId);
                        dto.setUserFullName(pd != null ? pd.getFullName() : "[ФИО не найдено]");
                        dto.setUserPhone(pd != null ? pd.getPhone() : "[Телефон не найден]");
                    } else {
                        dto.setUserFullName("[Абонент не найден]");
                        dto.setUserPhone("");
                    }
                    return dto;
                })
                .collect(Collectors.toList());
    }

    /**
     * Возвращает список услуг без персональных данных.
     */
    private List<OperatorServiceViewDTO> mapServicesToDTOWithoutUserData(List<ConnectedService> services) {
        return services.stream().map(service -> {
            OperatorServiceViewDTO dto = new OperatorServiceViewDTO();
            dto.setServiceId(service.getId());
            dto.setStartDate(service.getStartDate());
            dto.setStatus(service.getStatus());
            dto.setTariffName(service.getTariffPlan() != null ? service.getTariffPlan().getName() : "[Тариф не найден]");
            dto.setUserFullName("[Ошибка загрузки абонента]");
            dto.setUserPhone("");
            return dto;
        }).collect(Collectors.toList());
    }

    /**
     * Обработчики действий.
     */
    @Transactional
    public void approveService(int serviceId) {
        ConnectedService service = findServiceByIdAndCheckStatus(serviceId, ServiceStatus.PENDING, "одобрить");
        service.setStatus(ServiceStatus.ACTIVE);
        service.setNextBillingDate(LocalDate.now().plusMonths(1));
        connectedServiceRepository.save(service);
        // TODO: Логика списания средств СРАЗУ при одобрении
    }

    @Transactional
    public void reactivateService(int serviceId) {
        ConnectedService service = findServiceByIdAndCheckStatus(serviceId, ServiceStatus.INACTIVE, "повторно активировать");
        service.setStatus(ServiceStatus.ACTIVE);
        service.setNextBillingDate(LocalDate.now().plusMonths(1));
        connectedServiceRepository.save(service);
    }

    @Transactional
    public void rejectService(int serviceId) {
        ConnectedService service = findServiceByIdAndCheckStatus(serviceId, ServiceStatus.PENDING, "отклонить");
        service.setStatus(ServiceStatus.REJECTED);
        connectedServiceRepository.save(service);
    }

    @Transactional
    public void deactivateService(int serviceId) {
        ConnectedService service = findServiceByIdAndCheckStatus(serviceId, ServiceStatus.ACTIVE, "деактивировать");
        service.setStatus(ServiceStatus.INACTIVE);
        connectedServiceRepository.save(service);
    }

    @Transactional
    public void deleteService(int serviceId) {
        ConnectedService service = connectedServiceRepository.findById(serviceId)
                .orElseThrow(() -> new EntityNotFoundException("Услуга с ID " + serviceId + " не найдена для удаления."));
        connectedServiceRepository.delete(service);
    }

    /**
     * Находит услугу по статусу.
     */
    private ConnectedService findServiceByIdAndCheckStatus(int serviceId, ServiceStatus expectedStatus, String actionVerb) {
        ConnectedService service = connectedServiceRepository.findById(serviceId)
                .orElseThrow(() -> new EntityNotFoundException("Услуга с ID " + serviceId + " не найдена"));

        if (service.getStatus() != expectedStatus) {
            throw new IllegalStateException("Можно " + actionVerb + " только услугу в статусе " + expectedStatus + ". Текущий статус: " + service.getStatus());
        }
        return service;
    }
}