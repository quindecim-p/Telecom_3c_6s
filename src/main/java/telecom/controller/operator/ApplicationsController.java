package telecom.controller.operator;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import telecom.dto.OperatorServiceViewDTO;
import telecom.enums.ServiceStatus;
import telecom.service.operator.ApplicationsService;

import java.util.Arrays;
import java.util.List;

@Controller
@RequestMapping("/operator/applications")
@PreAuthorize("hasAnyRole('OPERATOR')")
public class ApplicationsController {

    private final ApplicationsService applicationsService;

    @Autowired
    public ApplicationsController(ApplicationsService applicationsService) {
        this.applicationsService = applicationsService;
    }

    /**
     * Отображает страницу управления заявками/услугами с фильтрацией по статусу.
     */
    @GetMapping
    public String showApplications(@RequestParam(value = "status", required = false, defaultValue = "PENDING") String statusParam, Model model) {
        ServiceStatus currentStatus;
        try {
            currentStatus = ServiceStatus.valueOf(statusParam.toUpperCase());
        } catch (IllegalArgumentException e) {
            currentStatus = ServiceStatus.PENDING;
            model.addAttribute("errorMessage", "Неверный статус '" + statusParam + "'. Показаны ожидающие заявки.");
        }

        try {
            List<OperatorServiceViewDTO> serviceViews = applicationsService.getServiceViewsByStatus(currentStatus);
            model.addAttribute("serviceViews", serviceViews);
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Ошибка при загрузке услуг со статусом " + currentStatus.name());
            model.addAttribute("serviceViews", List.of());
        }

        List<ServiceStatus> allStatuses = Arrays.asList(ServiceStatus.values());
        model.addAttribute("allStatuses", allStatuses);
        model.addAttribute("currentStatus", currentStatus);
        model.addAttribute("pageTitle", "Управление услугами: " + currentStatus.getDisplayName());

        return "operator/applications";
    }

    /**
     * Обработчики действий.
     */
    @PostMapping("/approve")
    public String approveApplication(@RequestParam("serviceId") int serviceId, RedirectAttributes ra) {
        return handleServiceAction(serviceId, applicationsService::approveService, "одобрена", "одобрения", "PENDING", ra);
    }

    @PostMapping("/reject")
    public String rejectApplication(@RequestParam("serviceId") int serviceId, RedirectAttributes ra) {
        return handleServiceAction(serviceId, applicationsService::rejectService, "отклонена", "отклонения", "PENDING", ra);
    }

    @PostMapping("/reactivate")
    public String reactivateApplication(@RequestParam("serviceId") int serviceId, RedirectAttributes ra) {
        return handleServiceAction(serviceId, applicationsService::reactivateService, "повторно активирована", "повторной активации", "INACTIVE", ra);
    }

    @PostMapping("/deactivate")
    public String deactivateApplication(@RequestParam("serviceId") int serviceId, RedirectAttributes ra) {
        return handleServiceAction(serviceId, applicationsService::deactivateService, "деактивирована", "деактивации", "ACTIVE", ra);
    }

    @PostMapping("/delete")
    public String deleteApplication(@RequestParam("serviceId") int serviceId, @RequestParam(value="returnStatus", defaultValue = "ACTIVE") String returnStatus, RedirectAttributes ra) {
        return handleServiceAction(serviceId, applicationsService::deleteService, "удалена", "удаления", returnStatus, ra);
    }

    /**
     * Вспомогательный метод для обработки действий.
     */
    private String handleServiceAction(int serviceId, ActionExecutor action, String successVerb, String errorNoun, String returnStatus, RedirectAttributes redirectAttributes) {
        try {
            action.execute(serviceId);
            redirectAttributes.addFlashAttribute("successMessage", "Услуга ID " + serviceId + " успешно " + successVerb + ".");
        } catch (EntityNotFoundException | IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Ошибка " + errorNoun + " услуги ID " + serviceId + ": " + e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Непредвиденная ошибка при обработке услуги ID " + serviceId + ".");
        }
        return "redirect:/operator/applications?status=" + returnStatus;
    }

    /**
     * Функциональный интерфейс для передачи методов сервиса.
     */
    @FunctionalInterface
    interface ActionExecutor {
        void execute(int serviceId);
    }
}