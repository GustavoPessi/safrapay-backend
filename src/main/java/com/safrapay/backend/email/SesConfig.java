package com.safrapay.backend.email;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ses.SesClient;

@Configuration
public class SesConfig {

    // As credenciais da AWS (access key / secret key) NAO ficam aqui: o SDK
    // busca automaticamente nas variaveis de ambiente AWS_ACCESS_KEY_ID e
    // AWS_SECRET_ACCESS_KEY (default credentials provider chain).
    @Bean
    public SesClient sesClient(@Value("${aws.ses.region}") String regiao) {
        return SesClient.builder()
                .region(Region.of(regiao))
                .build();
    }
}
