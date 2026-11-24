package com.urbanvibe.utility;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

public class FileManager {

    public static final String RECEIPTS_DIR = "receipts";
    public static final String LOGS_DIR = "logs";
    public static final String REPORTS_DIR = "reports";
    public static final String EXPORTS_DIR = "exports";
    public static final String CONFIG_DIR = "config";

    private static final Object APPEND_LOCK = new Object();

    private FileManager() { }

    public static void ensureDirectories() {
        String[] dirs = {RECEIPTS_DIR, LOGS_DIR, REPORTS_DIR, EXPORTS_DIR, CONFIG_DIR};
        for (String dir : dirs) {
            try {
                Files.createDirectories(Paths.get(dir));
            } catch (IOException e) {
                System.out.println("Warning: could not create folder '" + dir + "': " + e.getMessage());
            }
        }
    }

    public static void writeFile(String path, String content) throws IOException {
        Path p = Paths.get(path);
        if (p.getParent() != null) {
            Files.createDirectories(p.getParent());
        }
        try (BufferedWriter writer = Files.newBufferedWriter(p, StandardCharsets.UTF_8)) {
            writer.write(content);
        }
    }

    public static void appendToFile(String path, String line) throws IOException {
        synchronized (APPEND_LOCK) {
            File file = new File(path);
            File parent = file.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, true))) {
                writer.write(line);
                writer.newLine();
            }
        }
    }

    public static String readFile(String path) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(path))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append('\n');
            }
        }
        return sb.toString();
    }

    public static Properties readProperties(String path) {
        Properties props = new Properties();
        File file = new File(path);
        if (!file.exists()) {
            return props;
        }
        try (FileReader reader = new FileReader(file)) {
            props.load(reader);
        } catch (IOException e) {
            System.out.println("Warning: could not read " + path + ": " + e.getMessage());
        }
        return props;
    }

    public static List<String> listFiles(String dir) {
        File[] files = new File(dir).listFiles();
        List<String> names = new ArrayList<>();
        if (files != null) {
            for (File f : files) {
                if (f.isFile() && !f.getName().startsWith(".")) {
                    names.add(f.getName());
                }
            }
        }
        Collections.sort(names);
        return names;
    }
}
