package io.triode.rsstopdf;

import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndEntryImpl;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.feed.synd.SyndFeedImpl;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RssParserTest {

    @Test
    public void testRefetchContentIfTooSmall() {
        Instant runTs = Instant.parse("2024-12-29T07:00:00Z");
        var rssParser = new RssParser(runTs);

        SyndEntryImpl twentyFourHoursAgoPlusOneArticle = feedOfDate("2024-12-28T07:00:01Z");
        SyndEntryImpl twentyFourHoursAgoArticle = feedOfDate("2024-12-28T07:00:00Z");

        SyndEntryImpl futureArticle = feedOfDate("2024-12-29T07:00:01Z");
        SyndEntryImpl yesterdayArticle = feedOfDate("2024-12-28T10:30:01Z");

        syndFeed(twentyFourHoursAgoPlusOneArticle, twentyFourHoursAgoArticle, futureArticle, yesterdayArticle);

        List<SyndEntry> result = rssParser.articlesFilteredByDate(
                syndFeed(twentyFourHoursAgoPlusOneArticle, twentyFourHoursAgoArticle, futureArticle, yesterdayArticle)
                ).toList();

        assertThat(result.stream().map(e -> e.getPublishedDate()))
                .containsExactly(twentyFourHoursAgoPlusOneArticle.getPublishedDate(), yesterdayArticle.getPublishedDate());
    }

    private static SyndFeed syndFeed(SyndEntry... entries) {
        SyndFeedImpl syndFeed = new SyndFeedImpl();
        syndFeed.setEntries(Arrays.asList(entries));
        return syndFeed;
    }

    @NotNull
    private static SyndEntryImpl feedOfDate(String instantString) {
        SyndEntryImpl feed = new SyndEntryImpl();
        feed.setPublishedDate(Date.from(Instant.parse(instantString)));
        return feed;
    }

}