package uce.proyect.service.agreementImp;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import lombok.extern.log4j.Log4j2;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uce.proyect.exceptions.handleException.JwtException;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

@Slf4j
@Service
public class JwtService {

    private static final String BEARER = "Bearer ";
    private static final String USER = "user";
    private static final String ROLES = "roles";
    private static final String ISSUER = "sgvacunas";
    private static final int EXPIRES_IN_MILLISECOND = 360000;
    private static final String SECRET = "clave-secreta-test"; // test

    public String createToken(String user, List<String> roles) {
        log.warn(new Date(System.currentTimeMillis() + EXPIRES_IN_MILLISECOND).toString());
        return JWT.create()
                .withIssuer(ISSUER)
                .withIssuedAt(new Date())
                .withNotBefore(new Date())
                .withExpiresAt(new Date(System.currentTimeMillis() + EXPIRES_IN_MILLISECOND))
                .withClaim(USER, user)
                .withArrayClaim(ROLES, roles.toArray(new String[0]))
                .sign(Algorithm.HMAC256(SECRET));

    }

    public boolean isBearer(String authorization) {
        return authorization != null
                && authorization.startsWith(BEARER)
                && authorization.split("\\.").length == 3;
    }

    public String user (String authorization) throws JwtException {
        return this.verify(authorization).getClaim(USER).asString();
    }

    private DecodedJWT verify(String authorization) throws JwtException {
        if (!this.isBearer(authorization)) {
            throw new JwtException("It is not Berear");
        }
        try {
            return JWT.require(Algorithm.HMAC256(SECRET))
                    .withIssuer(ISSUER).build()
                    .verify(authorization.substring(BEARER.length()));
        } catch (Exception exception){
            throw new JwtException("JWT is wrong. " + exception.getMessage());
        }
    }

    public List<String> roles(String authorization) throws JwtException {
        return Arrays.asList(this.verify(authorization).getClaim(ROLES).asArray(String.class));
    }

}