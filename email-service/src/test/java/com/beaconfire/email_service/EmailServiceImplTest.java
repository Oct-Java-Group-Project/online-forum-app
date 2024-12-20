package com.beaconfire.email_service;

import com.beaconfire.email_service.Service.impl.EmailServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import javax.mail.Message;
import javax.mail.Transport;

import static org.mockito.Mockito.*;

public class EmailServiceImplTest {

    private EmailServiceImpl emailService;

    @BeforeEach
    public void setUp() {
        emailService = new EmailServiceImpl();
        emailService.smtpHost = "smtp.example.com";
        emailService.smtpEmail = "test@example.com";
        emailService.smtpPassword = "password";
        emailService.fromEmailAddress = "noreply@example.com";
    }

    @Test
    public void testSendEmail() throws Exception {
        // 模拟 Transport 类的静态方法
        try (MockedStatic<Transport> transportMock = mockStatic(Transport.class)) {
            String recipientEmail = "recipient@example.com";
            String firstname = "John";
            String lastname = "Doe";
            String url = "http://example.com/verify";

            // 调用方法
            emailService.sendEmail(recipientEmail, firstname, lastname, url);

            // 验证 Transport.send 方法是否被调用
            transportMock.verify(() -> Transport.send(any(Message.class)), times(1));
        }
    }
}
