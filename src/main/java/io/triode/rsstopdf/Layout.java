package io.triode.rsstopdf;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.context.Context;

import java.io.*;
import java.util.*;

public class Layout {

    private final Template template;
    private final VelocityContext velocityContext;
    private final List<Map<String, String>> newsItems = new ArrayList<>();

    public Layout() {
        VelocityEngine velocityEngine = new VelocityEngine();
        velocityEngine.setProperty("file.resource.loader.path", "src/main/resources");
        velocityEngine.init();

        // Load the LaTeX template
        template = velocityEngine.getTemplate("layout.vm");
        velocityContext = new VelocityContext();
        velocityContext.put("newsItems", newsItems);
    }

    public String renderContext(Context context) {
        StringWriter writer = new StringWriter();
        // Render the template
        template.merge(context, writer);

        return writer.toString();
    }

    public void addArticle(String title, String description, String source) {
        Map<String, String> newsItem = new HashMap<>();

        newsItem.put("title", escapeAllChars(title));
//		newsItem.put("date", article);

        newsItem.put("content", escapeAllChars(description));
        newsItem.put("source", escapeAllChars(source));

        newsItems.add(newsItem);
    }

    public void renderWriter(Writer writer) {
        template.merge(velocityContext, writer);
    }

    public static void main(String[] args) {
        // Prepare the data for rendering
        List<Map<String, String>> newsItems = new ArrayList<>();

        Map<String, String> news1 = new HashMap<>();
        news1.put("title", "Breaking News");
//		news1.put("imageFilePath", "images/news1.png");
        news1.put("date", "2024-11-21");
        news1.put("content", "This is the content of the first news item.");

        Map<String, String> news2 = new HashMap<>();
        news2.put("title", "Technology Update");
//		news2.put("imageFilePath", "images/news2.png");
        news2.put("date", "2024-11-20");
        news2.put("content", "This is the content of the second news item.");

        newsItems.add(news1);
        newsItems.add(news2);

        // Create the Velocity context
        VelocityContext context = new VelocityContext();
        context.put("newsItems", newsItems);

        // Render the template
        String result = new Layout().renderContext(context);

        // Output the rendered LaTeX document
        System.out.println(result);

        // Optionally, save to a file
        try (BufferedWriter fileWriter = new BufferedWriter(new FileWriter("output.tex"))) {
            fileWriter.write(result);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Escapes LaTeX chars.
     *
     * @param input the String
     * @return the String with escaped chars
     */
    public static String escapeAllChars(String input) {
        String quotedLatexFile = input
                .replace("\\", "\\textbackslash ")
                .replace("#", "\\#")
                .replace("$", "\\$")
                .replace("%", "\\%")
                .replace("_", "\\_")
                .replace("&", "\\&")
                .replace("{", "\\{")
                .replace("}", "\\}")
                .replace("~", "\\textasciitilde ")
                .replace("^", "\\textasciicircum ");

        // Regenerate the escaped LaTeX string
        return JSoup.unquoteLatexTag(quotedLatexFile);
    }
}
