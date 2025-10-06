package io.triode.rsstopdf;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.tinylog.Logger;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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
		latex.executePdflatex( texFilePath.toString(), texFilePath.getParent().toFile() );
		fileDump.movePdfFile();
	}

	private void fetchRss(Opml opml) {
		try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {

			rssTraversal.extractRssFeeds( opml )
					.forEach( outline -> executor.submit( () -> fetchRSS( outline ) ) );
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

		try (ExecutorService executorArticle = Executors.newVirtualThreadPerTaskExecutor()) {
			for (RssParser.Article article : parsedRssArticles.articles()) {
				executorArticle.submit(() -> fetchSpecificSite(article, parsedRssArticles.feed().getTitle()));
			}
		}

	}

	private void fetchSpecificSite(RssParser.Article article, String websiteTitle) {

		RssParser.Article refetchContentIfTooSmall = rssContent.refetchContentIfTooSmall(article);
		RssParser.Article cleanContent = rssContent.cleanContent(refetchContentIfTooSmall);

		fileDump.dumpArticle(cleanContent, websiteTitle);
		try (ExecutorService executorImages = Executors.newVirtualThreadPerTaskExecutor()) {
			for(RssParser.ArticleImage articleImage : cleanContent.articleImages()) {
				executorImages.submit(() -> {
					RssParser.ArticleImage imageWithByteContent = rssContent.addImageToArticle(articleImage, articleImage.fileName());
					fileDump.dumpImage(cleanContent.title(), imageWithByteContent);
				});
			}
		} finally {
			layout.addArticle(cleanContent.title(), cleanContent.body(), cleanContent.outline().htmlUrl);
		}
	}
}