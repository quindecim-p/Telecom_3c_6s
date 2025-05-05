package telecom.controller.admin;

import telecom.enums.TariffType;
import telecom.entity.TariffPlan;
import telecom.service.admin.TariffsService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/admin/tariffs")
@PreAuthorize("hasAnyRole('ADMIN')")
public class TariffsController {

    private final TariffsService tariffService;

    @Autowired
    public TariffsController(TariffsService tariffService) {
        this.tariffService = tariffService;
    }

    /**
     * Отображает список тарифов с учетом фильтров.
     */
    @GetMapping
    public String listTariffs(
            @RequestParam(required = false) String nameKeyword,
            @RequestParam(required = false) TariffType tariffType,
            @RequestParam(required = false) Double minPrice,
            @RequestParam(required = false) Double maxPrice,
            Model model) {

        List<TariffPlan> tariffs = tariffService.findTariffsWithFilter(nameKeyword, tariffType, minPrice, maxPrice);

        model.addAttribute("tariffs", tariffs);

        model.addAttribute("nameKeyword", nameKeyword);
        model.addAttribute("selectedTariffType", tariffType);
        model.addAttribute("minPrice", minPrice);
        model.addAttribute("maxPrice", maxPrice);

        model.addAttribute("tariffTypes", TariffType.values());

        return "admin/tariffs";
    }

    /**
     * Отображает форму для добавления нового тарифа.
     */
    @GetMapping("/add")
    public String showAddTariffForm(Model model) {
        model.addAttribute("tariff", new TariffPlan());
        model.addAttribute("tariffTypes", TariffType.values());
        model.addAttribute("pageTitle", "Добавить новый тариф");
        return "admin/tariff_form";
    }

    /**
     * Отображает форму для редактирования существующего тарифа.
     */
    @GetMapping("/edit/{id}")
    public String showEditTariffForm(@PathVariable("id") int id, Model model, RedirectAttributes redirectAttributes) {
        Optional<TariffPlan> tariffOpt = tariffService.findTariffById(id);
        if (tariffOpt.isPresent()) {
            model.addAttribute("tariff", tariffOpt.get());
            model.addAttribute("tariffTypes", TariffType.values());
            model.addAttribute("pageTitle", "Редактировать тариф: " + tariffOpt.get().getName());
            return "admin/tariff_form";
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "Тариф с ID " + id + " не найден.");
            return "redirect:/admin/tariffs";
        }
    }

    /**
     * Сохраняет новый или обновленный тариф.
     */
    @PostMapping("/save")
    public String saveTariff(@Valid @ModelAttribute("tariff") TariffPlan tariff,
                             BindingResult bindingResult,
                             Model model,
                             RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            model.addAttribute("tariffTypes", TariffType.values());
            model.addAttribute("pageTitle", tariff.getId() == 0 ? "Добавить новый тариф" : "Редактировать тариф: " + tariff.getName());
            return "admin/tariff_form";
        }

        try {
            tariffService.saveTariff(tariff);
            redirectAttributes.addFlashAttribute("successMessage", "Тариф '" + tariff.getName() + "' успешно сохранен.");
            return "redirect:/admin/tariffs";
        } catch (IllegalArgumentException e) {
            bindingResult.rejectValue("name", "error.tariff.duplicate", e.getMessage());
            model.addAttribute("tariffTypes", TariffType.values());
            model.addAttribute("pageTitle", tariff.getId() == 0 ? "Добавить новый тариф" : "Редактировать тариф: " + tariff.getName());
            return "admin/tariff_form";
        } catch (Exception e) {
            model.addAttribute("tariffTypes", TariffType.values());
            model.addAttribute("pageTitle", tariff.getId() == 0 ? "Добавить новый тариф" : "Редактировать тариф: " + tariff.getName());
            model.addAttribute("errorMessage", "Непредвиденная ошибка при сохранении тарифа.");
            return "admin/tariff_form";
        }
    }

    /**
     * Удаляет тариф.
     */
    @PostMapping("/delete/{id}")
    public String deleteTariff(@PathVariable("id") int id, RedirectAttributes redirectAttributes) {
        try {
            tariffService.deleteTariff(id);
            redirectAttributes.addFlashAttribute("successMessage", "Тариф с ID " + id + " успешно удален.");
        } catch (EntityNotFoundException | DataIntegrityViolationException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Непредвиденная ошибка при удалении тарифа.");
        }
        return "redirect:/admin/tariffs";
    }
}