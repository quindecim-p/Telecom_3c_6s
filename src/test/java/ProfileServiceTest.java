import jakarta.validation.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import telecom.dto.AccountEditFormDTO;
import telecom.dto.PersonDataEditFormDTO;
import telecom.dto.ProfileViewDTO;
import telecom.entity.*;
import telecom.repository.*;
import telecom.service.general.ProfileService;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ProfileServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PersonDataRepository personDataRepository;
    @Mock private SubscriberRepository subscriberRepository;

    @InjectMocks private ProfileService profileService;

    private User user;
    private PersonData personData;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        user = new User();
        user.setId(1);
        user.setLogin("testUser");
        user.setPassword("encodedPassword");

        personData = new PersonData();
        personData.setUser(user);
        personData.setFullName("John Doe");
        personData.setPhone("1234567890");
        personData.setEmail("john@example.com");
    }

    @Test
    void getProfileViewData_shouldReturnProfileViewDTO() {
        when(userRepository.findByLogin("testUser")).thenReturn(Optional.of(user));
        when(personDataRepository.findByUser(user)).thenReturn(Optional.of(personData));
        when(subscriberRepository.findByUser(user)).thenReturn(Optional.empty());

        ProfileViewDTO dto = profileService.getProfileViewData("testUser");

        assertNotNull(dto);
        assertEquals(user, dto.getUser());
        assertEquals(personData, dto.getPersonData());
        assertNull(dto.getBillingAccount());
    }

    @Test
    void getPersonDataForEdit_shouldReturnForm() {
        when(userRepository.findByLogin("testUser")).thenReturn(Optional.of(user));
        when(personDataRepository.findByUser(user)).thenReturn(Optional.of(personData));

        var form = profileService.getPersonDataForEdit("testUser");

        assertNotNull(form);
        assertEquals("John Doe", form.getFullName());
        assertEquals("1234567890", form.getPhone());
    }

    @Test
    void getAccountDataForEdit_shouldReturnLogin() {
        when(userRepository.findByLogin("testUser")).thenReturn(Optional.of(user));

        var form = profileService.getAccountDataForEdit("testUser");

        assertNotNull(form);
        assertEquals("testUser", form.getLogin());
    }

    @Test
    void updatePersonData_shouldUpdateSuccessfully() {
        PersonDataEditFormDTO form = new PersonDataEditFormDTO();
        form.setFullName("Jane Doe");
        form.setPhone("0987654321");
        form.setEmail("jane@example.com");
        form.setAddress("New Address");

        when(userRepository.findByLogin("testUser")).thenReturn(Optional.of(user));
        when(personDataRepository.findByUser(user)).thenReturn(Optional.of(personData));
        when(personDataRepository.existsByEmailAndUserIdNot(anyString(), anyInt())).thenReturn(false);
        when(personDataRepository.existsByPhoneAndUserIdNot(anyString(), anyInt())).thenReturn(false);

        profileService.updatePersonData("testUser", form);

        assertEquals("Jane Doe", personData.getFullName());
        assertEquals("0987654321", personData.getPhone());
        assertEquals("jane@example.com", personData.getEmail());
        verify(personDataRepository).save(personData);
    }

    @Test
    void updateAccountData_shouldThrow_whenPasswordsDoNotMatch() {
        AccountEditFormDTO form = new AccountEditFormDTO();
        form.setLogin("testUser");
        form.setNewPassword("newPass");
        form.setConfirmNewPassword("differentPass");
        form.setCurrentPassword("oldPass");

        when(userRepository.findByLogin("testUser")).thenReturn(Optional.of(user));

        ValidationException ex = assertThrows(ValidationException.class,
                () -> profileService.updateAccountData("testUser", form));

        assertEquals("Новый пароль и подтверждение не совпадают.", ex.getMessage());
    }
}
