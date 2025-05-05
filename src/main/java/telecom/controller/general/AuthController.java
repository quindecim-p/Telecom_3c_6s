package telecom.controller.general;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import telecom.dto.RegistrationFormDTO;
import telecom.enums.RoleType;
import telecom.service.general.RegistrationService;

@Controller
public class AuthController {

    private final RegistrationService registrationService;

    @Autowired
    public AuthController(RegistrationService registrationService) {
        this.registrationService = registrationService;
    }

    /**
     * Отображает форму логина.
     */
    @GetMapping("/login")
    public String loginPage(@RequestParam(value = "error", required = false) String error,
                            @RequestParam(value = "logout", required = false) String logout,
                            Model model) {
        if (error != null) {
            model.addAttribute("errorMessage", "Неверный логин или пароль.");
        }
        if (logout != null) {
            model.addAttribute("logoutMessage", "Вы успешно вышли из системы.");
        }
        return "general/login";
    }

    /**
     * Отображает форму регистрации.
     */
    @GetMapping("/register")
    public String registrationPage(Model model) {
        model.addAttribute("registrationForm", new RegistrationFormDTO());
        return "general/register";
    }

    /**
     * Обрабатывает данные из формы для регистрации.
     */
    @PostMapping("/register")
    public String processRegistration(@Valid @ModelAttribute("registrationForm") RegistrationFormDTO form,
                                      BindingResult bindingResult,
                                      Model model) {

        if (bindingResult.hasErrors()) {
            return "general/register";
        }

        if (form.getPassword() != null && !form.getPassword().equals(form.getConfirmPassword())) {
            bindingResult.rejectValue("confirmPassword", "error.password.mismatch", "Пароли не совпадают");
            model.addAttribute("allRoles", RoleType.values());
            return "general/register";
        }

        try {
            registrationService.registerUser(form);
            return "redirect:/login?registered=true";
        } catch (RuntimeException e) {
            model.addAttribute("registrationError", e.getMessage());
            return "general/register";
        }
    }
}