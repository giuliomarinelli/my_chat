package backend.app.my_chat.security;

import backend.app.my_chat.Models.TokenPair;
import backend.app.my_chat.Models.dto.outputDto.HttpErrorOutputDto;
import backend.app.my_chat.Models.entities.User;
import backend.app.my_chat.exception_handling.UnauthorizedException;
import backend.app.my_chat.repositories.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.ExpiredJwtException;
import io.micrometer.common.lang.NonNull;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
//@Log4j2
public class JwtFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    @Qualifier("time_to_activate")
    private Long timeToActivate;


    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest req, @NonNull HttpServletResponse res, @NonNull FilterChain filterChain) throws ServletException, IOException {
        try {

            if (req.getCookies() == null) {
                throw new UnauthorizedException("No provided access and refresh tokens");
            }
            Cookie[] cookies = req.getCookies();
            TokenPair tokens = new TokenPair();
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals("__access_tkn")) tokens.setAccessToken(cookie.getValue());
                if (cookie.getName().equals("__refresh_tkn")) tokens.setRefreshToken(cookie.getValue());
            }
            if (tokens.getAccessToken() == null) throw new UnauthorizedException("Access token non fornito");
            if (tokens.getRefreshToken() == null) throw new UnauthorizedException("Refresh token non fornito");
            try {
                jwtUtils.verifyAccessToken(tokens.getAccessToken());
                UUID userId = jwtUtils.extractUserIdFromAccessToken(tokens.getAccessToken());
                User u = userRepository.findValidUserById(userId, timeToActivate).orElseThrow(
                        () -> new Exception("Access Token non valido")
                );
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(u, cookies);
                SecurityContextHolder.getContext().setAuthentication(authentication);
                filterChain.doFilter(req, res);
            } catch (ExpiredJwtException e) {
                throw new Exception("Access Token scaduto");
            }


        } catch (Exception e) {
            ObjectMapper mapper = new ObjectMapper();
            res.setStatus(HttpStatus.UNAUTHORIZED.value());
            res.setContentType("application/json;charset=UTF-8");
            res.getWriter().write(mapper.writeValueAsString(
                    new HttpErrorOutputDto(HttpStatus.UNAUTHORIZED, "Unauthorized", e.getMessage(), req.getRequestURI())
            ));
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest req) throws ServletException {
        return new AntPathMatcher().match("/auth/**", req.getServletPath());
    }

}