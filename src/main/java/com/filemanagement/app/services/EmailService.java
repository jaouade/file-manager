package com.filemanagement.app.services;

import lombok.Builder;
import lombok.Data;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import java.io.File;
import java.io.IOException;
import java.util.Properties;

/**
 * @author Jaouad El Aoud
 */
@Builder
@Data
@Component
public class EmailService {

    private final Environment environment;

    public EmailService(Environment environment) {
        this.environment = environment;
    }


    public void send(String subject,String msg,File...attachments) throws MessagingException, IOException {


        Properties props = new Properties();
        props.put("mail.smtp.auth", environment.getProperty("mail.smtp.auth","true"));
        props.put("mail.smtp.starttls.enable", environment.getProperty("mail.smtp.starttls.enable","true"));
        props.put("mail.smtp.host", environment.getProperty("mail.smtp.server",""));
        props.put("mail.smtp.port", environment.getProperty("mail.smtp.port",""));
        props.put("mail.smtp.ssl.trust", environment.getProperty("mail.smtp.ssl.trust","true"));


        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(environment.getProperty("mail.smtp.user",""), environment.getProperty("mail.smtp.password",""));
            }
        });

//			create a default mime message object
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(environment.getProperty("mail.smtp.from","")));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(environment.getProperty("mail.smtp.to","")));
            message.setSubject(subject);

//          body part for message
            MimeBodyPart mimeBodyPart = new MimeBodyPart();
            mimeBodyPart.setContent(msg, "text/html");

//          body part for attachments


//          create a multipart to combine them together
            Multipart multipart = new MimeMultipart();
            multipart.addBodyPart(mimeBodyPart);
            if (attachments!=null && attachments.length>0) {
                MimeBodyPart attachmentBodyPart = new MimeBodyPart();
                for (File file : attachments) {
                    attachmentBodyPart.attachFile(file);
                }
                multipart.addBodyPart(attachmentBodyPart);
            }


            //now set the multipart as the content of the message
            message.setContent(multipart);

//			send the message
            Transport.send(message);



    }


}
