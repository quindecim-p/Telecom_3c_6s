package telecom.controller.operator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import telecom.dto.OperatorSubscriberViewDTO;
import telecom.service.operator.SearchUsersService;

import java.util.List;

@Controller
@RequestMapping("/operator/subscribers")
@PreAuthorize("hasAnyRole('OPERATOR')")
public class SearchUsersController {

    private final SearchUsersService searchUsersService;

    @Autowired
    public SearchUsersController(SearchUsersService searchUsersService) {
        this.searchUsersService = searchUsersService;
    }

    /**
     * Отображает страницу поиска пользователей.
     */
    @GetMapping
    public String searchOrShowSubscribersPage(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "address", required = false) String address,
            @RequestParam(value = "banned", required = false) Boolean isBanned,
            @RequestParam(value = "minServices", required = false) Integer minServices,
            @RequestParam(value = "maxServices", required = false) Integer maxServices,
            Model model) {

        try {
            List<OperatorSubscriberViewDTO> results = searchUsersService.searchSubscribers(
                    keyword, address, isBanned, minServices, maxServices
            );
            model.addAttribute("subscriberViews", results);
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Произошла ошибка во время поиска.");
            model.addAttribute("subscriberViews", List.of());
        }

        model.addAttribute("keyword", keyword);
        model.addAttribute("address", address);
        model.addAttribute("banned", isBanned);
        model.addAttribute("minServices", minServices);
        model.addAttribute("maxServices", maxServices);

        model.addAttribute("pageTitle", "Поиск абонентов");

        return "operator/search_users";
    }
}