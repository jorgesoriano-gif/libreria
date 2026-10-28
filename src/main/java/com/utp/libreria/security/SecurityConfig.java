package com.utp.libreria.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();   // RNF02: contraseñas con hash seguro
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(a -> a
                .requestMatchers("/css/**", "/js/**", "/login", "/error").permitAll()
                // Solo Administrador: crear, editar y activar/desactivar productos
                .requestMatchers("/productos/nuevo", "/productos/*/editar").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/productos/**").hasRole("ADMIN")
                // Solo Administrador: categorías, usuarios y reportes
                .requestMatchers("/categorias/**", "/usuarios/**", "/reportes/**").hasRole("ADMIN")
                // Administrador e Inventario: stock, proveedores y compras
                .requestMatchers("/inventario/**", "/proveedores/**", "/compras/**").hasAnyRole("ADMIN", "INVENTARIO")
                // Vendedor (y Administrador)
                .requestMatchers("/clientes/**", "/ventas/**").hasAnyRole("ADMIN", "VENDEDOR")
                // Resto (panel y consulta de productos): cualquier usuario autenticado
                .anyRequest().authenticated())
            .formLogin(f -> f
                .loginPage("/login")
                .defaultSuccessUrl("/", true)
                .failureHandler((request, response, ex) -> {
                    boolean inactivo = ex instanceof DisabledException;
                    response.sendRedirect(request.getContextPath() + "/login?" + (inactivo ? "inactivo" : "error"));
                }))
            .logout(l -> l.logoutSuccessUrl("/login?logout"));
        return http.build();
    }
}
