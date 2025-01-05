package io.triode.rsstopdf;

import org.jsoup.helper.StringUtil;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.NodeTraversor;
import org.jsoup.select.NodeVisitor;
import org.tinylog.Logger;

import static io.triode.rsstopdf.RSSContent.findImageFileNameFromUrl;

/* This is copied from the original JSoup library from the Node class and edited to parse only the images from the HTML
 * The original JSoup library is licensed under the MIT License https://github.com/jhy/jsoup/blob/master/LICENSE
 * The original JSoup library can be found at https://github.com/jhy/jsoup */
public class JSoup {

    Node node;
    private String LATEX_NEW_PARAGRAPH = "\n\n";

    public JSoup(Node node) {
        this.node = node;
    }

    public String textWithImages(String articleName) {
        final StringBuilder output = new StringBuilder();
        NodeTraversor.traverse(new NodeVisitor() {
            @Override
            public void head(Node node, int depth) {
                if (node instanceof TextNode textNode) {
                    appendNormalisedText(output, textNode);
                } else if (node instanceof Element element) {
                    if ("img".equals(element.tagName())) {
                        transformImage(element, articleName, output);
                    } else if ("br".equals(element.tagName())) {
                        output.append(LATEX_NEW_PARAGRAPH);
                    } else if ("p".equals(element.tagName())) {
                        output.append(LATEX_NEW_PARAGRAPH);
                    } else if (output.length() > 0 &&
                            (element.isBlock() || "br".equals(element.tagName())) &&
                            !lastCharIsWhitespace(output)) {
                        output.append(' ');
                    }
                }
            }

            public void tail(Node node, int depth) {
                // Do nothing for tail
            }
        }, node);
        return output.toString().trim();
    }

    private void transformImage(Element element, String articleName, StringBuilder accum) {
        String imageOriginalSource = element.attr("src");
        Logger.info("Image original source: {}", imageOriginalSource);

        String imageFileName = findImageFileNameFromUrl(imageOriginalSource);

        if(imageFileName.isEmpty()) {
            Logger.info("Empty image source: " + element);
            return;
        }
        String withImgPrefix = "img/%s/%s".formatted(articleName, imageFileName);

        String alt = element.attr("alt");
        String imgPlaceholder = generateQuotedLatextTag(withImgPrefix, alt);

        accum.append(imgPlaceholder).append(' ');
    }

    private static final String BACKSLASH = "@@BACKSLASH@@";
    private static final String OPEN_BRACKET = "@@OPENBRACKET@@";
    private static final String CLOSE_BRACKET = "@@CLOSEBRACKET@@";

    // \image{img/ireland.jpg}
    public static String generateQuotedLatextTag(String src, String alt) {
        return "%simage%s%s%s%s%s%s"
                .formatted(
                        BACKSLASH,
                        OPEN_BRACKET,
                        src,
                        CLOSE_BRACKET,
                        OPEN_BRACKET,
                        alt,
                        CLOSE_BRACKET);
    }

    public static String unquoteLatexTag(String source) {
        return source
                .replace(BACKSLASH, "\\")
                .replace(OPEN_BRACKET, "{")
                .replace(CLOSE_BRACKET, "}");
    }


    public static void appendNormalisedText(StringBuilder accum, TextNode textNode) {
        String text = textNode.getWholeText();
        if (preserveWhitespace(textNode.parentNode())) {
            accum.append(text);
        } else {
            StringUtil.appendNormalisedWhitespace(accum, text, lastCharIsWhitespace(accum));
        }

    }

    public static boolean lastCharIsWhitespace(StringBuilder sb) {
        return sb.length() != 0 && sb.charAt(sb.length() - 1) == ' ';
    }


    static boolean preserveWhitespace(Node node) {
        if (node != null && node instanceof Element) {
            Element el = (Element) node;
            int i = 0;

            do {
                if (el.tag().preserveWhitespace()) {
                    return true;
                }

                el = el.parent();
                ++i;
            } while (i < 6 && el != null);
        }

        return false;
    }
}
