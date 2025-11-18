package com.nacrondx.suitesync.service;

import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {
  @Value("${app.mail.from}")
  private String fromEmail;

  @Value("${app.base-url}")
  private String baseUrl;

  @Value("${app.mail.api-key}")
  private String apiKey;

  public void sendConfirmationEmail(String email, String firstName, Long userId, String token) {
    var from = new Email(fromEmail);
    var subject = "Welcome to Suite Sync - Confirm Your Account";
    var to = new Email(email);
    var activationLink = baseUrl + "/api/v1/users/" + userId + "/activate?token=" + token;
    var htmlContent =
        """
          <!DOCTYPE html>
          <html>
          <head>
              <meta charset="UTF-8">
              <style>
                  body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                  .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                  .header { background-color: #4CAF50; color: white; padding: 20px; text-align: center; }
                  .content { padding: 20px; background-color: #f9f9f9; }
                  .button { display: inline-block; padding: 12px 24px; background-color: #4CAF50;
                            color: white; text-decoration: none; border-radius: 4px; margin: 20px 0; }
                  .footer { text-align: center; padding: 20px; font-size: 12px; color: #666; }
              </style>
          </head>
          <body>
              <div class="container">
                  <div class="header">
                      <h1>Welcome to Suite Sync!</h1>
                  </div>
                  <div class="content">
                      <p>Hello %s,</p>
                      <p>Thank you for registering with Suite Sync. To complete your registration and activate your account, please click the button below:</p>
                      <div style="text-align: center;">
                          <a href="%s" class="button">Activate Your Account</a>
                      </div>
                      <p>If the button doesn't work, you can copy and paste this link into your browser:</p>
                      <p style="word-break: break-all; color: #4CAF50;">%s</p>
                      <p>This activation link will expire in 24 hours.</p>
                      <p>If you didn't create an account with Suite Sync, please ignore this email.</p>
                  </div>
                  <div class="footer">
                      <p>&copy; 2025 Suite Sync. All rights reserved.</p>
                      <p>This is an automated message, please do not reply to this email.</p>
                  </div>
              </div>
          </body>
          </html>
          """
            .formatted(firstName, activationLink, activationLink);
    var content = new Content("text/html", htmlContent);
    var mail = new Mail(from, subject, to, content);

    var sg = new SendGrid(apiKey);
    sg.setDataResidency("eu");
    var request = new Request();

    try {
      request.setMethod(Method.POST);
      request.setEndpoint("mail/send");
      request.setBody(mail.build());
      var response = sg.api(request);
      log.info(String.valueOf(response.getStatusCode()));
      log.info(response.getBody());
      System.out.println(response.getHeaders());
      log.info("Confirmation email sent successfully to: {}", email);

    } catch (IOException e) {
      log.error("Failed to send confirmation email to: {}", email, e);
      throw new RuntimeException("Failed to send confirmation email", e);
    }
  }
}
