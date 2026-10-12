package com.inventrack.inventrack.service;

import java.time.Duration;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import com.inventrack.inventrack.entity.Usuario;
import lombok.RequiredArgsConstructor;

/** Genera el token JWT (HS256) que devuelve el login. */
@Service
@RequiredArgsConstructor
public class JwtService {

    private final JwtEncoder encoder;

    @Value("${jwt.expiracion-horas}")
    private long horas;

    public String generar(Usuario u) {
        Instant ahora = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
            .issuer("inventrack")
            .subject(String.valueOf(u.getId()))
            .issuedAt(ahora)
            .expiresAt(ahora.plus(Duration.ofHours(horas)))
            .claim("nombre", u.getNombre())
            .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
}
