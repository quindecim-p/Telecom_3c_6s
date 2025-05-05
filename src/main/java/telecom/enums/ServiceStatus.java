package telecom.enums;

import lombok.Getter;

@Getter
public enum ServiceStatus {
    PENDING("Ожидает"),
    ACTIVE("Активна"),
    INACTIVE("Отключена"),
    REJECTED("Отклонена");

    private final String displayName;

    ServiceStatus(String displayName) {
        this.displayName = displayName;
    }

}