package io.triode.rsstopdf;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RSSContentTest {

    @Test
    public void testFindImageFileNameFromUrl() {
        var url = "https://images.macrumors.com/article-new/2024/10/iPhone-17-Slim-Feature.jpg";
        var result = RSSContent.findImageFileNameFromUrl(url);

        assertEquals("iPhone-17-Slim-Feature.jpg", result);
    }

}