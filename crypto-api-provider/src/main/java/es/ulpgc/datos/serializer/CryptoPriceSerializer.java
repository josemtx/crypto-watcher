package es.ulpgc.datos.serializer;

import es.ulpgc.datos.model.CryptoPrice;

import java.util.List;

public interface CryptoPriceSerializer {
    void save(List<CryptoPrice> prices);
}