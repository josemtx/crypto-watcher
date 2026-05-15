package es.ulpgc.datos.infrastructure;

import es.ulpgc.datos.domain.NewsFeeder;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;
import es.ulpgc.datos.domain.NewsArticle;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class CoinDeskFeeder implements NewsFeeder {
    // Usamos el endpoint oficial de sindicación (RSS) diseñado para consumo automatizado
    private static final String RSS_URL = "https://www.coindesk.com/arc/outboundfeeds/rss/?outputType=xml";

    @Override
    public List<NewsArticle> fetchNews() {
        try {
            // Le indicamos a Jsoup que procese la respuesta como XML en lugar de HTML
            Document doc = Jsoup.connect(RSS_URL)
                    .parser(Parser.xmlParser())
                    .timeout(10000)
                    .get();

            return extractNews(doc);
        } catch (IOException e) {
            System.err.println("Error de conexión al leer el RSS: " + e.getMessage());
            return List.of();
        }
    }

    private List<NewsArticle> extractNews(Document doc) {
        List<NewsArticle> newsList = new ArrayList<>();
        // En formato RSS, cada noticia viene envuelta en una etiqueta <item>
        Elements items = doc.select("item");
        Instant capturedAt = Instant.now();

        for (Element item : items) {
            // Extraemos los datos directamente de las subetiquetas
            String title = item.select("title").text();
            String articleUrl = item.select("link").text();

            if (isValidNewsLink(title, articleUrl)) {
                newsList.add(new NewsArticle(title, articleUrl, capturedAt));
            }
        }
        return newsList;
    }

    private boolean isValidNewsLink(String title, String url) {
        return title != null && !title.isEmpty() && url != null && !url.isEmpty();
    }
}