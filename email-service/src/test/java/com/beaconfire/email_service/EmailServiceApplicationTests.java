package com.beaconfire.email_service;

import com.beaconfire.email_service.Service.EmailService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.context.SpringBootTest;

import static org.mockito.Mockito.*;

@SpringBootTest
class EmailServiceApplicationTest {

	@Mock
	private EmailService emailService;

	@InjectMocks
	private EmailServiceApplication emailServiceApplication;

	@Test
	public void testHandleEmailMessage() throws Exception {

		MockitoAnnotations.openMocks(this);

		String message = """
                {
                    "email": "test@example.com",
                    "firstName": "John",
                    "lastName": "Doe",
                    "url": "http://example.com/verify"
                }
                """;

		emailServiceApplication.handleEmailMessage(message);

		verify(emailService, times(1)).sendEmail(
				"test@example.com",
				"John",
				"Doe",
				"http://example.com/verify"
		);
	}
}
