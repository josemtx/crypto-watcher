package es.ulpgc.datos.application;

import es.ulpgc.datos.domain.CryptoPrice;

import java.util.List;

public interface CryptoPriceStore {
    void save(List<CryptoPrice> prices);
}