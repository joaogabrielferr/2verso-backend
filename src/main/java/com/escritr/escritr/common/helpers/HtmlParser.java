package com.escritr.escritr.common.helpers;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.safety.Safelist;
import org.jsoup.select.Elements;

public class HtmlParser {

    public static String extractFirstParagraph(String _input){

        String input = Jsoup.clean(_input,safe());

        if(input.isBlank())return "";

        Document document = Jsoup.parse(input);
        Elements candidates = document.select("p,h1,h2");
        for(Element e : candidates){
            String text = e.text().trim();
            if(!text.isEmpty())return text;
        }

        return "";
    }

    public static String cleanContent(String input){
        if (input == null) {
            return "";
        }
        return Jsoup.clean(input,safe());
    }

    public static Safelist safe() {
        return Safelist.relaxed()
                .addAttributes("img", "src", "alt", "title")
                .addProtocols("img", "src", "http", "https")
                .addAttributes("span", "data-type", "data-id", "data-label")
                .addAttributes("a", "href", "title", "target", "rel")
                .addProtocols("a", "href", "http", "https", "mailto")
                .addAttributes("span", "data-type", "data-id", "data-label");
    }

    public static String cleanNormalText(String titleInput) {
        // Remove all HTML tags, leaving only text content
        String plainText = Jsoup.clean(titleInput, Safelist.none());
        return plainText.trim().replaceAll("\\s+", " ");
    }

}
