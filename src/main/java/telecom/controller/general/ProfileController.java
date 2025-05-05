package telecom.controller.general;

import jakarta.validation.Valid;
import jakarta.validation.ValidationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import telecom.dto.AccountEditFormDTO;
import telecom.dto.PersonDataEditFormDTO;
import telecom.dto.ProfileViewDTO;
import telecom.service.general.ProfileService;

import jakarta.persistence.EntityNotFoundException;
import java.util.Objects;

@Controller
@RequestMapping("/profile")
public class ProfileController {

    private final ProfileService profileService;

    @Autowired
    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    /**
     * Отображает профиль пользователя.
     */
    @GetMapping
    public String profilePage(Authentication authentication, Model model) {
        String login = authentication.getName();

        try {
            ProfileViewDTO profileData = profileService.getProfileViewData(login);
            model.addAttribute("profileData", profileData);

            if (!model.containsAttribute("personDataForm")) {
                try {
                    model.addAttribute("personDataForm", profileService.getPersonDataForEdit(login));
                } catch (EntityNotFoundException enfe) {
                    model.addAttribute("personDataForm", new PersonDataEditFormDTO());
                }
            }
            if (!model.containsAttribute("accountForm")) {
                model.addAttribute("accountForm", profileService.getAccountDataForEdit(login));
            }

            return "general/profile";
        } catch (UsernameNotFoundException e) {
            return "redirect:/login";
        } catch (Exception e) {
            model.addAttribute("globalErrorMessage", "Не удалось загрузить профиль: " + e.getMessage());
            model.addAttribute("profileData", new ProfileViewDTO());
            return "general/profile";
        }
    }

    /**
     * Обрабатывает обновление персональных данных пользователя.
     */
    @PostMapping("/personal")
    public String updatePersonalData(@Valid @ModelAttribute("personDataForm") PersonDataEditFormDTO form,
                                     BindingResult bindingResult,
                                     Authentication authentication,
                                     RedirectAttributes redirectAttributes,
                                     Model model) {
        String login = authentication.getName();

        if (bindingResult.hasErrors()) {
            model.addAttribute("accountForm", profileService.getAccountDataForEdit(login));
            model.addAttribute("personalDataError", "Пожалуйста, исправьте ошибки в форме.");
            model.addAttribute("profileData", profileService.getProfileViewData(login));
            return "general/profile";
        }

        try {
            profileService.updatePersonData(login, form);
            redirectAttributes.addFlashAttribute("successMessage", "Персональные данные успешно обновлены.");
            return "redirect:/profile";
        } catch (DataIntegrityViolationException e) {
            model.addAttribute("accountForm", profileService.getAccountDataForEdit(login));
            model.addAttribute("personalDataError", e.getMessage());
            model.addAttribute("profileData", profileService.getProfileViewData(login));
            return "general/profile";
        } catch (Exception e) {
            model.addAttribute("accountForm", profileService.getAccountDataForEdit(login));
            model.addAttribute("personalDataError", "Произошла непредвиденная ошибка.");
            model.addAttribute("profileData", profileService.getProfileViewData(login));
            return "general/profile";
        }
    }

    /**
     * Обрабатывает обновление данных аккаунта пользователя.
     */
    @PostMapping("/account")
    public String updateAccountData(@Valid @ModelAttribute("accountForm") AccountEditFormDTO form,
                                    BindingResult bindingResult,
                                    Authentication authentication,
                                    RedirectAttributes redirectAttributes,
                                    Model model) {
        String login = authentication.getName();

        if (StringUtils.hasText(form.getNewPassword()) && !Objects.equals(form.getNewPassword(), form.getConfirmNewPassword())) {
            bindingResult.rejectValue("confirmNewPassword", "error.password.mismatch", "Новый пароль и подтверждение не совпадают");
        }

        if (bindingResult.hasErrors()) {
            try {
                model.addAttribute("personDataForm", profileService.getPersonDataForEdit(login));
            } catch (EntityNotFoundException enfe){
                model.addAttribute("personDataForm", new PersonDataEditFormDTO());
            }
            model.addAttribute("accountDataError", "Пожалуйста, исправьте ошибки в форме.");
            form.setCurrentPassword(null);
            form.setNewPassword(null);
            form.setConfirmNewPassword(null);
            model.addAttribute("profileData", profileService.getProfileViewData(login));
            return "general/profile";
        }

        try {
            profileService.updateAccountData(login, form);
            redirectAttributes.addFlashAttribute("successMessage", "Данные аккаунта успешно обновлены.");

            if (!Objects.equals(login, form.getLogin())) {
                redirectAttributes.addFlashAttribute("logoutMessage", "Ваш логин был изменен. Пожалуйста, войдите снова.");
                SecurityContextHolder.clearContext();
                return "redirect:/login?logout=true";
            }
            return "redirect:/profile";
        } catch (ValidationException | DataIntegrityViolationException e) {
            try {
                model.addAttribute("personDataForm", profileService.getPersonDataForEdit(login));
            } catch (EntityNotFoundException enfe){
                model.addAttribute("personDataForm", new PersonDataEditFormDTO());
            }
            model.addAttribute("accountDataError", e.getMessage());
            form.setCurrentPassword(null);
            form.setNewPassword(null);
            form.setConfirmNewPassword(null);
            model.addAttribute("profileData", profileService.getProfileViewData(login));
            return "general/profile";
        } catch (Exception e) {
            try {
                model.addAttribute("personDataForm", profileService.getPersonDataForEdit(login));
            } catch (EntityNotFoundException enfe){
                model.addAttribute("personDataForm", new PersonDataEditFormDTO());
            }
            model.addAttribute("accountDataError", "Произошла непредвиденная ошибка.");
            form.setCurrentPassword(null);
            form.setNewPassword(null);
            form.setConfirmNewPassword(null);
            model.addAttribute("profileData", profileService.getProfileViewData(login));
            return "general/profile";
        }
    }
}