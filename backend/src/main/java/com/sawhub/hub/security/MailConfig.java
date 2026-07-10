package com.sawhub.hub.security;

import java.util.Properties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

/** H1.4 (M18) — bean construído na mão a partir de {@code EmailProperties}, não pela
 * autoconfiguração do Spring Boot (que dispara em cima de {@code spring.mail.host}, uma condição
 * ambígua quando a propriedade existe mas está vazia). Mesmo espírito de "construir explícito em
 * vez de depender de autoconfig implícita" já usado no {@code RestClient} do
 * {@code MercadoPagoGatewayService} (M14). Retorna {@code null} quando não há credencial —
 * {@code EmailService} injeta o bean como opcional e trata esse caso. */
@Configuration
public class MailConfig {

    @Bean
    public JavaMailSender javaMailSender(EmailProperties emailProperties) {
        if (!emailProperties.isEnabled()) {
            return null;
        }
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(emailProperties.getHost());
        sender.setPort(emailProperties.getPort());
        sender.setUsername(emailProperties.getUsername());
        sender.setPassword(emailProperties.getPassword());

        Properties props = sender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        return sender;
    }
}
