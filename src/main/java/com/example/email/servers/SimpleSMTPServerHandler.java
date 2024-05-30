package com.example.email.servers;

import com.example.email.entity.MailEntity;
import com.example.email.repository.MailRepository;

import java.io.*;
import java.net.Socket;
import java.time.LocalDateTime;

public class SimpleSMTPServerHandler extends Thread {
    private Socket socket;
    private MailRepository mailRepository;

    public SimpleSMTPServerHandler(Socket socket, MailRepository mailRepository) {
        this.socket = socket;
        this.mailRepository = mailRepository;
    }

    @Override
    public void run() {
        try (InputStream input = socket.getInputStream();
             BufferedReader reader = new BufferedReader(new InputStreamReader(input));
             OutputStream output = socket.getOutputStream();
             PrintWriter writer = new PrintWriter(output, true)) {

            writer.println("220 Simple SMTP Server");

            String sender = null;
            String recipient = null;
            String subject = null;
            StringBuilder data = new StringBuilder();

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("HELO") || line.startsWith("EHLO")) {
                    writer.println("250 Hello " + line.substring(5));
                } else if (line.startsWith("MAIL FROM:")) {
                    sender = line.substring(10).trim();
                    writer.println("250 OK");
                } else if (line.startsWith("RCPT TO:")) {
                    recipient = line.substring(8).trim();
                    writer.println("250 OK");
                } else if (line.startsWith("DATA")) {
                    writer.println("354 End data with <CR><LF>.<CR><LF>");
                } else if (line.equals(".")) {
                    writer.println("250 OK: Message accepted for delivery");
                    if (sender != null && recipient != null) {
                        MailEntity mail = new MailEntity();
                        mail.setFromEmail(sender);
                        mail.setToEmail(recipient); // 使用新的收件人字段
                        mail.setSubject(subject);
                        mail.setBody(data.toString());
                        mail.setSentDate(LocalDateTime.now());
                        mail.setIsRead(false); // 初始设置为未读
                        mailRepository.save(mail);
                        System.out.println("Saved email to database.");
                    }
                } else if (line.startsWith("QUIT")) {
                    writer.println("221 Bye");
                    break;
                } else if (line.startsWith("Subject:")) {
                    subject = line.substring(8).trim();
                } else {
                    data.append(line).append("\n");
                }
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}