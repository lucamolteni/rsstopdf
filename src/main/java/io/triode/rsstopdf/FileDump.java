package io.triode.rsstopdf;

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
		String sanitiziedTitle = sanitizeFileName(feedTitle);
		Path path = rssToPdfPath(PHASE_01_FETCH, sanitiziedTitle + ".xml");

		return writeFile(fetchBody, path);
	}

	public String dumpArticle(RssParser.Article article, String websiteTitle) {
		// TODO probably it's insicure to depend on a file name provided by outside, I'm not sure this is enough
		String sanitiziedTitle = sanitizeFileName(websiteTitle);
		String sanitiziedArticle = sanitizeFileName(article.title());
		Path articlePath = rssToPdfPath(PHASE_02_CREATE_ARTICLES, sanitiziedTitle, sanitiziedArticle + ".xml");

		String fetchBody = article.body();
		return writeFile(fetchBody, articlePath);
	}

	public void dumpImage(String article, RssParser.ArticleImage ai) {
		String sanitizedArticle = sanitizeFileName(article);
		String sanitizedImageName = sanitizeFileName(ai.fileName());
		Path imagePath = rssToPdfPath(PHASE_03_PDF, "img", sanitizedArticle, sanitizedImageName);
		writeByteArray(ai.content(), imagePath);
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

	public boolean pdfExists() {
		return Files.exists(Paths.get(todayDirectory, PHASE_03_PDF, filename + ".pdf"));
	}

	public String movePdfFile() {
		if (!pdfExists()) {
			Logger.error("PDF was not generated");
			return null;
		}
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

	public void dumpErrorLog(List<String> errors) {
		if (errors.isEmpty()) {
			return;
		}
		Path errorLog = Path.of(todayDirectory, "errors.log");
		writeFile(String.join("\n", errors), errorLog);
		Logger.warn("Errors occurred during processing. See: {}", errorLog);
	}

	public void dumpLatexErrorLog(String output) {
		if (output.isBlank()) {
			return;
		}
		Path errorLog = Path.of(todayDirectory, "error-latex.log");
		writeFile(output, errorLog);
		Logger.warn("pdflatex output saved to: {}", errorLog);
	}

	static String sanitizeFileName(String name) {
		return name.replaceAll("[:/\\\\*?\"<>|%]", "_");
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
