/*
 * Copyright (c) 2014 Wael Chatila / Icegreen Technologies. All Rights Reserved.
 * This software is released under the Apache license 2.0
 */
package com.icegreen.greenmail.smtp;

import com.icegreen.greenmail.Managers;
import com.icegreen.greenmail.smtp.commands.SmtpCommandRegistry;
import com.icegreen.greenmail.util.ServerSetup;
import com.icegreen.greenmail.util.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.CountDownLatch;

public class NioSmtpServer implements Service { // Changed from AbstractServer
    protected final Logger log = LoggerFactory.getLogger(getClass());
    protected ServerSetup setup;
    protected Managers managers;
    protected ServerSocketChannel serverSocketChannel;
    protected Selector selector;
    private volatile boolean keepRunning = false;
    private volatile boolean running = false;
    private final CountDownLatch startupMonitor = new CountDownLatch(1);
    private Thread serverThread;
    private ExecutorService workerPool; // For handling client connections

    public NioSmtpServer(ServerSetup setup, Managers managers) {
        this.setup = setup;
        this.managers = managers;
        // Using a fixed thread pool, size can be configurable
        this.workerPool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    }

    protected void openServerSocket() throws IOException {
        serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.configureBlocking(false); // Non-blocking for selector

        InetAddress bindTo;
        String bindAddress = setup.getBindAddress();
        if (null == bindAddress) {
            bindAddress = setup.getDefaultBindAddress();
        }
        bindTo = InetAddress.getByName(bindAddress);

        serverSocketChannel.socket().setReuseAddress(true);
        serverSocketChannel.socket().bind(new InetSocketAddress(bindTo, setup.getPort()));

        // Update setup with actual port if it was dynamically allocated (port 0)
        this.setup = setup.port(serverSocketChannel.socket().getLocalPort());
        log.info("NIO SMTP Server started on {}:{}", bindAddress, setup.getPort());

        selector = Selector.open();
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
    }

    public void runServerLoop() {
        try {
            initServerSocket();
            setRunning(true);
            startupMonitor.countDown();
            log.debug("NIO SMTP Server {} accepting connections", getName());

            while (keepRunning && serverSocketChannel.isOpen()) {
                try {
                    // Wait for an event, timeout to allow checking keepRunning flag
                    if (selector.select(1000) == 0) {
                        continue;
                    }

                    Set<SelectionKey> selectedKeys = selector.selectedKeys();
                    Iterator<SelectionKey> keyIterator = selectedKeys.iterator();

                    while (keyIterator.hasNext()) {
                        SelectionKey key = keyIterator.next();
                        keyIterator.remove();

                        if (!key.isValid()) {
                            continue;
                        }

                        if (key.isAcceptable()) {
                            acceptConnection(key);
                        } else if (key.isReadable()) {
                            // This simplistic model directly hands off to a handler.
                            // A more advanced model might do initial read here or use OP_WRITE.
                            // For now, the handler takes over the channel.
                            // This part might need refinement if we want the selector to manage reads/writes.
                            handleClientActivity(key);
                        }
                    }
                } catch (IOException e) {
                    if (keepRunning) { // Only log if we are supposed to be running
                        log.error("Error in NIO SMTP server loop for {}", getName(), e);
                    }
                    // If serverSocketChannel is closed or selector is closed, it might throw exceptions here.
                    // We should ensure keepRunning is false to exit loop gracefully.
                }
            }
        } catch (Exception e) {
            // Catch all for unexpected errors during startup or loop
            log.error("Fatal error in NIO SMTP server {}, shutting down.", getName(), e);
            setRunning(false);
            startupMonitor.countDown(); // Ensure anyone waiting on startup is released
        } finally {
            closeSelectorAndChannel();
            workerPool.shutdown();
            try {
                if (!workerPool.awaitTermination(5, TimeUnit.SECONDS)) {
                    workerPool.shutdownNow();
                }
            } catch (InterruptedException e) {
                workerPool.shutdownNow();
                Thread.currentThread().interrupt();
            }
            setRunning(false);
            log.info("NIO SMTP Server {} stopped.", getName());
        }
    }

    protected synchronized void initServerSocket() throws IOException {
        openServerSocket();
    }

    private void acceptConnection(SelectionKey key) throws IOException {
        ServerSocketChannel ssc = (ServerSocketChannel) key.channel();
        SocketChannel socketChannel = ssc.accept(); // Accept the connection
        if (socketChannel != null) {
            socketChannel.configureBlocking(false); // Configure non-blocking for the client channel
            log.debug("Accepted new connection from {}", socketChannel.getRemoteAddress());

            // Create handler and submit to worker pool
            // The NioSmtpHandler will manage this channel from now on.
            // For a more selector-centric model, we would register this channel with the selector for OP_READ.
            // However, the current NioSmtpHandler.run() is more of a self-contained blocking-style handler.
            // This hybrid approach is simpler for now.
            NioSmtpHandler handler = new NioSmtpHandler(this, new SmtpCommandRegistry(), managers.getSmtpManager(), socketChannel);
            workerPool.submit(handler);
        }
    }

    private void handleClientActivity(SelectionKey key) {
        // In a model where the selector manages reads for client channels:
        // SocketChannel clientChannel = (SocketChannel) key.channel();
        // NioSmtpHandler handler = (NioSmtpHandler) key.attachment();
        // handler.handleRead(); // Or similar method
        // This is not used in the current hybrid model where handler takes over the channel.
        // If NioSmtpHandler's run() method is changed to be event-driven, this method would be used.
        // For now, client channels are not registered for OP_READ with this server's selector.
        // The NioSmtpHandler uses the channel in a more traditional blocking way (via InputStream wrapper)
        // or direct ByteBuffer reads within its own thread.
    }


    protected void closeSelectorAndChannel() {
        log.debug("Closing NIO SMTP server socket and selector for {}", getName());
        if (selector != null && selector.isOpen()) {
            try {
                // Iterate over keys and close channels associated with them
                for (SelectionKey key : selector.keys()) {
                    if (key.channel() != null && key.channel().isOpen()) {
                        try {
                            key.channel().close();
                        } catch (IOException e) {
                            log.warn("Error closing channel during shutdown for {}", getName(), e);
                        }
                    }
                }
                selector.close();
            } catch (IOException e) {
                log.error("Error closing selector for {}", getName(), e);
            }
        }

        if (serverSocketChannel != null && serverSocketChannel.isOpen()) {
            try {
                serverSocketChannel.close();
            } catch (IOException e) {
                log.error("Error closing server socket channel for {}", getName(), e);
            }
        }
    }

    public String getName() {
        if (setup == null || serverSocketChannel == null || serverSocketChannel.socket() == null) {
            return "NioSmtpServer";
        }
        SocketAddress localAddress = serverSocketChannel.socket().getLocalSocketAddress();
        if (localAddress instanceof InetSocketAddress) {
            InetSocketAddress la = (InetSocketAddress) localAddress;
            return setup.getProtocol() + ":" + la.getAddress().getHostAddress() + ":" + la.getPort();
        }
        return setup.getProtocol() + ":<unbound>";
    }

    @Override
    public void startService() {
        if (running || (serverThread != null && serverThread.isAlive())) {
            log.warn("NIO SMTP Server {} already running or start requested multiple times.", getName());
            return;
        }
        keepRunning = true;
        serverThread = new Thread(this::runServerLoop, "NioSmtpServer-Acceptor-" + setup.getPort());
        serverThread.start();
    }

    @Override
    public void stopService(long timeout) {
        log.info("Stopping NIO SMTP Server {}...", getName());
        keepRunning = false;
        if (selector != null) {
            selector.wakeup(); // Interrupt selector.select()
        }

        if (serverThread != null) {
            try {
                serverThread.join(timeout);
            } catch (InterruptedException e) {
                log.warn("Interrupted while waiting for NIO SMTP server {} to stop.", getName(), e);
                Thread.currentThread().interrupt();
            }
        }
        // Additional cleanup for worker pool is in runServerLoop's finally block
        setRunning(false); // Ensure running is false after attempted stop
        log.info("NIO SMTP Server {} has been requested to stop.", getName());
    }

    @Override
    public void stopService() {
        stopService(TimeUnit.SECONDS.toMillis(10)); // Default timeout
    }

    @Override
    public boolean isRunning() {
        return running && keepRunning && (serverSocketChannel != null && serverSocketChannel.isOpen());
    }

    protected void setRunning(boolean r) {
        this.running = r;
    }

    @Override
    public boolean waitTillRunning(long timeoutInMs) throws InterruptedException {
        return startupMonitor.await(timeoutInMs, TimeUnit.MILLISECONDS) && isRunning();
    }

    // Methods for handler management (optional, depending on selector model)
    public void registerClientChannel(SocketChannel clientChannel, NioSmtpHandler handler) throws IOException {
        // If we were to manage client reads/writes via main selector:
        // clientChannel.register(selector, SelectionKey.OP_READ, handler);
        // selector.wakeup(); // To make the selector re-evaluate immediately
    }

    public void deregisterClientChannel(SocketChannel clientChannel) {
        // If client channel was registered with the selector:
        // SelectionKey key = clientChannel.keyFor(selector);
        // if (key != null) {
        //    key.cancel();
        // }
    }

    public ServerSetup getServerSetup() {
        return setup;
    }
}
