package io.triode.rsstopdf;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.tinylog.Logger;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.StructuredTaskScope;
import java.util.concurrent.StructuredTaskScope.Subtask;
import java.util.concurrent.TimeUnit;

import static org.tinylog.Logger.info;

public final class RSSToPDF {

	private final RssTraversal rssTraversal;
	private final HttpFetch httpFetch;
	private final FileDump fileDump;
	private final RssParser rssParser;
	private final RSSContent rssContent;
	private final Layout layout;
	private final Latex latex;

	private final List<String> errors = new CopyOnWriteArrayList<>();
	private final MeterRegistry registry = new SimpleMeterRegistry();
	private final Timer timer;

	public RSSToPDF(
			RssTraversal rssTraversal,
			HttpFetch httpFetch,
			FileDump fileDump,
			RssParser rssParser,
			RSSContent rssContent, Layout layout, Latex latex) {
		this.rssTraversal = rssTraversal;
		this.httpFetch = httpFetch;
		this.fileDump = fileDump;
		this.rssParser = rssParser;
		this.rssContent = rssContent;
		this.layout = layout;
		this.latex = latex;

		timer = registry.timer( "fetchrssfeed.timer" );
	}

	public static void main(String[] args) throws IOException, JAXBException {
		Instant runTimeStamp = Instant.now();

		Configuration configuration = new Configuration();

		Path opmlPath;
		if(args.length > 0) {
            opmlPath = Paths.get(args[0]);
        } else {
			opmlPath = Paths.get(Configuration.HOME_DIR, Configuration.RSS_TO_PDF_FOLDER, "opml.xml");
		}

		String outputFolder;
		if(args.length > 1) {
			outputFolder = args[1];
		} else {
			outputFolder = configuration.rssToPdfFolderOutputPDF();
		}

		FileDump fileDump = new FileDump(
				configuration.rssToPdfFolderEnsureCreation(runTimeStamp),
				outputFolder,
				configuration.todayTimestampString(runTimeStamp)
		);

		Opml opml = configuration.parseOpmlFromPath(opmlPath);

		RssTraversal rssTraversal = new RssTraversal();
		HttpFetch httpFetch = new HttpFetch();

		RssParser rssParser = new RssParser( runTimeStamp );
		RSSContent readability = new RSSContent( httpFetch );
		Layout layout = new Layout();
		Latex latex = new Latex();

		new RSSToPDF( rssTraversal, httpFetch, fileDump, rssParser, readability, layout, latex ).run( opml );
	}

	private void run(Opml opml) {
		timer.record( () -> fetchRss( opml ) );
		info( "Elapsed time:  {} ms", timer.totalTime( TimeUnit.MILLISECONDS ) );

		Path texFilePath = fileDump.dumpFinalTexFile( layout );
		String latexOutput = latex.executePdflatex( texFilePath.toString(), texFilePath.getParent().toFile() );
		fileDump.dumpLatexErrorLog(latexOutput);
		fileDump.dumpErrorLog(errors);
		fileDump.movePdfFile();
	}

	private void fetchRss(Opml opml) {
		try (var scope = StructuredTaskScope.open(StructuredTaskScope.Joiner.awaitAll())) {
			List<Subtask<?>> subtasks = new ArrayList<>();
			rssTraversal.extractRssFeeds( opml )
					.forEach( outline -> subtasks.add( scope.fork( () -> fetchRSS( outline ) ) ) );
			scope.join();
			logFailedSubtasks(subtasks, "feed");
		} catch (InterruptedException e) {
			Logger.error("Feed fetching interrupted", e);
		}
	}

	private void fetchRSS(Outline outline) {
		Optional<HttpResponse<String>> optArticles = httpFetch.getURLFollowingRedirects( outline.xmlUrl );

		if ( optArticles.isEmpty() ) {
			return;
		}

		HttpResponse<String> articles = optArticles.get();

		// Do this only if dump is enabled
		fileDump.dumpFetchResponse(
				outline.title,
				articles.body()
		);

		RssParser.ParseResult optionalParseResult = rssParser.parseRSSBodyAndFilterByDate( outline, articles );
		if ( optionalParseResult instanceof RssParser.ParseFailure ) {
			Logger.error( "Failed to parse RSS feed: {}", ( (RssParser.ParseFailure) optionalParseResult ).e() );
			return;
		}

		RssParser.ParseSuccess parsedRssArticles = (RssParser.ParseSuccess) optionalParseResult;

		try (var scope = StructuredTaskScope.open(StructuredTaskScope.Joiner.awaitAll())) {
			List<Subtask<?>> subtasks = new ArrayList<>();
			for (RssParser.Article article : parsedRssArticles.articles()) {
				subtasks.add(scope.fork(() -> fetchSpecificSite(article, parsedRssArticles.feed().getTitle())));
			}
			scope.join();
			logFailedSubtasks(subtasks, "article");
		} catch (InterruptedException e) {
			Logger.error("Article fetching interrupted for feed: {}", outline.title, e);
		}

	}

	private void fetchSpecificSite(RssParser.Article article, String websiteTitle) {

		RssParser.Article refetchContentIfTooSmall = rssContent.refetchContentIfTooSmall(article);
		RssParser.Article cleanContent = rssContent.cleanContent(refetchContentIfTooSmall);

		fileDump.dumpArticle(cleanContent, websiteTitle);
		try (var scope = StructuredTaskScope.open(StructuredTaskScope.Joiner.awaitAll())) {
			List<Subtask<?>> subtasks = new ArrayList<>();
			for(RssParser.ArticleImage articleImage : cleanContent.articleImages()) {
				subtasks.add(scope.fork(() -> {
					RssParser.ArticleImage imageWithByteContent = rssContent.addImageToArticle(articleImage, articleImage.fileName());
					fileDump.dumpImage(cleanContent.title(), imageWithByteContent);
				}));
			}
			scope.join();
			logFailedSubtasks(subtasks, "image");
		} catch (InterruptedException e) {
			Logger.error("Image fetching interrupted for article: {}", cleanContent.title(), e);
		} finally {
			layout.addArticle(cleanContent.title(), cleanContent.body(), cleanContent.outline().htmlUrl);
		}
	}

	private void logFailedSubtasks(List<Subtask<?>> subtasks, String taskType) {
		for (Subtask<?> subtask : subtasks) {
			if (subtask.state() == Subtask.State.FAILED) {
				Throwable ex = subtask.exception();
				Logger.error("Failed {} task: {}", taskType, ex.getMessage(), ex);
				StringWriter sw = new StringWriter();
				ex.printStackTrace(new PrintWriter(sw));
				errors.add("[" + taskType + "] " + sw);
			}
		}
	}
}