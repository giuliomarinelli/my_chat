package backend.app.my_chat.security;

import backend.app.my_chat.Models.TokenPair;
import backend.app.my_chat.Models.entities.RevokedToken;
import backend.app.my_chat.Models.entities.User;
import backend.app.my_chat.Models.enums.TokenType;
import backend.app.my_chat.exception_handling.UnauthorizedException;
import backend.app.my_chat.repositories.RevokedTokenRepository;
import backend.app.my_chat.repositories.UserRepository;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Date;
import java.util.UUID;

@PropertySource("application.properties")
@Component
public class JwtUtils {

    @Value("${access_token.secret}")
    private String accessSecret;

    @Value("${refresh_token.secret}")
    private String refreshSecret;

    @Value("${access_token.expiresIn}")
    private String accessExp;

    @Value("${refresh_token.expiresIn}")
    private String refreshExp;

    @Value("${preauthorization_token.expiresIn}")
    private String preAuthorizationSecret;

    @Value("${preauthorization_token.expiresIn}")
    private String preAuthorizationExp;

    @Autowired
    @Qualifier("time_to_activate")
    private Long timeToActivate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RevokedTokenRepository revokedTokenRepository;


    public void revokeRefreshToken(String refreshToken) {
        UUID jti = UUID.fromString(
                (String) Jwts.parser().verifyWith(Keys.hmacShaKeyFor(refreshSecret.getBytes()))
                        .build().parseSignedClaims(refreshToken).getPayload().get("jti")
        );
        RevokedToken revokedToken = new RevokedToken(jti);
        revokedTokenRepository.save(revokedToken);
    }

    public boolean isRevokedToken(UUID jti) {
        return revokedTokenRepository.findById(jti).isPresent();
    }

    public TokenPair generateNewTokenPair(String refreshToken, boolean restore) throws UnauthorizedException {
        try {
            String secret = "";

            UUID userId = UUID.fromString(
                    Jwts.parser().
                            verifyWith(Keys.hmacShaKeyFor(refreshSecret.getBytes())).
                            build()
                            .parseSignedClaims(refreshToken).
                            getPayload().
                            getSubject());
            User u = userRepository.findValidUserById(userId, timeToActivate).orElseThrow(
                    UnauthorizedException::new
            );

            return new TokenPair(
                    generateToken(u, TokenType.ACCESS_TOKEN, restore),
                    generateToken(u, TokenType.REFRESH_TOKEN, restore)
            );

        } catch (Exception exception) {
            throw new UnauthorizedException("Invalid refresh token");
        }


    }

    public String generateToken(User u, TokenType type, boolean restore) throws UnauthorizedException {
        long exp = 1;
        String secret = accessSecret;
        switch (type) {
            case TokenType.ACCESS_TOKEN -> {
                exp = Long.parseLong(accessExp);
                secret = accessSecret;
            }
            case TokenType.REFRESH_TOKEN -> {
                exp = Long.parseLong(refreshExp);
                secret = refreshSecret;
            }
            case TokenType.PRE_AUTHORIZATION_TOKEN -> {
                exp = Long.parseLong(preAuthorizationExp);
                secret = preAuthorizationSecret;
            }
            default -> throw new UnauthorizedException();

        }

        return Jwts.builder()
                .issuer("my_chat")
                .subject(u.getId().toString())
                .claim("restore", restore)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + exp))
                .claim("jti", UUID.randomUUID())
                .signWith(Keys.hmacShaKeyFor(secret.getBytes()))
                .compact();

    }

    public void verifyAccessToken(String accessToken) throws UnauthorizedException {

        try {
            Jwts.parser().verifyWith(Keys.hmacShaKeyFor(accessSecret.getBytes())).build()
                    .parseSignedClaims(accessToken).getPayload();
        } catch (MalformedJwtException | SignatureException | UnsupportedJwtException | IllegalArgumentException e) {
            throw new UnauthorizedException("Invalid access token");
        } catch (ExpiredJwtException e) {
            throw e;
        } catch (Exception e) {
            throw new UnauthorizedException("Invalid access token");
        }

    }

    public boolean verifyRefreshToken(String refreshToken) throws UnauthorizedException {
        Claims claims;
        try {
            claims = Jwts.parser().verifyWith(Keys.hmacShaKeyFor(refreshSecret.getBytes())).build()
                    .parseSignedClaims(refreshToken).getPayload();
            return (Boolean) claims.get("restore");
        } catch (Exception e) {
            throw new UnauthorizedException("Invalid refresh token");
        }

    }

    public UUID extractUserIdFromAccessToken(String accessToken) throws UnauthorizedException {

        try {
            return UUID.fromString(Jwts.parser()
                    .verifyWith(Keys.hmacShaKeyFor(accessSecret.getBytes()))
                    .build()
                    .parseSignedClaims(accessToken)
                    .getPayload()
                    .getSubject());
        } catch (Exception e) {
            throw new UnauthorizedException("Invalid access token");
        }

    }


    public UUID extractUserIdFromRefreshToken(String refreshToken) throws UnauthorizedException {
        try {
            return UUID.fromString(Jwts.parser()
                    .verifyWith(Keys.hmacShaKeyFor(refreshSecret.getBytes()))
                    .build()
                    .parseSignedClaims(refreshToken)
                    .getPayload()
                    .getSubject());
        } catch (Exception e) {
            throw new UnauthorizedException("Invalid refresh token");
        }
    }

    public UUID verifyPreAuthorizationTokenAndExtractUserId(String preAuthorizationToken) {
        return UUID.fromString(
                Jwts.parser()
                        .verifyWith(Keys.hmacShaKeyFor(preAuthorizationSecret.getBytes()))
                        .build()
                        .parseSignedClaims(preAuthorizationToken)
                        .getPayload()
                        .getSubject()
        );
    }


    public UUID extractUserIdFromContext() throws UnauthorizedException {
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        HttpServletRequest req;
        if (requestAttributes instanceof ServletRequestAttributes) {
            req = ((ServletRequestAttributes) requestAttributes).getRequest();
        } else
            throw new UnauthorizedException("Invalid access and refresh tokens");


        if (req.getCookies() == null) {
            throw new UnauthorizedException("No provided access and refresh tokens");
        }
        Cookie[] cookies = req.getCookies();
        TokenPair tokens = new TokenPair();
        for (Cookie cookie : cookies) {
            if (cookie.getName().equals("__access_tkn")) tokens.setAccessToken(cookie.getValue());
            if (cookie.getName().equals("__refresh_tkn")) tokens.setRefreshToken(cookie.getValue());
        }
        if (tokens.getAccessToken() == null) throw new UnauthorizedException("No provided access token");
        if (tokens.getRefreshToken() == null) throw new UnauthorizedException("No provided refresh token");
        try {
            verifyAccessToken(tokens.getAccessToken());
            return extractUserIdFromAccessToken(tokens.getAccessToken());
        } catch (ExpiredJwtException e) {
            return extractUserIdFromRefreshToken(tokens.getRefreshToken());
        }


    }

}
