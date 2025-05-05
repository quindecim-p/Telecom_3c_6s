package telecom.controller.user;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.access.prepost.PreAuthorize;
import telecom.entity.ConnectedService;
import telecom.service.user.ConnectedServiceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/services")
@PreAuthorize("hasAnyRole('SUBSCRIBER')")
public class ConnectedServiceController {

    private final ConnectedServiceService connectedServiceService;

    @Autowired
    public ConnectedServiceController(ConnectedServiceService connectedServiceService) {
        this.connectedServiceService = connectedServiceService;
    }

    /**
     * Отображает список подключенных услуг абонента.
     */
    @GetMapping("/my")
    public String showMyServices(Authentication authentication, Model model) {
        String login = authentication.getName();

        try {
            List<ConnectedService> services = connectedServiceService.getAllServicesForSubscriber(login);
            double totalCost = connectedServiceService.calculateTotalMonthlyCost(login);
            model.addAttribute("services", services);
            model.addAttribute("totalMonthlyCost", totalCost);
        } catch (UsernameNotFoundException | EntityNotFoundException e) {
            model.addAttribute("infoMessage", "Не удалось загрузить информацию об услугах: " + e.getMessage());
            model.addAttribute("services", List.of());
            model.addAttribute("totalMonthlyCost", 0.0);
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Произошла ошибка при загрузке ваших услуг.");
            model.addAttribute("services", List.of());
            model.addAttribute("totalMonthlyCost", 0.0);
        }
        model.addAttribute("pageTitle", "Мои услуги");
        return "user/services";
    }

    /**
     * Обрабатывает отключение услуги абонентом.
     */
    @PostMapping("/disconnect")
    public String disconnectService(@RequestParam("serviceId") int serviceId,
                                    Authentication authentication,
                                    RedirectAttributes redirectAttributes) {
        String login = authentication.getName();

        try {
            connectedServiceService.disconnectService(login, serviceId);
            redirectAttributes.addFlashAttribute("successMessage", "Услуга ID " + serviceId + " успешно отключена.");
        } catch (EntityNotFoundException | UsernameNotFoundException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Невозможно отключить услугу: " + e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Произошла ошибка при отключении услуги.");
        }

        return "redirect:/services/my";
    }
}