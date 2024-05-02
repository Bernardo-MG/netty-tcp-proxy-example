/**
 * The MIT License (MIT)
 * <p>
 * Copyright (c) 2023 the original author or authors.
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.bernardomg.example.netty.proxy.cli.command;

import java.io.OutputStream;
import java.io.PrintWriter;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;

import com.bernardomg.example.netty.proxy.cli.TransactionPrinterListener;
import com.bernardomg.example.netty.proxy.cli.version.ManifestVersionProvider;
import com.bernardomg.example.netty.proxy.server.NettyTcpProxyServer;
import com.bernardomg.example.netty.proxy.server.ProxyListener;
import com.bernardomg.example.netty.proxy.server.Server;

import picocli.CommandLine.Command;
import picocli.CommandLine.Help;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;

/**
 * Start TCP proxy command.
 *
 * @author Bernardo Mart&iacute;nez Garrido
 *
 */
@Command(name = "start", description = "Starts a TCP proxy", mixinStandardHelpOptions = true,
        versionProvider = ManifestVersionProvider.class)
public final class StartProxyCommand implements Runnable {

    /**
     * Debug flag. Shows debug logs.
     */
    @Option(names = { "--debug" }, paramLabel = "flag", description = "Enable debug logs.", defaultValue = "false")
    private Boolean     debug;

    /**
     * Server port.
     */
    @Option(names = { "-p", "--port" }, paramLabel = "port", description = "Proxy server port to listen.",
            required = true)
    private Integer     port;

    /**
     * Command specification. Used to get the line output.
     */
    @Spec
    private CommandSpec spec;

    /**
     * Target host.
     */
    @Option(names = { "-th", "--targetHost" }, paramLabel = "targetHost", description = "Target host.", required = true)
    private String      targetHost;

    /**
     * Target port.
     */
    @Option(names = { "-tp", "--targetPort" }, paramLabel = "targetPort", description = "Target port.", required = true)
    private Integer     targetPort;

    /**
     * Verbose mode. If active prints info into the console. Active by default.
     */
    @Option(names = { "--verbose" }, paramLabel = "flag", description = "Print information to console.",
            defaultValue = "true", showDefaultValue = Help.Visibility.ALWAYS)
    private Boolean     verbose;

    /**
     * Default constructor.
     */
    public StartProxyCommand() {
        super();
    }

    @Override
    public final void run() {
        final PrintWriter   writer;
        final Server        server;
        final ProxyListener listener;

        if (debug) {
            activateDebugLog();
        }

        if (verbose) {
            // Prints to console
            writer = spec.commandLine()
                .getOut();
        } else {
            // Prints nothing
            writer = new PrintWriter(OutputStream.nullOutputStream());
        }

        listener = new TransactionPrinterListener(port, targetHost, targetPort, writer);
        server = new NettyTcpProxyServer(port, targetHost, targetPort, listener);

        server.start();
    }

    /**
     * Activates debug logs for the application.
     */
    private final void activateDebugLog() {
        Configurator.setLevel("com.bernardomg.example", Level.DEBUG);
        Configurator.setLevel("io.netty.handler.logging", Level.DEBUG);
    }

}
