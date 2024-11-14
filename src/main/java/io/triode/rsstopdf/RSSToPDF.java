package io.triode.rsstopdf;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.tinylog.Logger;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
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
		FileDump fileDump = new FileDump(
				configuration.rssToPdfFolderEnsureCreation(runTimeStamp),
				configuration.todayDataDir(runTimeStamp)
		);
		Opml opml = configuration.parseOpmlFromTemporaryFolder();

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

		Path texFilePath = fileDump.dumpFinalPDF( layout );
		latex.executePdflatex( texFilePath.toString(), texFilePath.getParent().toFile() );
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
		List<RssParser.Article> fullArticles =
				parsedRssArticles.articles()
						.stream()
						.map( rssContent::refetchContentIfTooSmall )
						.map( rssContent::cleanContent )
						.toList();

		RssParser.ParseSuccess rssWithFullArticles = new RssParser.ParseSuccess(
				parsedRssArticles.outline(),
				parsedRssArticles.feed(),
				fullArticles
		);

		// Do this only if dump is enabled
		for ( RssParser.Article article : rssWithFullArticles.articles() ) {
			fileDump.dumpArticles( rssWithFullArticles, article );
		}

		for ( RssParser.Article article : fullArticles ) {
			layout.addArticle( article.title(), article.body(), article.outline().htmlUrl );
		}
	}
}