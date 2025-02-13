package com.example;

import io.prometheus.client.Counter;
import io.prometheus.client.exporter.HTTPServer;
import org.apache.commons.cli.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;

public class Main {
    public static void main(String[] args) throws IOException {
        // Kommandozeilen-Parser
        Options options = new Options();
        options.addOption("p", "sourcePath", true, "Path to source file (default: /tmp/source.bin)");
        options.addOption("d", "destinationPath", true, "Path to destination file (default: /tmp/target.bin)");
        options.addOption("s", "size", true, "Size of file in MB (default: 200)");
        options.addOption("t", "interval", true, "Interval in seconds (default: 5)");
        options.addOption("n", "runs", true, "Number of runs (default: 2)");

        CommandLineParser parser = new DefaultParser();
        CommandLine cmd;
        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            System.err.println("Error parsing arguments: " + e.getMessage());
            new HelpFormatter().printHelp("java -jar app.jar", options);
            return;
        }

        // Werte aus Argumenten oder Standardwerte setzen
        String sourcePath = cmd.getOptionValue("p", "/tmp/source.bin");
        String destinationPath = cmd.getOptionValue("d", "/tmp/target.bin");
        int fileSizeMB = Integer.parseInt(cmd.getOptionValue("s", "200"));
        int intervalSeconds = Integer.parseInt(cmd.getOptionValue("t", "5"));
        int runs = Integer.parseInt(cmd.getOptionValue("n", "2"));

        System.out.println("=== Java Mem Thief ===");
        System.out.println("== Config ==");
        System.out.println("filePathSource: " + sourcePath);
        System.out.println("filePathDestination: " + destinationPath);
        System.out.println("fileSize: " + fileSizeMB);
        System.out.println("intervalSeconds: " + intervalSeconds);
        System.out.println("runs: " + runs);

//        new HTTPServer(2112);

        // Metriken definieren
        Counter fileWriteOperationsTotal = Counter.build()
                .name("file_write_operations_total")
                .help("Total number of file write operations")
                .labelNames("destination", "status", "size")
                .register();

        try {
            byte[] randomData = new byte[fileSizeMB * 1024 * 1024];
            Arrays.fill(randomData, (byte) 1);
            Files.write(Path.of(sourcePath), randomData, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            System.out.printf("Source File %s (%d MB) Created\n", sourcePath, fileSizeMB);
        } catch (IOException e) {
            System.err.println("Error creating the source file: " + e.getMessage());
            return;
        }

        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            int i = 0;

            @Override
            public void run() {
                if (i < runs) {
                    try {
                        byte[] content = Files.readAllBytes(Path.of(sourcePath));
                        System.out.println("File read, Size: " + content.length + " Bytes");
                        Files.write(Path.of(destinationPath), content, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                        fileWriteOperationsTotal.labels(destinationPath, "success", String.valueOf(fileSizeMB)).inc();
                        System.out.println("File written");
                    } catch (IOException e) {
                        System.err.println("Error reading/writing file: " + e.getMessage());
                    }
                    i++;
                } else {
                    System.out.println(runs + " read writes done... idling");
                    timer.cancel();
                }
            }
        }, 0, intervalSeconds * 1000);
    }
}
