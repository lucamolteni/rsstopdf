package io.triode.rsstopdf;

import com.rometools.rome.feed.synd.SyndContentImpl;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.FeedException;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.tinylog.Logger.info;

public class RssParser {

	private final Instant runTimeStamp;

	public RssParser(Instant currentDate) {
		this.runTimeStamp = currentDate;
	}

	public sealed interface ParseResult permits ParseSuccess, ParseFailure, ParseFailureFile {
	}

	public record ParseSuccess(Outline outline, SyndFeed feed, List<Article> articles) implements ParseResult {
	}

	public record ParseFailure(Outline outline, Exception e) implements ParseResult {
	}

	public record ParseFailureFile(File file, Exception e) implements ParseResult {
	}

	public record Article(Outline outline, SyndEntry entry, List<ArticleImage> articleImages) {
		public Article(Outline outline, SyndEntry entry) {
			this( outline, entry, new ArrayList<>( 0 ) );
		}

		public String title() {
			return entry.getTitle();
		}

		public String body() {
			return entry.getDescription() != null ? entry.getDescription().getValue() : "";
		}

		public String link() {
			return entry.getLink();
		}

		public SyndEntry changeBody(String newBody) {
			if(entry.getDescription() == null) {
				entry.setDescription( new SyndContentImpl() );
			}
			entry.getDescription().setValue( newBody );
			return entry;
		}

		public Article addImage(ArticleImage image) {
			articleImages.add( image );
			return this;
		}
	}

	public record ArticleImage(byte[] content, String fileName) {
		public static final byte[] EMPTY_BYTE = new byte[0];

		public ArticleImage(String fileName) {
			this(EMPTY_BYTE, fileName);
		}
	}


	public ParseResult parseRSSBodyAndFilterByDate(
			Outline outline,
			SyndFeed feed) {

		try {
			List<Article> articles = articlesFilteredByDate( feed )
					.map( entry -> new Article( outline, entry ) )
					.toList();
			return new ParseSuccess( outline, feed, articles );
		}
		catch (IllegalArgumentException e) {
			info( "Failed to parse feed from URL: " + outline.xmlUrl );
			return new ParseFailure( outline, e );
		}
	}

	public ParseResult parseRSSBodyAndFilterByDate(Outline outline, HttpResponse<String> response) {
		byte[] byteArray = response.body().getBytes( StandardCharsets.UTF_8 );
		InputStream inputStream = new ByteArrayInputStream( byteArray );

		try {
			XmlReader reader = new XmlReader( inputStream );
			SyndFeed feed = new SyndFeedInput().build( reader );
			return parseRSSBodyAndFilterByDate( outline, feed );
		}
		catch (FeedException | IOException e) {
			return new ParseFailure( outline, e );
		}
	}

	public Stream<SyndEntry> articlesFilteredByDate(SyndFeed feed) {
		return feed.getEntries().stream()
				.filter( e -> {
					Instant minusOneday = runTimeStamp.minus(Duration.ofDays(1));
					Instant articleInstant = e.getPublishedDate()
							.toInstant();

					return articleInstant.isAfter( minusOneday ) && articleInstant.isBefore( runTimeStamp );
				} );
	}
}
