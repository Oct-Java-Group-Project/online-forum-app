package com.beaconfire.email_service.Service.impl;

import com.beaconfire.email_service.Service.EmailService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Properties;

@Service
public class EmailServiceImpl implements EmailService {

    @Value("${email.smtp.host}")
    public String smtpHost;

    @Value("${email.smtp.email}")
    public String smtpEmail;

    @Value("${email.smtp.password}")
    public String smtpPassword;

    @Value("${email.smtp.from.address}")
    public String fromEmailAddress;

    @Override
    public void sendEmail(String recipientEmail, String firstname, String lastname, String url) throws MessagingException {
        Properties properties = new Properties();
        properties.put("mail.smtp.auth", "true");
        properties.put("mail.smtp.starttls.enable", "true");
        properties.put("mail.smtp.host", smtpHost);
        properties.put("mail.smtp.port", "587");

        Session session = Session.getInstance(properties, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(smtpEmail, smtpPassword);
            }
        });

        Message message = new MimeMessage(session);
        message.setFrom(new InternetAddress(fromEmailAddress));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipientEmail));
        message.setSubject("Welcome to Our Service");

        String htmlContent = "<html>" +
                "<body>" +
                "<p>Dear " + firstname + " " + lastname + ",</p>" +
                "<p>Thank you for registering with us! Your token is:</p>" +
                "<p><a href='" + url + "'>" + url + "</a></p>" +
                "</body>" +
                "</html>";

        message.setContent(htmlContent, "text/html; charset=UTF-8");

        Transport.send(message);
        System.out.println("Email sent successfully to: " + recipientEmail);
    }
}
