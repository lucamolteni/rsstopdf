package io.triode.rsstopdf;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JSoupTest {

    @Test
    public void quoteUnquoteLatexTag() {
        var input = JSoup.generateQuotedLatextTag("img/ireland", "alt-text");
        var unquote = JSoup.unquoteLatexTag(input);

        assertEquals("\\image{img/ireland}{alt-text}", unquote);
    }

}