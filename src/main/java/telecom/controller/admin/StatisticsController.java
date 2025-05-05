package telecom.controller.admin;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import telecom.dto.StatisticsDTO;
import telecom.service.admin.StatisticsService;

@Controller
@RequestMapping("/admin/statistics")
@PreAuthorize("hasAnyRole('ADMIN')")
public class StatisticsController {

    private final StatisticsService statisticsService;

    @Autowired
    public StatisticsController(StatisticsService statisticsService) {
        this.statisticsService = statisticsService;
    }

    /**
     * Отображает статистику.
     */
    @GetMapping
    public String showStatisticsPage(Model model) {
        try {
            StatisticsDTO stats = statisticsService.getStatistics();
            model.addAttribute("stats", stats);
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Не удалось загрузить статистику: " + e.getMessage());
            model.addAttribute("stats", new StatisticsDTO());
        }
        return "admin/statistics";
    }
}