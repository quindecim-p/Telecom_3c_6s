package telecom.controller.admin;

import org.springframework.dao.DataIntegrityViolationException;
import telecom.dto.UserPersonDataDTO;
import telecom.enums.RoleType;
import telecom.entity.User;
import telecom.service.admin.UsersService;
import telecom.dto.UserCreationDTO;
import jakarta.validation.Valid;
import org.springframework.validation.BindingResult;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/users")
@PreAuthorize("hasAnyRole('ADMIN')")
public class UsersController {

    private final UsersService usersService;

    @Autowired
    public UsersController(UsersService usersService) {
        this.usersService = usersService;
    }

    /**
     * Отображает список пользователей с учетом фильтров.
     */
    @GetMapping
    public String listUsers(@RequestParam(required = false) String keyword,
                            @RequestParam(required = false) RoleType role,
                            @PageableDefault(sort = "id") Pageable pageable,
                            Model model) {
        try {
            Page<UserPersonDataDTO> usersPage = usersService.findUsersWithDetails(keyword, role, pageable);
            model.addAttribute("usersPage", usersPage);

            model.addAttribute("keyword", keyword);
            model.addAttribute("selectedRole", role);

            model.addAttribute("allRoles", RoleType.values());

        } catch (Exception e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("usersPage", Page.empty(pageable));
            model.addAttribute("allRoles", RoleType.values());
        }
        return "admin/users";
    }

    /**
     * Изменяет роль пользователя.
     */
    @PostMapping("/role")
    public String changeUserRole(@RequestParam("userId") int userId,
                                 @RequestParam("role") RoleType role,
                                 RedirectAttributes redirectAttributes) {
        try {
            User user = usersService.updateUserRole(userId, role);
            redirectAttributes.addFlashAttribute("successMessage", "Роль пользователя '" + user.getLogin() + "' успешно изменена на " + role.name() + ".");
        } catch (EntityNotFoundException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Произошла ошибка при изменении роли пользователя.");
        }
        return "redirect:/admin/users";
    }

    /**
     * Изменяет статус блокировки пользователя.
     */
    @PostMapping("/ban")
    public String toggleUserBan(@RequestParam("userId") int userId, RedirectAttributes redirectAttributes) {
        try {
            User user = usersService.toggleBanStatus(userId);
            String action = user.isBanned() ? "заблокирован" : "разблокирован";
            redirectAttributes.addFlashAttribute("successMessage", "Пользователь '" + user.getLogin() + "' успешно " + action + ".");
        } catch (EntityNotFoundException | IllegalStateException | IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Произошла ошибка при изменении статуса пользователя.");
        }
        return "redirect:/admin/users";
    }

    /**
     * Удаляет пользователя.
     */
    @PostMapping("/delete")
    public String deleteUser(@RequestParam("userId") int userId, RedirectAttributes redirectAttributes) {
        try {
            usersService.deleteUserAndRelatedData(userId);
            redirectAttributes.addFlashAttribute("successMessage", "Пользователь с ID " + userId + " и все связанные данные успешно удалены.");
        } catch (EntityNotFoundException | IllegalStateException | IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Ошибка при удалении пользователя ID " + userId + ": " + e.getMessage());
        }
        return "redirect:/admin/users";
    }

    /**
     * Отображает форму для добавления нового пользователя.
     */
    @GetMapping("/add")
    public String showAddUserForm(Model model) {
        model.addAttribute("userCreationForm", new UserCreationDTO());
        model.addAttribute("allRoles", RoleType.values());
        return "admin/user_form";
    }

    /**
     * Обрабатывает данные из формы добавления нового пользователя.
     */
    @PostMapping("/add")
    public String processAddUserForm(@Valid @ModelAttribute("userCreationForm") UserCreationDTO form,
                                     BindingResult bindingResult,
                                     Model model,
                                     RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            model.addAttribute("allRoles", RoleType.values());
            return "admin/user_form";
        }

        if (form.getPassword() != null && !form.getPassword().equals(form.getConfirmPassword())) {
            bindingResult.rejectValue("confirmPassword", "error.password.mismatch", "Пароли не совпадают");
            model.addAttribute("allRoles", RoleType.values());
            return "admin/user_form";
        }

        try {
            User newUser = usersService.createUser(form);
            redirectAttributes.addFlashAttribute("successMessage", "Пользователь " + newUser.getLogin() + " успешно создан!");
            return "redirect:/admin/users";
        } catch (DataIntegrityViolationException e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("allRoles", RoleType.values());
            return "admin/user_form";
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Произошла непредвиденная ошибка при создании пользователя.");
            model.addAttribute("allRoles", RoleType.values());
            return "admin/user_form";
        }
    }
}