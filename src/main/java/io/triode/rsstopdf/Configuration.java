package io.triode.rsstopdf;

import org.tinylog.Logger;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class Configuration {
    public static final String HOME_DIR = System.getProperty("user.home");
    public static final String RSS_TO_PDF_FOLDER = ".rsstopdf";
    private static final String PDF_OUTPUT_FOLDER = "pdfs";

    private static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm");

    public String todayTimestampString(Instant runTimestamp) {
        return dateTimeFormatter.format(runTimestamp.atZone(ZoneId.systemDefault()));
    }

    public String rssToPdfFolderEnsureCreation(Instant runTimestamp) {
        Path configDir = Paths.get(HOME_DIR, RSS_TO_PDF_FOLDER, todayTimestampString(runTimestamp));
        return ensureDirectoryCreated(configDir);
    }

    public String rssToPdfFolderOutputPDF() {
        Path configDir = Paths.get(HOME_DIR, RSS_TO_PDF_FOLDER, PDF_OUTPUT_FOLDER);
        return ensureDirectoryCreated(configDir);
    }

    private static String ensureDirectoryCreated(Path configDir) {
        try {
            if (Files.notExists(configDir)) {
                Files.createDirectory(configDir);
                Logger.info("Created directory: " + configDir.toAbsolutePath());
            }
        } catch (IOException e) {
            Logger.error("Failed to create directory: " + configDir.toAbsolutePath(), e);
        }

        return configDir.toString();
    }

    public Opml parseOpmlFromPath(Path path) throws JAXBException {
        if (!path.toFile().exists()) {
            throw new IllegalStateException("opml.xml not found in " + path);
        }

        JAXBContext context = JAXBContext.newInstance(Opml.class);
        Unmarshaller unmarshaller = context.createUnmarshaller();
        return (Opml) unmarshaller.unmarshal(path.toFile());
    }

}
