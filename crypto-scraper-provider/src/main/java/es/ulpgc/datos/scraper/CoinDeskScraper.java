package es.ulpgc.datos.scraper;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import es.ulpgc.datos.model.NewsItem;
import es.ulpgc.datos.database.DatabaseManager;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class CoinDeskScraper {

    private static final String URL = "https://www.coindesk.com/";
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36";
    private static final DatabaseManager dbManager = new DatabaseManager(); // Instancia global

    public static void main(String[] args) {
        System.out.println("Iniciando el servicio de scraping de CoinDesk...");

        // Creamos un planificador de tareas
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

        // Programamos la tarea: ejecutar runScrapingTask() ahora mismo, y luego cada 1 hora
        scheduler.scheduleAtFixedRate(CoinDeskScraper::runScrapingTask, 0, 1, TimeUnit.HOURS);
    }

    private static void runScrapingTask() {
        System.out.println("\n[" + LocalDateTime.now() + "] Ejecutando captura de noticias...");
        try {
            Document doc = fetchDocument();
            List<NewsItem> newsList = extractNews(doc);

            dbManager.insertNews(newsList);

        } catch (IOException e) {
            System.err.println("Error de conexión al realizar el scraping: " + e.getMessage());
        }
    }

    private static Document fetchDocument() throws IOException {
        return Jsoup.connect(CoinDeskScraper.URL).userAgent(USER_AGENT).get();
    }

    private static List<NewsItem> extractNews(Document doc) {
        List<NewsItem> newsList = new ArrayList<>();
        Elements newsLinks = doc.select("a:has(h2), a:has(h3), a:has(h4), a:has(h5), a:has(h6)");

        for (Element link : newsLinks) {
            String title = link.text();
            String articleUrl = link.absUrl("href");

            if (isValidNewsLink(title, articleUrl)) {
                newsList.add(new NewsItem(title, articleUrl, LocalDateTime.now()));
            }
        }
        return newsList;
    }

    private static boolean isValidNewsLink(String title, String url) {
        return !title.isEmpty() && url.startsWith("https://www.coindesk.com/");
    }
}