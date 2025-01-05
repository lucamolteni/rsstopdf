package io.triode.rsstopdf;

import net.dankito.readability4j.Article;
import net.dankito.readability4j.Readability4J;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JSoupTest {

    @Test
    public void quoteUnquoteLatexTag() {
        var input = JSoup.generateQuotedImgLatextTag("image", "img/ireland", "alt-text");
        var unquote = JSoup.unquoteLatexTag(input);

        assertEquals("\\image{img/ireland}{alt-text}", unquote);
    }

    @Test
    public void testHTMLNewlinesShouldInsertABlankLineForLatex() {

        String contentHtml = "<div><p>First line</p><p>Second<br />line</p></div><div>Third Line</div>";
        String transformed = processHTML(contentHtml);

        assertEquals("First line \n\nSecond\n\nline \n\n Third Line", transformed);
    }

    @Test
    public void testHTMLHRefShouldBeConvertedInLatexLink() {

        String contentHtml = "Download Here: <a href=\"https://example.com\">https://example.com</a>";

        String transformed = processHTML(contentHtml);
        var unquote = JSoup.unquoteLatexTag(transformed);

        assertEquals("Download Here: \\href{https://example.com}{link}", unquote);
    }

    @Test
    public void testHTMLHRefShouldBeConvertedInLatexLinkWithProperLinkName() {

        String contentHtml = "Download Here: <a href=\"https://example.com\">This is a link</a>";

        String transformed = processHTML(contentHtml);
        var unquote = JSoup.unquoteLatexTag(transformed);

        assertEquals("Download Here: \\href{https://example.com}{This is a link}", unquote);
    }

    private static String processHTML(String contentHtml) {
        Readability4J readability4J = new Readability4J(
                "", // url is not needed for tests
                contentHtml
        );
        Article article = readability4J.parse();
        return new JSoup(article.getArticleContent()).textWithImages("");
    }

}