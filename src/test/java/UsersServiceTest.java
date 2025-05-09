import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import telecom.dto.UserCreationDTO;
import telecom.dto.UserPersonDataDTO;
import telecom.entity.PersonData;
import telecom.entity.Subscriber;
import telecom.entity.User;
import telecom.enums.RoleType;
import telecom.repository.*;

import jakarta.persistence.EntityNotFoundException;
import telecom.service.admin.UsersService;

import java.util.Date;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class UsersServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PersonDataRepository personDataRepository;
    @Mock
    private SubscriberRepository subscriberRepository;
    @Mock
    private BillingAccountRepository billingAccountRepository;
    @Mock
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @InjectMocks
    private UsersService usersService;

    private User testUser;
    private UserCreationDTO creationDTO;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // Подготовка тестового User
        testUser = new User();
        testUser.setId(1);
        testUser.setLogin("testUser");
        testUser.setPassword("encodedPassword");
        testUser.setRole(RoleType.SUBSCRIBER);
        testUser.setBanned(false);

        // Подготовка DTO для createUser
        creationDTO = new UserCreationDTO();
        creationDTO.setLogin("newUser");
        creationDTO.setPassword("password123");
        creationDTO.setFullName("New User FullName");
        creationDTO.setEmail("newuser@example.com");
        creationDTO.setPhone("+375291234567");
        creationDTO.setAddress("New Address");
        creationDTO.setBirthDate(new Date());
        creationDTO.setRole(RoleType.SUBSCRIBER);

        // Мокаем SecurityContext
        Authentication authentication = new UsernamePasswordAuthenticationToken("adminUser", "password");
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    void findUsersWithDetails_shouldReturnEmptyPage_whenNoUsersFound() {
        Pageable pageable = PageRequest.of(0, 10);
        when(userRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(Page.empty());

        Page<UserPersonDataDTO> result = usersService.findUsersWithDetails("test", null, pageable);

        assertTrue(result.isEmpty());
        verify(userRepository).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    void toggleBanStatus_shouldToggleBanStatus() {
        UserDetails userDetails = org.springframework.security.core.userdetails.User
                .withUsername("adminUser")
                .password("password")
                .roles("ADMIN")
                .build();

        Authentication authentication = new UsernamePasswordAuthenticationToken(userDetails, "password", userDetails.getAuthorities());
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);


        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User result = usersService.toggleBanStatus(1);

        assertTrue(result.isBanned());
        verify(userRepository).save(testUser);
    }

    @Test
    void deleteUserAndRelatedData_shouldThrow_whenUserNotFound() {
        when(userRepository.findById(1)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> usersService.deleteUserAndRelatedData(1));
    }

    @Test
    void updateUserRole_shouldUpdateRole() {
        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User updatedUser = usersService.updateUserRole(1, RoleType.ADMIN);

        assertEquals(RoleType.ADMIN, updatedUser.getRole());
        verify(userRepository).save(testUser);
    }

    @Test
    void createUser_shouldCreateUserAndPersonData_whenRoleIsSubscriber() {
        // Мокаем проверку существования
        when(userRepository.existsByLogin("newUser")).thenReturn(false);
        when(personDataRepository.existsByEmail("newuser@example.com")).thenReturn(false);
        when(personDataRepository.existsByPhone("+375291234567")).thenReturn(false);

        // Мокаем сохранение User
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(2); // эмулируем сохранение с ID
            return u;
        });

        // Мокаем сохранение PersonData
        when(personDataRepository.save(any(PersonData.class))).thenAnswer(inv -> inv.getArgument(0));

        // Мокаем сохранение Subscriber
        when(subscriberRepository.save(any(Subscriber.class))).thenAnswer(inv -> {
            Subscriber s = inv.getArgument(0);
            s.setId(123); // Эмулируем ID абонента
            return s;
        });

        // Мокаем сохранение BillingAccount
        when(billingAccountRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // Мокаем кодировщик паролей
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword123");

        User createdUser = usersService.createUser(creationDTO);

        assertEquals("newUser", createdUser.getLogin());
        assertEquals(RoleType.SUBSCRIBER, createdUser.getRole());
        verify(personDataRepository).save(any(PersonData.class));
        verify(subscriberRepository).save(any(Subscriber.class));
        verify(billingAccountRepository).save(any());
    }

}
