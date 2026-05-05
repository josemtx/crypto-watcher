// Archivo: CoinDeskFeeder.java
package es.ulpgc.datos.feeder;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import es.ulpgc.datos.model.NewsArticle;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class CoinDeskFeeder implements NewsFeeder {
    private static final String URL = "https://www.coindesk.com/";
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36";

    @Override
    public List<NewsArticle> fetchNews() {
        try {
            Document doc = Jsoup.connect(URL).userAgent(USER_AGENT).get();
            return extractNews(doc);
        } catch (IOException e) {
            System.err.println("Error de conexión al realizar el scraping: " + e.getMessage());
            return List.of(); // Devolvemos lista vacía para que el programa no crashee
        }
    }

    private List<NewsArticle> extractNews(Document doc) {
        List<NewsArticle> newsList = new ArrayList<>();
        Elements newsLinks = doc.select("a:has(h2), a:has(h3), a:has(h4), a:has(h5), a:has(h6)");
        Instant capturedAt = Instant.now(); // Capturamos el momento exacto para todas las noticias

        for (Element link : newsLinks) {
            String title = link.text();
            String articleUrl = link.absUrl("href");

            if (isValidNewsLink(title, articleUrl)) {
                newsList.add(new NewsArticle(title, articleUrl, capturedAt));
            }
        }
        return newsList;
    }

    private boolean isValidNewsLink(String title, String url) {
        return !title.isEmpty() && url.startsWith("https://www.coindesk.com/");
    }
}