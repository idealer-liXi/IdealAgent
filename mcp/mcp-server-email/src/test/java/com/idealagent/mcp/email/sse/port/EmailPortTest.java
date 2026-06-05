package com.idealagent.mcp.email.sse.port;

import com.idealagent.mcp.email.credential.EmailCredential;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.lang.reflect.Method;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

class EmailPortTest {

    @Test
    void buildMailSenderUsesImplicitSslForSmtp465() throws Exception {
        JavaMailSenderImpl sender = buildMailSender(465);

        Properties props = sender.getJavaMailProperties();
        assertThat(props.getProperty("mail.smtp.ssl.enable")).isEqualTo("true");
        assertThat(props.getProperty("mail.smtp.starttls.enable")).isEqualTo("false");
        assertThat(props.getProperty("mail.smtp.starttls.required")).isEqualTo("false");
    }

    @Test
    void buildMailSenderUsesStartTlsForSmtp587() throws Exception {
        JavaMailSenderImpl sender = buildMailSender(587);

        Properties props = sender.getJavaMailProperties();
        assertThat(props.getProperty("mail.smtp.ssl.enable")).isEqualTo("false");
        assertThat(props.getProperty("mail.smtp.starttls.enable")).isEqualTo("true");
        assertThat(props.getProperty("mail.smtp.starttls.required")).isEqualTo("true");
    }

    private JavaMailSenderImpl buildMailSender(int smtpPort) throws Exception {
        EmailCredential credential = EmailCredential.builder()
                .smtpHost("smtp.qq.com")
                .smtpPort(smtpPort)
                .smtpUsername("sender@qq.com")
                .smtpPassword("auth-code")
                .fromAddress("sender@qq.com")
                .fromName("IdealAgent")
                .userId("7")
                .build();
        EmailPort port = new EmailPort();
        Method method = EmailPort.class.getDeclaredMethod("buildMailSender", EmailCredential.class);
        method.setAccessible(true);
        return (JavaMailSenderImpl) method.invoke(port, credential);
    }
}
