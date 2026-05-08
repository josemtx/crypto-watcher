package es.ulpgc.datos.domain;

import java.util.List;

public interface CryptoFeeder {
    List<CryptoPrice> fetchPrices();
}