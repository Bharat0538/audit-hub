package in.audithub.iam.service;

import in.audithub.common.exception.UnauthorizedException;
import in.audithub.common.security.JwtService;
import in.audithub.iam.model.Session;
import in.audithub.iam.model.User;
import in.audithub.iam.repository.SessionRepository;
import in.audithub.iam.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final SessionRepository sessionRepository;
    private final JwtService jwtService;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public LoginResponse login(String email, String password) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid email or password");
        }

        String jti = UUID.randomUUID().toString();
        long ttlMs = 86400000L; // 24 hours
        Instant expiresAt = Instant.now().plusMillis(ttlMs);

        Session session = Session.builder()
                .id(jti)
                .user(user)
                .organizationId(user.getOrganization().getId())
                .isActive(true)
                .expiresAt(expiresAt)
                .build();

        sessionRepository.save(session);

        String accessToken = jwtService.generateToken(
                user.getEmail(),
                user.getId(),
                user.getOrganization().getId(),
                jti,
                ttlMs
        );

        // Refresh token — 7-day lifetime for local dev
        String refreshToken = jwtService.generateToken(
                user.getEmail(),
                user.getId(),
                user.getOrganization().getId(),
                UUID.randomUUID().toString(),
                ttlMs * 7
        );

        user.setLastLoginAt(Instant.now());
        userRepository.save(user);

        LoginResponse.UserInfo userInfo = new LoginResponse.UserInfo(
                user.getId().toString(),
                user.getName(),
                user.getEmail(),
                "OWNER",
                user.getOrganization().getId().toString()
        );

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(ttlMs / 1000)
                .expiresAt(expiresAt.toString())
                .user(userInfo)
                .build();
    }

    @lombok.Value
    @lombok.Builder
    public static class LoginResponse {
        String accessToken;
        String refreshToken;
        long expiresIn;
        String expiresAt;
        UserInfo user;

        @lombok.Value
        public static class UserInfo {
            String id;
            String name;
            String email;
            String role;
            String organizationId;
        }
    }
}
