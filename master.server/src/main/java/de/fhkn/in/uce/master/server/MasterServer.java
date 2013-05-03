/*
 * Copyright (c) 2013 Robert Danczak,
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package de.fhkn.in.uce.master.server;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.fhkn.in.uce.mediator.Mediator;
import de.fhkn.in.uce.relaying.server.RelayServer;
import de.fhkn.in.uce.stun.server.StunServer;

/**
 * Class to start a main server which starts a stun, relay and mediator server.
 *
 * @author Robert Danczak
 */
public class MasterServer {

    private static final Logger logger = LoggerFactory.getLogger(MasterServer.class);

    private final int EXECUTOR_THREADS = 3;
    private final int TERMINATION_TIME = 100;

    private final ExecutorService executorService;
    private ArgumentHandler argHandler;

    /**
     * Creates a master server.
     */
    public MasterServer() {
        executorService = Executors.newFixedThreadPool(EXECUTOR_THREADS);
        try {
            argHandler = new ArgumentHandler(logger);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Main method to create and start the master server.
     *
     * @param args
     */
    public static void main(final String[] args) {
        MasterServer masterServer = new MasterServer();
        try {
            masterServer.run(args);
        } catch (Exception e) {
            logger.error("An error occured during startup of the master server.");
            e.printStackTrace();
            logger.error("Execption:", e);
        }
    }

    /**
     * Starts the master server and its children stun, relay and mediator.
     */
    public void run(final String[] args) throws InterruptedException {
        try {
            argHandler.parseArguments(args);
        } catch (IllegalArgumentException e) {
            return;
        }

        stunServerTask();
        relayServerTask();
        mediatorServerTask();

        shutdownExecutor();
    }


    private void shutdownExecutor() {
        try {
            executorService.shutdown();
            logger.info("Force shutting down worker threads in {} ms", TERMINATION_TIME);
            if (!executorService.awaitTermination(TERMINATION_TIME, TimeUnit.MILLISECONDS)) {
                executorService.shutdownNow();
            }
        } catch (Exception e) {
            executorService.shutdown();
            Thread.currentThread().interrupt();
        }
    }

    private void logInfo(String msg) {
        System.out.println(msg);
        logger.info(msg);
    }

    private void relayServerTask() {
        logInfo("Starting Relay Server");
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    List<String> relayArgs = argHandler.getRelayArgs();
                    RelayServer.main(relayArgs.toArray(new String[relayArgs.size()]));
                    logInfo("Successfully started Relay Server");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void stunServerTask() {
        logInfo("Starting Stun Server");
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                List<String> stunArgs = argHandler.getStunArgs();
                StunServer.main(stunArgs.toArray(new String[stunArgs.size()]));
                logInfo("Successfully started Stun Server");
            }
        });
    }

    private void mediatorServerTask() {
        logInfo("Starting Mediator Server");
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    List<String> mediatorArgs = argHandler.getMediatorArgs();
                    Mediator.main(mediatorArgs.toArray(new String[mediatorArgs.size()]));
                    logInfo("Successfully started Mediator Server");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }
}
