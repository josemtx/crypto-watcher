package es.ulpgc.datos.datamart;

public class MarketSignalCalculator {
    private static final double HIGH_VOLATILITY_THRESHOLD = 0.02;
    private static final int HYPE_NEWS_THRESHOLD = 5;

    public double calculateVolatilityRatio(double minPrice, double maxPrice) {
        if (minPrice <= 0.0) {
            return 0.0;
        }
        return (maxPrice - minPrice) / minPrice;
    }

    public boolean calculateHypeWarning(double volatilityRatio, int newsVolume) {
        return volatilityRatio > HIGH_VOLATILITY_THRESHOLD && newsVolume > HYPE_NEWS_THRESHOLD;
    }

    public String calculateSignal(double volatilityRatio,
                                  int newsVolume,
                                  double averageSentimentScore,
                                  boolean hypeWarning) {
        boolean highVolatility = volatilityRatio > HIGH_VOLATILITY_THRESHOLD;
        boolean lowOrMediumVolatility = volatilityRatio <= HIGH_VOLATILITY_THRESHOLD;
        boolean positiveSentiment = averageSentimentScore > 0.1;
        boolean negativeSentiment = averageSentimentScore < -0.1;
        boolean manyNews = newsVolume > HYPE_NEWS_THRESHOLD;

        if (highVolatility || negativeSentiment || (manyNews && highVolatility)) {
            return "Riesgoso";
        }

        if (lowOrMediumVolatility && positiveSentiment && !hypeWarning) {
            return "Favorable";
        }

        return "Neutral";
    }
}