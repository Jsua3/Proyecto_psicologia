package com.psychosim.security;

import com.psychosim.domain.user.User;
import com.psychosim.domain.user.UserRepository;
import com.psychosim.domain.user.UserRole;
import com.psychosim.shared.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest req) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.email(), req.password())
        );
        User user = userRepository.findByEmail(req.email())
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
        String token = jwtService.generateToken(user);
        return ResponseEntity.ok(ApiResponse.ok(new LoginResponse(token, UserSummary.from(user))));
    }

    @PostMapping("/register")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserSummary>> register(@Valid @RequestBody RegisterRequest req) {
        if (userRepository.existsByEmail(req.email())) {
            throw new IllegalArgumentException("El email ya está registrado");
        }
        User user = new User();
        user.setEmail(req.email());
        user.setPasswordHash(passwordEncoder.encode(req.password()));
        user.setNombre(req.nombre());
        user.setApellido(req.apellido());
        user.setRole(req.role());
        userRepository.save(user);
        return ResponseEntity.ok(ApiResponse.ok("Usuario creado exitosamente", UserSummary.from(user)));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserSummary>> me(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.ok(UserSummary.from(user)));
    }

    // --- Records internos ---

    public record LoginRequest(
            @Email @NotBlank String email,
            @NotBlank String password) {}

    public record RegisterRequest(
            @Email @NotBlank String email,
            @NotBlank @Size(min = 8) String password,
            @NotBlank String nombre,
            @NotBlank String apellido,
            UserRole role) {}

    public record LoginResponse(String token, UserSummary user) {}

    public record UserSummary(Long id, String nombre, String apellido, String email, String role) {
        static UserSummary from(User u) {
            return new UserSummary(u.getId(), u.getNombre(), u.getApellido(), u.getEmail(), u.getRole().name());
        }
    }
}
