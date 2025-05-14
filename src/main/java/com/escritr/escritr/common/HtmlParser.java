package com.escritr.escritr.common;

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
        return Jsoup.clean(input,safe());
    }

    public static Safelist safe() {
        return Safelist.relaxed()
                .addAttributes("img", "src", "alt", "title")
                .addProtocols("img", "src", "http", "https")
                .addAttributes("span", "data-type", "data-id", "data-label")
                .addAttributes("a", "href", "title", "target", "rel")
                .addProtocols("a", "href", "http", "https", "mailto");
//        return Safelist.basicWithImages()
//                .addTags("u", "s", "code", "pre")
//                // Attributes for code highlighting
//                .addAttributes("code", "class")
//                // Attributes for images
//                .addAttributes("img", "src", "alt", "title")
//                .addProtocols("img", "src", "http", "https")
//                //for mentions or custom nodes
//                .addAttributes("span", "data-type", "data-id", "data-label")
//                .addTags("span");
    }

}
