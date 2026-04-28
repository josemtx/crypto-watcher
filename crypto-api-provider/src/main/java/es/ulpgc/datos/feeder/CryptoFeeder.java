package es.ulpgc.datos.feeder;

import es.ulpgc.datos.model.CryptoPrice;

import java.util.List;

public interface CryptoFeeder {
    List<CryptoPrice> fetchPrices();
}