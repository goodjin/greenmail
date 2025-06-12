/*
 * Copyright (c) 2023 Wael Chatila / Icegreen Technologies. All Rights Reserved.
 * This software is released under the Apache license 2.0
 */
package com.icegreen.greenmail.smtp;

import com.icegreen.greenmail.Managers;
import com.icegreen.greenmail.user.GreenMailUser;
import com.icegreen.greenmail.user.UserException;
import com.icegreen.greenmail.util.GreenMailUtil;
import com.icegreen.greenmail.util.ServerSetup;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.icegreen.greenmail.util.GreenMailUtil.createTextEmail;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

public class NioSmtpServerTest {

    private NioSmtpServer nioSmtpServer;
    private Managers managers;
    private ServerSetup smtpSetup;

    @BeforeEach
    void setUp() {
        managers = new Managers();
        // Use a dynamic port for testing to avoid conflicts
        smtpSetup = new ServerSetup(0, ServerSetup.getLocalHostAddress(), ServerSetup.PROTOCOL_SMTP);
        nioSmtpServer = new NioSmtpServer(smtpSetup, managers);
        nioSmtpServer.startService();
        // Update smtpSetup with the actual port being used by the server
        smtpSetup = nioSmtpServer.getServerSetup();
    }

    @AfterEach
    void tearDown() {
        if (nioSmtpServer != null) {
            nioSmtpServer.stopService();
        }
    }

    private boolean waitForIncomingEmail(long timeout, int emailCount) {
        return managers.getImapHostManager().getInbox(GreenMailUser.getNonExistentUser()).waitForMessages(emailCount, timeout);
    }

    private MimeMessage[] getReceivedMessages() {
        return managers.getImapHostManager().getAllMessages();
    }

    private GreenMailUser setUser(String login, String password) {
        try {
            return managers.getUserManager().createUser(login, login, password);
        } catch (UserException e) {
            return managers.getUserManager().getUser(login);
        }
    }

    @Test
    void testSmtpServerBasic() throws MessagingException, IOException {
        GreenMailUtil.sendTextEmail("to@localhost", "from@localhost", "subject", "body", smtpSetup);
        assertThat(waitForIncomingEmail(2000, 1)).isTrue();
        MimeMessage[] emails = getReceivedMessages();
        assertThat(emails).hasSize(1);
        assertThat(emails[0].getSubject()).isEqualTo("subject");
        assertThat(emails[0].getContent().toString().trim()).isEqualTo("body"); // getContent might add newlines
    }

    @Test
    void testSmtpServerReceiveMultipart() throws Exception {
        assertThat(getReceivedMessages()).isEmpty();

        String subject = GreenMailUtil.random();
        String body = GreenMailUtil.random();
        byte[] attachmentData = {0, 1, 2, 3, 4, 5};
        GreenMailUtil.sendAttachmentEmail("test@localhost", "from@localhost", subject, body,
            attachmentData, "application/octet-stream", "testfile.dat", "test file", smtpSetup);

        assertThat(waitForIncomingEmail(2000, 1)).isTrue();
        Message[] emails = getReceivedMessages();
        assertThat(emails).hasSize(1);
        assertThat(emails[0].getSubject()).isEqualTo(subject);

        Object content = emails[0].getContent();
        assertThat(content).isInstanceOf(MimeMultipart.class);
        MimeMultipart mp = (MimeMultipart) content;
        assertThat(mp.getCount()).isEqualTo(2); // Body and attachment

        // Part 0 should be the body
        assertThat(GreenMailUtil.getBody(mp.getBodyPart(0)).trim()).isEqualTo(body);

        // Part 1 should be the attachment
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        GreenMailUtil.copyStream(mp.getBodyPart(1).getInputStream(), bout);
        assertThat(bout.toByteArray()).isEqualTo(attachmentData);
    }

    @Test
    void testSmtpServerLeadingPeriods() throws MessagingException, IOException {
        String body = ". body with leading period\r\n.. second line";
        GreenMailUtil.sendTextEmail("to@localhost", "from@localhost", "subject", body, smtpSetup);
        assertThat(waitForIncomingEmail(2000, 1)).isTrue();
        MimeMessage[] emails = getReceivedMessages();
        assertThat(emails).hasSize(1);
        assertThat(emails[0].getSubject()).isEqualTo("subject");
        // SmtpManager/RFC5321 adds a CRLF if missing, and dotLimitedInputStream removes escaping.
        // Also, GreenMailUtil.sendTextEmail might use "text/plain; charset=us-ascii"
        // which can affect how content is retrieved if not perfectly aligned.
        String receivedContent = GreenMailUtil.getBody(emails[0].getPart(0)).trim();
        String expectedContent = body.replace("\r\n..", "\r\n."); // Expected transformation by server
        assertThat(receivedContent).isEqualTo(expectedContent);
    }

    @Test
    void testSendAndWaitForIncomingMailsInBcc() throws Throwable {
        String subject = GreenMailUtil.random();
        String body = GreenMailUtil.random();
        final MimeMessage message = createTextEmail("test@localhost", "from@localhost", subject, body, smtpSetup);
        message.addRecipients(Message.RecipientType.BCC, "bcc1@localhost,bcc2@localhost");

        assertThat(getReceivedMessages()).isEmpty();

        GreenMailUtil.sendMimeMessage(message);

        assertThat(waitForIncomingEmail(2000, 3)).isTrue(); // To, BCC1, BCC2

        MimeMessage[] emails = getReceivedMessages();
        assertThat(emails).hasSize(3);
    }

    @Test
    void testAuth() throws Throwable {
        assertThat(getReceivedMessages()).isEmpty();
        setUser("foo", "bar");

        String subject = GreenMailUtil.random();
        String body = GreenMailUtil.random();

        Properties props = smtpSetup.configureJavaMailSessionProperties(null, false);
        Session session = Session.getInstance(props);

        MimeMessage message = GreenMailUtil.createTextEmail("test@localhost", "from@localhost", subject, body, session);

        // Try without auth - should succeed if server doesn't require auth by default
        Transport.send(message);
        assertThat(waitForIncomingEmail(2000,1)).isTrue();

        // Try with correct auth
        Transport.send(message, "foo", "bar");
        assertThat(waitForIncomingEmail(5000, 2)).isTrue(); // Wait for the second message (total of 2)

        // Try with incorrect auth
        try {
            Transport.send(message, "foo", "wrongpassword");
            fail("Authentication should have failed");
        } catch (MessagingException e) {
            assertThat(e).isInstanceOf(jakarta.mail.AuthenticationFailedException.class);
        }

        // Ensure only two messages were actually sent and received
        MimeMessage[] emails = getReceivedMessages();
        assertThat(emails).hasSize(2);
        for (MimeMessage receivedMsg : emails) {
            assertThat(receivedMsg.getSubject()).isEqualTo(subject);
            assertThat(receivedMsg.getContent().toString().trim()).isEqualTo(body);
        }
    }

    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void testConcurrentSend() throws InterruptedException, MessagingException {
        int numberOfClients = 5;
        int emailsPerClient = 2;
        int totalEmails = numberOfClients * emailsPerClient;
        CountDownLatch latch = new CountDownLatch(totalEmails);
        ExecutorService executor = Executors.newFixedThreadPool(numberOfClients);

        for (int i = 0; i < numberOfClients; i++) {
            final int clientId = i;
            executor.submit(() -> {
                try {
                    for (int j = 0; j < emailsPerClient; j++) {
                        String subject = "Concurrent Test Client " + clientId + " Email " + j;
                        String body = "Body for email " + j + " from client " + clientId;
                        String to = "to" + clientId + "_" + j + "@localhost";
                        String from = "from" + clientId + "@localhost";
                        GreenMailUtil.sendTextEmail(to, from, subject, body, smtpSetup);
                        latch.countDown();
                    }
                } catch (MessagingException e) {
                    e.printStackTrace();
                }
            });
        }

        latch.await(); // Wait for all emails to be sent
        assertThat(waitForIncomingEmail(8000, totalEmails)).as("All emails should be received").isTrue();

        MimeMessage[] receivedMessages = getReceivedMessages();
        assertThat(receivedMessages).hasSize(totalEmails);

        // Optional: Verify subjects or content if needed, though size check is primary for concurrency
        List<String> subjects = new ArrayList<>();
        for(MimeMessage msg : receivedMessages) {
            subjects.add(msg.getSubject());
        }
        for (int i = 0; i < numberOfClients; i++) {
            for (int j = 0; j < emailsPerClient; j++) {
                assertThat(subjects).contains("Concurrent Test Client " + i + " Email " + j);
            }
        }

        executor.shutdown();
        if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
            executor.shutdownNow();
        }
    }
}
