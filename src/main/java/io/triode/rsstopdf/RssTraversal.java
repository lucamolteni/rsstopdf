package io.triode.rsstopdf;

import java.util.stream.Stream;

public class RssTraversal {

	private Stream.Builder<Outline> streamBuilder;

	public Stream<Outline> extractRssFeeds(Opml opml) {
		streamBuilder = Stream.builder();
		if ( opml != null && opml.body != null && opml.body.outlines != null ) {
			for ( Outline outline : opml.body.outlines ) {
				collectRssFeedsRecursively( outline );
			}
		}
		return streamBuilder.build();
	}

	private void collectRssFeedsRecursively(Outline outline) {
		if ( outline.xmlUrl != null && !outline.xmlUrl.isEmpty() ) {
			streamBuilder.add( outline );
		}
		if ( outline.subOutlines != null ) {
			for ( Outline subOutline : outline.subOutlines ) {
				collectRssFeedsRecursively( subOutline );
			}
		}
	}
}
