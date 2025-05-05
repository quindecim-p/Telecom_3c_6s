package telecom.enums;

import lombok.Getter;

@Getter
public enum TariffType {
    MOBILE("Мобильная связь"),
    INTERNET("Интернет"),
    TV("Телевидение"),
    SECURITY("Безопасность"),
    MIX("Комплекс");

    private final String displayName;

    TariffType(String displayName) {
        this.displayName = displayName;
    }

}