package com.icegreen.greenmail;

import com.icegreen.greenmail.smtp.NioSmtpServer;
import com.icegreen.greenmail.smtp.SmtpServer;
import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetup;
import com.icegreen.greenmail.util.Service;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;


public class GreenMailCoreTest {

    @Test
    void testStartsBioSmtpServerByDefault() {
        // Default ServerSetup for SMTP uses a dynamic port
        ServerSetup smtpSetup = ServerSetup.SMTP.dynamicPort();
        GreenMail greenMail = new GreenMail(smtpSetup);
        try {
            greenMail.start();
            Service smtpService = greenMail.getSmtp();
            assertThat(smtpService).isNotNull();
            assertThat(smtpService).isInstanceOf(SmtpServer.class);
            assertThat(greenMail.isRunning()).isTrue();
        } finally {
            greenMail.stop();
            assertThat(greenMail.isRunning()).isFalse();
        }
    }

    @Test
    void testStartsBioSmtpServerExplicitly() {
        ServerSetup smtpSetup = ServerSetup.SMTP.dynamicPort()
                .smtpImplementation(ServerSetup.SmtpServerImplementation.BIO);
        GreenMail greenMail = new GreenMail(smtpSetup);
        try {
            greenMail.start();
            Service smtpService = greenMail.getSmtp();
            assertThat(smtpService).isNotNull();
            assertThat(smtpService).isInstanceOf(SmtpServer.class);
            assertThat(greenMail.isRunning()).isTrue();
        } finally {
            greenMail.stop();
        }
    }

    @Test
    void testStartsNioSmtpServerWhenConfigured() {
        ServerSetup smtpSetup = ServerSetup.SMTP.dynamicPort()
                .smtpImplementation(ServerSetup.SmtpServerImplementation.NIO);
        GreenMail greenMail = new GreenMail(smtpSetup);
        try {
            greenMail.start();
            Service smtpService = greenMail.getSmtp();
            assertThat(smtpService).isNotNull();
            assertThat(smtpService).isInstanceOf(NioSmtpServer.class);
            assertThat(greenMail.isRunning()).isTrue();
        } finally {
            greenMail.stop();
        }
    }

    @Test
    void testStartsBioSmtpsServerByDefault() {
        ServerSetup smtpsSetup = ServerSetup.SMTPS.dynamicPort();
        GreenMail greenMail = new GreenMail(smtpsSetup);
        try {
            greenMail.start();
            Service smtpsService = greenMail.getSmtps();
            assertThat(smtpsService).isNotNull();
            assertThat(smtpsService).isInstanceOf(SmtpServer.class);
            assertThat(greenMail.isRunning()).isTrue();
        } finally {
            greenMail.stop();
        }
    }

    @Test
    void testStartsNioSmtpsServerWhenConfigured() {
        ServerSetup smtpsSetup = ServerSetup.SMTPS.dynamicPort()
                .smtpImplementation(ServerSetup.SmtpServerImplementation.NIO);
        GreenMail greenMail = new GreenMail(smtpsSetup);
        try {
            greenMail.start();
            Service smtpsService = greenMail.getSmtps();
            assertThat(smtpsService).isNotNull();
            assertThat(smtpsService).isInstanceOf(NioSmtpServer.class);
            assertThat(greenMail.isRunning()).isTrue();
        } finally {
            greenMail.stop();
        }
    }

    @Test
    void testNioOptionOnNonSmtpProtocolThrowsErrorInServerSetup() {
        final ServerSetup imapSetup = ServerSetup.IMAP.dynamicPort();
        Executable executable = () -> imapSetup.smtpImplementation(ServerSetup.SmtpServerImplementation.NIO);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, executable);
        assertThat(exception.getMessage()).isEqualTo("NIO implementation is only applicable to SMTP/S protocols.");
    }

    @Test
    void testBioOptionOnNonSmtpProtocolIsFineAndIgnored() {
        ServerSetup imapSetup = ServerSetup.IMAP.dynamicPort()
            .smtpImplementation(ServerSetup.SmtpServerImplementation.BIO); // This should be fine

        GreenMail greenMail = new GreenMail(imapSetup);
        try {
            greenMail.start();
            assertThat(greenMail.getImap()).isNotNull();
            // Check that it's not accidentally an SMTPServer or NioSmtpServer
            assertThat(greenMail.getImap()).isNotInstanceOf(SmtpServer.class);
            assertThat(greenMail.getImap()).isNotInstanceOf(NioSmtpServer.class);
            assertThat(greenMail.isRunning()).isTrue();
        } finally {
            greenMail.stop();
        }
    }

    @Test
    void testMixedServerSetupsBioAndNio() {
        ServerSetup smtpBio = ServerSetup.SMTP.dynamicPort()
                                .smtpImplementation(ServerSetup.SmtpServerImplementation.BIO);
        ServerSetup smtpsNio = ServerSetup.SMTPS.dynamicPort()
                                .smtpImplementation(ServerSetup.SmtpServerImplementation.NIO);
        ServerSetup imap = ServerSetup.IMAP.dynamicPort();

        GreenMail greenMail = new GreenMail(new ServerSetup[]{smtpBio, smtpsNio, imap});
        try {
            greenMail.start();
            assertThat(greenMail.getSmtp()).isInstanceOf(SmtpServer.class);
            assertThat(greenMail.getSmtps()).isInstanceOf(NioSmtpServer.class);
            assertThat(greenMail.getImap()).isNotNull();
            assertThat(greenMail.isRunning()).isTrue();
        } finally {
            greenMail.stop();
        }
    }
}
