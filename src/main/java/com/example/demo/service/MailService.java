package com.example.demo.service;

import java.util.Base64;

import org.springframework.stereotype.Service;

import com.sendgrid.*;
import com.sendgrid.SendGrid;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.Method;

import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Email;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Attachments;
@Service
public class MailService {

    private final String FROM_EMAIL = "calendarheatmap@gmail.com";

    // 📩 Send heatmap with attachment
    public void sendHeatmapMail(String to, String subject, String body, byte[] imageBytes) {

        try {
            Email from = new Email(FROM_EMAIL);
            Email toEmail = new Email(to);

            Content content = new Content("text/plain", body);
            Mail mail = new Mail(from, subject, toEmail, content);

            // attachment
            Attachments attachment = new Attachments();
            attachment.setContent(Base64.getEncoder().encodeToString(imageBytes));
            attachment.setType("image/png");
            attachment.setFilename("heatmap.png");
            attachment.setDisposition("attachment");

            mail.addAttachments(attachment);

            SendGrid sg = new SendGrid(System.getenv("SENDGRID_API_KEY"));

            Request request = new Request();
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());

            Response response = sg.api(request);

            System.out.println("Mail sent: " + response.getStatusCode());

        } catch (Exception e) {
            System.out.println("SendGrid error: " + e.getMessage());
        }
    }

    // 📩 Simple text mail
    public void sendTextMail(String to, String subject, String body) {

        try {
            Email from = new Email(FROM_EMAIL);
            Email toEmail = new Email(to);

            Content content = new Content("text/plain", body);
            Mail mail = new Mail(from, subject, toEmail, content);

            SendGrid sg = new SendGrid(System.getenv("SENDGRID_API_KEY"));

            Request request = new Request();
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());

            sg.api(request);

        } catch (Exception e) {
            System.out.println("SendGrid error: " + e.getMessage());
        }
    }
}
