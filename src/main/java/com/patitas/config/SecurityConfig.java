package com.patitas.config;

import com.patitas.Models.Usuario;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

/**
 * CONFIGURACIÓN DE SEGURIDAD (Spring Security)
 *
 * Esta clase responde a 3 preguntas:
 *   1) ¿Cómo se comparan las contraseñas?          -> passwordEncoder()
 *   2) ¿A dónde va cada usuario después de entrar?  -> successHandler()
 *   3) ¿Qué URLs puede ver cada quien?              -> filterChain()
 *
 * Las contraseñas se guardan cifradas con BCrypt: en la base de datos nunca queda la clave real.
 */
@Configuration       // Le dice a Spring: "esta clase define beans (objetos) de configuración".
@EnableWebSecurity   // Activa Spring Security para toda la aplicación web.
public class SecurityConfig {

    /**
     * 1) COMPARACIÓN DE CONTRASEÑAS
     * BCrypt cifra la clave con una sal aleatoria: dos usuarios con la misma clave tienen
     * textos distintos en la BD y no se puede "descifrar". Spring Security usa este mismo bean
     * para comparar lo que escribe el usuario en el login con el hash guardado.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * 2) REDIRECCIÓN DESPUÉS DEL LOGIN
     * Se ejecuta solo cuando el login fue exitoso. Aquí decidimos el destino según el rol.
     * 'auth.getPrincipal()' devuelve el usuario que acaba de entrar; como nuestra entidad
     * Usuario implementa UserDetails, podemos convertirla (cast) a Usuario.
     */
    @Bean
    public AuthenticationSuccessHandler successHandler() {
        return (request, response, auth) -> {
            Usuario u = (Usuario) auth.getPrincipal();
            String destino = switch (u.getRol()) {
                case ADMIN -> "/admin/panel";
                case REFUGIO -> "/refugio/panel";
                // El adoptante nuevo debe llenar primero su perfil de estilo de vida.
                case ADOPTANTE -> u.isPerfilCompleto() ? "/adoptante/catalogo" : "/adoptante/perfil";
            };
            // getContextPath() evita romper el enlace si la app se despliega bajo un prefijo.
            response.sendRedirect(request.getContextPath() + destino);
        };
    }

    /**
     * 3) REGLAS DE ACCESO A LAS URLs
     * Spring evalúa las reglas DE ARRIBA HACIA ABAJO y aplica la primera que coincide.
     * Por eso van primero las específicas y al final la regla general (anyRequest).
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, AuthenticationSuccessHandler successHandler) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                // Páginas libres: inicio, login, registro, estilos y la página de error.
                .requestMatchers("/", "/public/**", "/login", "/registro", "/css/**", "/error").permitAll()
                // Zonas protegidas por ROL. hasRole("ADMIN") busca la autoridad "ROLE_ADMIN"
                // (la que construimos en Usuario.getAuthorities()).
                .requestMatchers("/adoptante/**").hasRole("ADOPTANTE")
                .requestMatchers("/refugio/**").hasRole("REFUGIO")
                .requestMatchers("/admin/**").hasRole("ADMIN")
                // Cualquier otra URL que no listamos arriba exige haber iniciado sesión.
                .anyRequest().authenticated())
            .formLogin(form -> form
                .loginPage("/login")              // Usamos NUESTRA pantalla, no la genérica de Spring.
                .successHandler(successHandler)  
                .failureUrl("/login?error")      
                .permitAll())
            .logout(logout -> logout
                .logoutSuccessUrl("/login?logout") // Al salir, vuelve al login con un aviso.
                .permitAll());
        return http.build();
    }
}
