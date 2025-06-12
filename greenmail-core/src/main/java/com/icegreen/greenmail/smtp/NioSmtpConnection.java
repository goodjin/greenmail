/*
 * Copyright (c) 2014 Wael Chatila / Icegreen Technologies. All Rights Reserved.
 * This software is released under the Apache license 2.0
 * This file has been used and modified.
 * Original file can be found on http://foedus.sourceforge.net
 */
package com.icegreen.greenmail.smtp;

import com.icegreen.greenmail.util.EncodingUtil;
import com.icegreen.greenmail.util.InternetPrintWriter;
import com.icegreen.greenmail.util.LoggingInputStream;
import com.icegreen.greenmail.util.LoggingOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;

public class NioSmtpConnection {
    private static final Logger log = LoggerFactory.getLogger(NioSmtpConnection.class);

    // networking/io stuff
    SocketChannel socketChannel;
    InetAddress clientAddress;
    InternetPrintWriter out;
    InputStream in; // For dotLimitedInputStream, consider replacing with NIO equivalent
    SmtpHandler handler; // This will be NioSmtpHandler
    String heloName;
    boolean authenticated; // Was there a successful authentication?

    public NioSmtpConnection(SmtpHandler handler, SocketChannel socketChannel)
        throws IOException {
        this.socketChannel = socketChannel;
        clientAddress = socketChannel.socket().getInetAddress();

        // Output
        OutputStream o = Channels.newOutputStream(socketChannel);
        if(log.isDebugEnabled()) {
            o = new LoggingOutputStream(o, "S: ");
        }
        out = InternetPrintWriter.createForEncoding(o, true, EncodingUtil.CHARSET_EIGHT_BIT_ENCODING);

        // Input
        InputStream is = Channels.newInputStream(socketChannel);
        if (log.isDebugEnabled()) {
            is = new LoggingInputStream(is, "C: ");
        }
        // Wrap in BufferedInputStream for readLine and dotLimitedInputStream logic for now
        // This might need further NIO optimization if performance becomes an issue
        in = new BufferedInputStream(is);


        this.handler = handler;
    }

    public void send(String line) {
        try {
            ByteBuffer buffer = ByteBuffer.wrap((line + "\r\n").getBytes(StandardCharsets.US_ASCII));
            while (buffer.hasRemaining()) {
                socketChannel.write(buffer);
            }
        } catch (IOException e) {
            log.error("Error sending data on NIO connection", e);
            // Consider how to handle this error, e.g., closing connection
        }
    }

    public String readLine() throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream(256);
        ByteBuffer buffer = ByteBuffer.allocate(1); // Read one byte at a time
        while (true) {
            buffer.clear();
            int bytesRead = socketChannel.read(buffer);

            if (bytesRead == -1) { // End of stream
                if (log.isDebugEnabled()) {
                    log.debug("Unexpected end of stream, read {} bytes: {}", bos.size(), bos);
                }
                if (bos.size() > 0) {
                    return bos.toString(StandardCharsets.US_ASCII.name());
                } else {
                    return null; // No input received
                }
            }

            buffer.flip();
            byte b = buffer.get();

            if (b == '\r') {
                // Peek for \n
                buffer.clear();
                // This is tricky with non-blocking. For simplicity, let's assume a blocking read for the next char or adapt.
                // For a truly non-blocking approach, this would need a state machine.
                // The current Channels.newInputStream().read() approach for 'in' might be simpler here.
                // Reverting to a simpler read for now, assuming 'in' is from Channels.newInputStream(socketChannel)
                // and wrapped in BufferedInputStream as done in the constructor.
                // This part needs careful NIO handling if we move away from InputStream 'in'.
                // For now, let's use the 'in' field which is a BufferedInputStream wrapping the NIO channel's input stream.
                // This simplifies CRLF handling but is not pure NIO for reading.
                int nextB = in.read(); // This blocks if using the wrapped InputStream
                if (nextB == '\n') {
                    return bos.toString(StandardCharsets.US_ASCII.name());
                } else {
                    bos.write('\r');
                    if (nextB != -1) bos.write(nextB); // If it wasn't \n, put it in the stream
                    if (nextB == -1) return bos.toString(StandardCharsets.US_ASCII.name()); // EOS after \r
                }
            } else {
                bos.write(b);
            }
        }
    }


    private static final int CR_LF_DOT = '\r' << 16 | '\n' << 8 | '.';
    private static final int CR_LF_DOT_CR = '\r' << 24 | '\n' << 16 | '.' << 8 | '\r';

    public InputStream dotLimitedInputStream(byte[] initialContent) {
        // This method relies on blocking reads from 'in'.
        // For a pure NIO implementation, this would need to be rewritten
        // to use ByteBuffers and manage read states carefully.
        // Given the complexity, and for a first pass, we'll keep using 'in'
        // which is a BufferedInputStream over the SocketChannel's input stream.
        ByteArrayOutputStream bos = new ByteArrayOutputStream(1024);
        try {
            bos.write(initialContent);

            int cbuf = 0;
            while (true) {
                int b = in.read(); // Blocking read
                if (b < 0) {
                    throw new IllegalStateException("Unexpected end of stream, read " + bos.size() + " bytes: " + bos);
                }

                if (cbuf == CR_LF_DOT_CR && b == '\n') { // CRLF-DOT-CRLF
                    final byte[] buf = bos.toByteArray();
                    int maxLen = Math.min(bos.size(), bos.size() - 4 /* CR + LF + DOT + CR */);
                    return new ByteArrayInputStream(buf, 0, maxLen);
                } else if ((cbuf & 0xffffff) == CR_LF_DOT && b == '.') { // CR_LF_DOT and DOT => Skip dot once
                    // https://tools.ietf.org/html/rfc5321#section-4.5.2
                } else {
                    bos.write(b);
                }
                cbuf = (cbuf << 8) | b;
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Can not read line, read " + bos.size() + " bytes: " + bos, ex);
        }
    }

    public String getClientAddress() {
        return clientAddress.getHostAddress();
    }

    public InetAddress getServerAddress() {
        return socketChannel.socket().getLocalAddress();
    }

    public String getServerGreetingsName() {
        InetAddress address = getServerAddress();
        if (address != null)
            return address.toString();
        else
            return System.getProperty("user.name");
    }

    public String getHeloName() {
        return heloName;
    }

    public void setHeloName(String n) {
        heloName = n;
    }

    public void quit() {
        // Handler should close the SmtpState and then this connection.
        // The actual socketChannel closing might be managed by NioSmtpHandler or NioSmtpServer
        if (handler != null) {
            handler.close(); // This should trigger NioSmtpHandler.close()
        }
        try {
            if (socketChannel.isOpen()) {
                socketChannel.close();
            }
        } catch (IOException e) {
            log.warn("Failed to close socket channel", e);
        }
    }

    public boolean isAuthenticated() {
        return authenticated;
    }

    public void setAuthenticated(boolean authenticated) {
        this.authenticated = authenticated;
    }
}
