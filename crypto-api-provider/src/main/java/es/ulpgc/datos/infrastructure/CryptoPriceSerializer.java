package es.ulpgc.datos.infrastructure;

import es.ulpgc.datos.domain.CryptoPrice;

import java.util.List;

public interface CryptoPriceSerializer {
    void save(List<CryptoPrice> prices);
}