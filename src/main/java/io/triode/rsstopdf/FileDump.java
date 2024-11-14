package io.triode.rsstopdf;

import org.apache.commons.io.FilenameUtils;
import org.tinylog.Logger;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.List;


public class FileDump {

	private final String dumpDirectory;
	private final String filename;

	private static final String PHASE_01_FETCH = "01-fetch";
	private static final String PHASE_02_CREATE_ARTICLES = "02-create-articles";
	private static final String PHASE_03_PDF = "03-finalPDF";

	public FileDump(String dumpDirectory, String filename) {
		Logger.info("Using tmp dir: {} ", dumpDirectory);
		this.dumpDirectory = dumpDirectory;
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

	public String dumpArticles(RssParser.ParseSuccess rssWithFullArticles, RssParser.Article article) {
		// TODO probably it's insicure to depend on a file name provided by outside, I'm not sure this is enough
		String sanitiziedTitle = FilenameUtils.getName(rssWithFullArticles.feed().getTitle());
		String sanitiziedArticle = FilenameUtils.getName(article.title());
		Path articlePath = rssToPdfPath(PHASE_02_CREATE_ARTICLES, sanitiziedTitle, sanitiziedArticle + ".xml");

        List<RssParser.ArticleImage> articleImages = article.articleImages();

		articleImages.forEach(ai -> {
            Path imagePath = rssToPdfPath(PHASE_03_PDF, "img", ai.fileName());
            writeByteArray(ai.content(), imagePath);
        });

		String fetchBody = article.body();
		return writeFile(fetchBody, articlePath);

	}

	public Path dumpFinalPDF(Layout layout) {
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
		Path path = Paths.get(dumpDirectory, fullPath);
		try {
			Files.createDirectories(path.getParent());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return path;
	}
}
