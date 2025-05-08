package telecom.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import java.io.IOException;

public class CustomAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        String redirectURL = "/profile"; // fallback URL

        label:
        for (GrantedAuthority auth : authentication.getAuthorities()) {
            String role = auth.getAuthority();

            switch (role) {
                case "ROLE_ADMIN":
                    redirectURL = "/admin/users";
                    break label;
                case "ROLE_OPERATOR":
                    redirectURL = "/operator/applications";
                    break label;
                case "ROLE_SUBSCRIBER":
                    redirectURL = "/profile";
                    break label;
            }
        }

        response.sendRedirect(redirectURL);
    }
}