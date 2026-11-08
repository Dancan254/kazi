package com.kazi.kazi.ingestion.normalization;

import java.util.stream.Collectors;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.parser.Parser;

public final class HtmlTextNormalizer {

    private static final String BLOCK_ELEMENTS =
        "address, article, aside, blockquote, div, footer, h1, h2, h3, h4, h5, h6, header, "
            + "li, main, nav, ol, p, section, table, tr, ul";

    private HtmlTextNormalizer() {
    }

    public static String toPlainText(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }
        Document document = Jsoup.parse(Parser.unescapeEntities(value, true));
        document.select("script, style, noscript").remove();
        document.select("br").forEach(element -> element.after("\n"));
        document.select("li").forEach(element -> element.prepend("- "));
        document.select(BLOCK_ELEMENTS).forEach(element -> element.append("\n"));
        return document.wholeText().lines()
            .map(line -> line.replaceAll("[\\s\\p{Z}]+", " ").trim())
            .filter(line -> !line.isEmpty())
            .collect(Collectors.joining("\n"));
    }
}
