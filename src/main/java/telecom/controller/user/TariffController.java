package telecom.controller.user;

import jakarta.persistence.EntityNotFoundException;
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
import telecom.entity.TariffPlan;
import telecom.enums.TariffType;
import telecom.service.user.ConnectedServiceService;
import telecom.service.user.TariffPlanService;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Collections;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/tariffs")
public class TariffController {

    private final TariffPlanService tariffPlanService;
    private final ConnectedServiceService connectedServiceService;

    @Autowired
    public TariffController(TariffPlanService tariffPlanService,
                            ConnectedServiceService connectedServiceService) {
        this.tariffPlanService = tariffPlanService;
        this.connectedServiceService = connectedServiceService;
    }

    /**
     * Отображает список доступных тарифов для подключения.
     */
    @GetMapping
    public String showAvailableServices(Authentication authentication, Model model) {
        List<TariffPlan> allTariffs = tariffPlanService.getAllAvailableTariffs();
        Map<TariffType, List<TariffPlan>> groupedTariffs = allTariffs.stream()
                .collect(Collectors.groupingBy(TariffPlan::getType));

        Set<Integer> connectedOrPendingIds = Collections.emptySet();
        if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getPrincipal())) {
            boolean isSubscriber = authentication.getAuthorities().stream()
                    .anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals("ROLE_SUBSCRIBER"));

            if (isSubscriber) {
                String login = authentication.getName();
                connectedOrPendingIds = connectedServiceService.getActiveOrPendingTariffPlanIds(login);
            }
        }

        model.addAttribute("groupedTariffs", groupedTariffs);
        model.addAttribute("serviceTypes", TariffType.values());
        model.addAttribute("connectedOrPendingIds", connectedOrPendingIds);
        model.addAttribute("tariffs", allTariffs);
        return "user/tariffs";
    }

    /**
     * Обрабатывает подключение услуги абонента.
     */
    @PostMapping("/connect")
    public String requestServiceConnection(@RequestParam("tariffPlanId") int tariffPlanId,
                                           Authentication authentication,
                                           RedirectAttributes redirectAttributes) {

        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            redirectAttributes.addFlashAttribute("errorMessage", "Для подключения услуги необходимо войти.");
            return "redirect:/login";
        }

        String login = authentication.getName();

        try {
            connectedServiceService.requestServiceConnection(login, tariffPlanId);
            redirectAttributes.addFlashAttribute("successMessage", "Заявка на подключение услуги успешно отправлена! Ожидайте подтверждения оператором.");
        } catch (EntityNotFoundException | IllegalStateException | UsernameNotFoundException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Не удалось отправить заявку: " + e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Произошла непредвиденная ошибка при отправке заявки.");
        }
        return "redirect:/tariffs";
    }
}