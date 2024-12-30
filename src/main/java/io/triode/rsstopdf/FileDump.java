package io.triode.rsstopdf;

import org.apache.commons.io.FilenameUtils;
import org.tinylog.Logger;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.*;
import java.util.Arrays;
import java.util.List;


public class FileDump {

	private final String todayDirectory;
	private final String filename;
	private final String outputFolder;

	private static final String PHASE_01_FETCH = "01-fetch";
	private static final String PHASE_02_CREATE_ARTICLES = "02-create-articles";
	private static final String PHASE_03_PDF = "03-finalPDF";

    public FileDump(String todayDirectory, String outputFolder, String filename) {
        Logger.info("Using tmp dir: {} ", todayDirectory);
		this.outputFolder = outputFolder;
		this.todayDirectory = todayDirectory;
		this.filename = filename;
	}

	public String dumpFetchResponse(
			String feedTitle,
			String fetchBody) {

		// TODO probably it's insicure to depend on a file name provided by outside, I'm not sure this is enough
		String sanitiziedTitle = FilenameUtils.getName(feedTitle);
		Path path = rssToPdfPath(PHASE_01_FETCH, sanitiziedTitle + ".xml");

		return writeFile(fetchBody, path);
	}

	public String dumpArticle(RssParser.Article article, String websiteTitle) {
		// TODO probably it's insicure to depend on a file name provided by outside, I'm not sure this is enough
		String sanitiziedTitle = FilenameUtils.getName(websiteTitle);
		String sanitiziedArticle = FilenameUtils.getName(article.title());
		Path articlePath = rssToPdfPath(PHASE_02_CREATE_ARTICLES, sanitiziedTitle, sanitiziedArticle + ".xml");

        List<RssParser.ArticleImage> articleImages = article.articleImages();

		articleImages.forEach(ai -> {
            Path imagePath = rssToPdfPath(PHASE_03_PDF, "img", sanitiziedArticle, ai.fileName());
            writeByteArray(ai.content(), imagePath);
        });

		String fetchBody = article.body();
		return writeFile(fetchBody, articlePath);

	}

	public Path dumpFinalTexFile(Layout layout) {
		Path path = rssToPdfPath(PHASE_03_PDF, filename + ".tex");

		try (Writer fileWriter = new FileWriter(path.toFile())) {
			layout.renderWriter(fileWriter);
			Logger.info("Rendered to file: " + path.toAbsolutePath());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		return path;
	}

	public String writeFile(String fetchBody, Path path) {
		try {
			Logger.info("Dumping fetched content to file: {}", path);
			Files.writeString(path, fetchBody, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
			return path.toAbsolutePath().toString();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public String movePdfFile() {
		try {
			Path pdfPath = rssToPdfPath(PHASE_03_PDF, filename + ".pdf");
			Path finalPath = Path.of(outputFolder, filename + ".pdf");
			Files.move(pdfPath , finalPath);

			Logger.info("Moving generated file to: {}", finalPath);
			return finalPath.toString();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public String writeByteArray(byte[] content, Path path) {
		try {
			Logger.info("Dumping fetched content to file: {}", path);
			Files.write(path, content, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
			return path.toAbsolutePath().toString();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private Path rssToPdfPath(String phase, String... more) {
		String[] fixedPart = {phase};
		String[] fullPath = Arrays.copyOf(fixedPart, fixedPart.length + more.length);
		System.arraycopy(more, 0, fullPath, fixedPart.length, more.length);
		Path path = Paths.get(todayDirectory, fullPath);
		try {
			Files.createDirectories(path.getParent());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return path;
	}
}
