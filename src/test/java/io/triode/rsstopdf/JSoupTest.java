package io.triode.rsstopdf;

import net.dankito.readability4j.Article;
import net.dankito.readability4j.Readability4J;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JSoupTest {

    @Test
    public void quoteUnquoteLatexTag() {
        var input = JSoup.generateQuotedLatextTag("img/ireland", "alt-text");
        var unquote = JSoup.unquoteLatexTag(input);

        assertEquals("\\image{img/ireland}{alt-text}", unquote);
    }

    @Test
    public void testHTMLNewlinesShouldInsertABlankLineForLatex() {

        String contentHtml = "<div><p>First line</p><p>Second<br />line</p></div><div>Third Line</div>";

        Readability4J readability4J = new Readability4J(
                "", // url is not needed for tests
                contentHtml
        );
        Article article = readability4J.parse();
        String transformed = new JSoup(article.getArticleContent()).textWithImages("");

        assertEquals("First line \n\nSecond\n\nline \n\n Third Line", transformed);
    }

}