import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import telecom.dto.OperatorServiceViewDTO;
import telecom.entity.*;
import telecom.enums.ServiceStatus;
import telecom.repository.ConnectedServiceRepository;
import telecom.repository.PersonDataRepository;
import telecom.service.operator.ApplicationsService;

import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ApplicationsServiceTest {

    @Mock
    private ConnectedServiceRepository connectedServiceRepository;

    @Mock
    private PersonDataRepository personDataRepository;

    @InjectMocks
    private ApplicationsService applicationsService;

    private ConnectedService pendingService;
    private ConnectedService activeService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        pendingService = new ConnectedService();
        pendingService.setId(1);
        pendingService.setStatus(ServiceStatus.PENDING);

        activeService = new ConnectedService();
        activeService.setId(2);
        activeService.setStatus(ServiceStatus.ACTIVE);
    }

    @Test
    void approveService_shouldSetStatusToActiveAndSetNextBillingDate() {
        when(connectedServiceRepository.findById(1)).thenReturn(Optional.of(pendingService));

        applicationsService.approveService(1);

        assertEquals(ServiceStatus.ACTIVE, pendingService.getStatus());
        assertNotNull(pendingService.getNextBillingDate());
        verify(connectedServiceRepository).save(pendingService);
    }

    @Test
    void approveService_shouldThrowException_whenStatusIsNotPending() {
        when(connectedServiceRepository.findById(2)).thenReturn(Optional.of(activeService));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> {
            applicationsService.approveService(2);
        });

        assertTrue(ex.getMessage().contains("Можно одобрить только услугу в статусе"));
        verify(connectedServiceRepository, never()).save(any());
    }

    @Test
    void deactivateService_shouldSetStatusToInactive() {
        when(connectedServiceRepository.findById(2)).thenReturn(Optional.of(activeService));

        applicationsService.deactivateService(2);

        assertEquals(ServiceStatus.INACTIVE, activeService.getStatus());
        verify(connectedServiceRepository).save(activeService);
    }

    @Test
    void deleteService_shouldDeleteService() {
        when(connectedServiceRepository.findById(1)).thenReturn(Optional.of(pendingService));

        applicationsService.deleteService(1);

        verify(connectedServiceRepository).delete(pendingService);
    }

    @Test
    void getServiceViewsByStatus_shouldReturnDtoListWithPersonData() {
        ConnectedService service = new ConnectedService();
        service.setId(3);
        service.setStatus(ServiceStatus.PENDING);
        service.setStartDate(LocalDate.of(2024, 1, 1));

        TariffPlan tariffPlan = new TariffPlan();
        tariffPlan.setName("Test Tariff");
        service.setTariffPlan(tariffPlan);

        Subscriber subscriber = new Subscriber();
        User user = new User();
        user.setId(5);
        subscriber.setUser(user);
        service.setSubscriber(subscriber);

        PersonData personData = new PersonData();
        personData.setUserId(5);
        personData.setFullName("John Doe");
        personData.setPhone("+123456789");

        when(connectedServiceRepository.findByStatusOrderByIdAsc(ServiceStatus.PENDING))
                .thenReturn(List.of(service));
        when(personDataRepository.findByUserIdIn(List.of(5)))
                .thenReturn(List.of(personData));

        List<OperatorServiceViewDTO> result = applicationsService.getServiceViewsByStatus(ServiceStatus.PENDING);

        assertEquals(1, result.size());
        OperatorServiceViewDTO dto = result.get(0);
        assertEquals(3, dto.getServiceId());
        assertEquals(ServiceStatus.PENDING, dto.getStatus());
        assertEquals("Test Tariff", dto.getTariffName());
        assertEquals("John Doe", dto.getUserFullName());
        assertEquals("+123456789", dto.getUserPhone());
    }
}
