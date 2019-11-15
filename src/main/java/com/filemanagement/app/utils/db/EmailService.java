package com.filemanagement.app.utils.db;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import java.io.File;
import java.util.Properties;
/**
 * @author Jaouad El Aoud
 */
class EmailService {

    private String host = "";
    private int port = 0;
    private String fromAdd = "";
    private String toAdd = "";
    private String username = "";
    private String password = "";
    private String subject = "";
    private String msg = "";
    private File [] attachments;



    private EmailService() {}

    static EmailService builder() {
        return new EmailService();
    }

    EmailService setHost(String host){
        this.host = host;
        return this;
    }

    EmailService setPort(int port) {
        this.port = port;
        return this;
    }

    EmailService setFromAddress(String fromAdd) {
        this.fromAdd = fromAdd;
        return this;
    }

    EmailService setToAddress(String toAdd) {
        this.toAdd = toAdd;
        return  this;
    }

    EmailService setUsername(String username) {
        this.username = username;
        return this;
    }

    EmailService setPassword(String password) {
        this.password = password;
        return this;
    }

    EmailService setSubject(String subject) {
        this.subject = subject;
        return this;
    }

    EmailService setMessage(String message) {
        this.msg = message;
        return this;
    }

    EmailService setAttachments(File[] files) {
        this.attachments = files;
        return this;
    }

    private boolean isPropertiesSet() {
        return !this.host.isEmpty() &&
                this.port > 0 &&
                !this.username.isEmpty() &&
                !this.password.isEmpty() &&
                !this.toAdd.isEmpty() &&
                !this.fromAdd.isEmpty() &&
                !this.subject.isEmpty() &&
                !this.msg.isEmpty() &&
                this.attachments != null && this.attachments.length > 0;
    }


    boolean sendMail(boolean withAttachments) {

        if(!this.isPropertiesSet()) {
            return false;
        }

        Properties prop = new Properties();
        prop.put("mail.smtp.auth", true);
        prop.put("mail.smtp.starttls.enable", "true");
        prop.put("mail.smtp.host", this.host);
        prop.put("mail.smtp.port", this.port);
        prop.put("mail.smtp.ssl.trust", host);



        Session session = Session.getInstance(prop, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        });



        try {
//			create a default mime message object
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(fromAdd));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toAdd));
            message.setSubject(subject);

//          body part for message
            MimeBodyPart mimeBodyPart = new MimeBodyPart();
            mimeBodyPart.setContent(msg, "text/html");

//          body part for attachments


//          create a multipart to combine them together
            Multipart multipart = new MimeMultipart();
            multipart.addBodyPart(mimeBodyPart);
            if (withAttachments){
                MimeBodyPart attachmentBodyPart = new MimeBodyPart();
                for (File file: this.attachments) {
                    attachmentBodyPart.attachFile(file);
                }
                multipart.addBodyPart(attachmentBodyPart);
            }

            //now set the multipart as the content of the message
            message.setContent(multipart);

//			send the message
            Transport.send(message);


            return true;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

    }

}
