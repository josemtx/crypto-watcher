# Real-Time Market Intelligence & Crypto Analytics Platform

Plataforma distribuida de inteligencia de mercados en tiempo real basada en **Arquitectura Orientada a Eventos (EDA)** y el **patrón Kappa**. El sistema ingesta, normaliza y procesa de forma concurrente flujos de cotizaciones financieras y métricas de sentimiento (NLP) para computar señales algorítmicas de inversión y detectar anomalías (*Hype / Market corrections*) con baja latencia.

---

## Arquitectura del Sistema

El núcleo opera mediante un pipeline de datos inmutable y desacoplado a través de tópicos de mensajería:

### Principios y Patrones Clave

* **Arquitectura Kappa:** Los datos se tratan como una secuencia ordenada de eventos inmutables. El histórico completo reside en logs secuenciales (`.events`), lo que permite reprocesar la lógica de negocio y reconstruir el estado analítico (*Datamart*) ante caídas o cambios algorítmicos sin pérdida de datos.
* **Arquitectura Hexagonal (Ports & Adapters):** Desacoplamiento estricto de dominio e infraestructura en los módulos de ingesta (`NewsFeeder`, `SentimentAnalyzer`), permitiendo cambiar proveedores externos de red (APIs, RSS) sin alterar las reglas de negocio.
* **Desacoplamiento Espaciotemporal:** Ingesta y consumo asíncronos mediante **Apache ActiveMQ** (Publish-Subscribe).

---

## Componentes Multi-Módulo (Maven)

El proyecto está organizado en módulos independientes con responsabilidades delimitadas:

```text
crypto-watcher/
├── crypto-api-provider/       # Polling concurrente a CoinGecko REST API y publicación en broker
├── crypto-scraper-provider/   # Ingesta sindicada (CoinDesk RSS) + Análisis NLP y scoring (-1.0 a 1.0)
├── event-store-builder/       # Consumidor asíncrono y persistencia inmutable en logs planos (.events)
└── business-unit/             # Consumidor analítico, motor de agregaciones, Datamart y API REST (Javalin)

```

---

## Motor de Señales de Negocio

El módulo `business-unit` cruza en ventanas temporales horarias variables cuantitativas y cualitativas para derivar el estado analítico de cada activo:

$$\text{Liquidity Ratio} = \frac{\text{Market Cap}}{\text{Volume}_{24h}}$$

* **Detección de Volatilidad:** Cálculo de dispersión de precios en ventanas deslizantes para evaluar riesgo intra-horario.
* **Alertas de Hype:** Activación algorítmica cuando la volatilidad supera el umbral estadístico en simultaneidad con anomalías de volumen informativo en medios.
* **Señal de Inversión:** Clasificación determinista (*Favorable*, *Neutral*, *Riesgoso*) cruzando sentimiento agregado y tendencia de volumen.

---

## Datamart y Persistencia

La capa analítica expone consultas optimizadas a través de un Datamart local transaccional (SQLite) estructurado en cuatro tablas normalizadas:

| Tabla | Propósito | Granularidad |
| --- | --- | --- |
| `crypto_timeline` | Precios OHLC simplificados, volumen 24h y capitalización | Ventana horaria / Activo |
| `news_feed` | Registro crudo indexado de noticias, fuentes y sesgo NLP | Evento individual |
| `market_hype_alerts` | Agregaciones de volumen informativo y score de sentimiento promedio | Ventana horaria / Global |
| `market_signal` | Estado de señal consolidado consumible directamente por clientes web | Estado actual / Activo |

---

## Interfaz REST (API Endpoints)

El servidor ligero embebido (**Javalin**) expone los siguientes endpoints en el puerto `8080`:

* **`GET /api/coins`** → Lista de identificadores de criptomonedas monitoreadas activamente.
* **`GET /api/timeline?coin={coin_id}`** → Histórico de ventanas temporales (precios de cierre, mínimos y máximos).
* **`GET /api/summary?coin={coin_id}`** → Resumen consolidado del estado de mercado y señales:

```json
{
  "coinId": "ethereum",
  "signal": "Neutral",
  "volatilityRatio": 0.0045,
  "volatilityLevel": "Baja",
  "hypeWarning": false,
  "newsVolume": 50,
  "averageSentimentScore": 0.02,
  "sentimentLabel": "Neutral",
  "volume_24h": 17764855403.0,
  "market_cap": 268997571248.0
}

```

---

## Puesta en Marcha

### Prerrequisitos

* Java Development Kit (JDK) 21
* Maven 3.8+
* Instancia local de Apache ActiveMQ corriendo en `tcp://localhost:61616`
* Clave de API configurada en entorno:
```bash
export API_NINJAS_KEY="tu_api_key"

```



### Compilación y Ejecución

1. **Compilar el proyecto multi-módulo:**
```bash
mvn clean install

```


2. **Orden de inicio de servicios:**
1. Iniciar broker Apache ActiveMQ.
2. Levantar el almacenamiento de logs: ejecutar `Main` en `event-store-builder`.
3. Iniciar el motor analítico y API: ejecutar `Main` en `business-unit`.
4. Iniciar los agentes de ingesta: ejecutar `Main` en `crypto-api-provider` y `crypto-scraper-provider`.
