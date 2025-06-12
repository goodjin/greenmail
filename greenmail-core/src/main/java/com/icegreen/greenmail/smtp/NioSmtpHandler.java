/*
 * Copyright (c) 2014 Wael Chatila / Icegreen Technologies. All Rights Reserved.
 * This software is released under the Apache license 2.0
 */
package com.icegreen.greenmail.smtp;

import com.icegreen.greenmail.server.AbstractSocketProtocolHandler;
import com.icegreen.greenmail.server.BuildInfo;
import com.icegreen.greenmail.smtp.commands.SmtpCommand;
import com.icegreen.greenmail.smtp.commands.SmtpCommandRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.net.SocketTimeoutException; // May not be directly applicable with NIO selector model

public class NioSmtpHandler implements Runnable { // Changed from AbstractSocketProtocolHandler
    private static final Logger log = LoggerFactory.getLogger(NioSmtpHandler.class);

    public static final int LINE_LENGHT_LIMIT = 4096;

    protected SmtpCommandRegistry registry;
    protected SmtpManager manager;
    protected SocketChannel socketChannel; // Use SocketChannel
    protected NioSmtpConnection conn;
    protected SmtpState state;
    protected String currentLine;
    private volatile boolean quitting = false;
    private final NioSmtpServer server; // Reference to the server for callbacks, e.g. shutdown

    public NioSmtpHandler(NioSmtpServer server, SmtpCommandRegistry registry,
                          SmtpManager manager, SocketChannel socketChannel) {
        this.server = server;
        this.registry = registry;
        this.manager = manager;
        this.socketChannel = socketChannel;
    }

    @Override
    public void run() {
        try {
            // Note: SmtpHandler passed to NioSmtpConnection is 'this' (NioSmtpHandler)
            // NioSmtpConnection expects SmtpHandler, so we might need to adjust types or interfaces if strict typing is an issue.
            // For now, assuming NioSmtpConnection's handler field can accept this NioSmtpHandler.
            conn = new NioSmtpConnection((SmtpHandler) (Object) this, socketChannel); // Ugly cast, indicates potential refactor needed
            state = new SmtpState();

            sendGreetings();

            // In a true NIO model, this loop would be different, driven by selector events.
            // For now, keeping it similar to the blocking model for a start.
            // The actual readLine might block if not careful with channel configuration.
            while (!isQuitting() && socketChannel.isOpen()) {
                handleCommand();
            }
        } catch (SocketTimeoutException ste) { // This might be less relevant in a pure NIO selector based model
            if (conn != null) {
                conn.send("421 Service shutting down and closing transmission channel (socket timeout)");
            }
            close();
        } catch (IOException e) {
            if (!isQuitting()) {
                log.error("Unexpected error handling connection for {} ", socketChannel.socket().getRemoteSocketAddress(), e);
            }
        } catch (Exception e) {
            if (!isQuitting()) {
                log.error("Unexpected error handling connection for {} ", socketChannel.socket().getRemoteSocketAddress(), e);
                throw new IllegalStateException("Unexpected error handling connection", e);
            }
        } finally {
            if (null != state) {
                state.clearMessage();
            }
            close(); // Ensure resources are cleaned up
        }
    }

    protected void sendGreetings() {
        conn.send("220 " + conn.getServerGreetingsName() +
            " GreenMail NIO SMTP Service v" + BuildInfo.INSTANCE.getProjectVersion() + " ready");
    }

    protected void handleCommand() throws IOException {
        currentLine = conn.readLine();

        if (currentLine == null) {
            log.debug("Client closed connection {}", socketChannel.getRemoteAddress());
            close();
            return;
        }

        if (!commandLegalSize()) {
            return;
        }

        String commandName = currentLine.substring(0, 4).toUpperCase();
        SmtpCommand command = registry.getCommand(commandName);

        if (command == null) {
            conn.send("500 Command not recognized");
            return;
        }

        command.execute(conn, state, manager, currentLine);
    }

    protected boolean commandLegalSize() {
        if (currentLine.length() < 4) {
            conn.send("500 Invalid command. Must be 4 characters");
            return false;
        }

        if (currentLine.length() > 4 &&
            currentLine.charAt(4) != ' ') {
            conn.send("500 Invalid command. Must be 4 characters");
            return false;
        }

        if (currentLine.length() > LINE_LENGHT_LIMIT) {
            conn.send("500 Command too long.  " + LINE_LENGHT_LIMIT + " character maximum.");
            return false;
        }

        return true;
    }

    public void close() {
        if (quitting) {
            return;
        }
        quitting = true;
        if (conn != null) {
            // conn.quit() will call this.close() again, creating a potential loop if not handled.
            // NioSmtpConnection.quit() already tries to close the channel.
            // We ensure socketChannel is closed here.
        }
        try {
            if (socketChannel != null && socketChannel.isOpen()) {
                socketChannel.close();
                log.debug("Closed SMTP connection to {}", socketChannel.getRemoteAddress());
            }
        } catch (IOException e) {
            log.warn("Error closing socket channel for {}", socketChannel.socket().getRemoteSocketAddress(), e);
        }
        // If this handler was registered with a selector, it should be cancelled here.
        // server.deregisterHandler(this); // Example if server manages handlers
    }

    public boolean isQuitting() {
        return quitting || (socketChannel != null && !socketChannel.isOpen());
    }

    // This method is called by NioSmtpConnection.quit()
    // It's a bit of a circular dependency if SmtpConnection expects a SmtpHandler that has this method.
    // The cast `(SmtpHandler)(Object)this` is a hack.
    // Ideally, NioSmtpConnection would take a more generic interface, or NioSmtpHandler would implement SmtpHandler.
    // For now, this placeholder satisfies the NioSmtpConnection's call to handler.close().
    // Consider this part of the "TODO" for refactoring.
    public void closeFromConnection() {
        this.close();
    }
}
