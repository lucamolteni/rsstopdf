package io.triode.rsstopdf;

import net.dankito.readability4j.Article;
import net.dankito.readability4j.Readability4J;
import org.apache.commons.io.FilenameUtils;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

public class RSSContent {

    private final HttpFetch httpFetch;

    public RSSContent(HttpFetch httpFetch) {
        this.httpFetch = httpFetch;
    }

    public RssParser.Article refetchContentIfTooSmall(RssParser.Article article) {
        String articleBody = article.body();
        String link = article.link();

        if (articleBody == null || articleBody.length() < 5000) {
            Optional<HttpResponse<String>> stringHttpResponse = httpFetch.getURLFollowingRedirects(link);

            articleBody = stringHttpResponse
                    .map(HttpResponse::body)
                    .orElse(articleBody);
        }

        return new RssParser.Article(article.outline(), article.changeBody(articleBody));
    }

    public RssParser.Article cleanContent(RssParser.Article a) {
        String url = a.link();
        Readability4J readability4J = new Readability4J(
                url,
                a.body()
        ); // url is just needed to resolve relative urls
        Article article = readability4J.parse();

        Element contentHtml = article.getArticleContent();

        Elements images = contentHtml.getElementsByTag("img");

        for (Element img : images) {
            String src = img.attr("src");
            a.articleImages().add(new RssParser.ArticleImage(src));
        }

        String textWithImages = new JSoup(contentHtml).textWithImages(FilenameUtils.getName(a.title()));

        return new RssParser.Article(a.outline(), a.changeBody(textWithImages), a.articleImages());
    }

    public RssParser.ArticleImage addImageToArticle(RssParser.ArticleImage article, String url) {
        Optional<HttpResponse<byte[]>> imageByte = httpFetch.getURLFollowingRedirectsByte(url);
        String fileName = findImageFileNameFromUrl(url);

        return imageByte
                .map(httpResponse ->
                        new RssParser.ArticleImage(
                        httpResponse.body(),
                        fileName
                )).orElse(article);
    }

    public static String findImageFileNameFromUrl(String url) {
        String path = "";
        try {
            URI uri = new URI(url);
            path = uri.getPath();

            Path nioPath = Paths.get(path);
            return nioPath.getFileName().toString();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
}
