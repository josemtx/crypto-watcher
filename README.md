# Market Intelligence: Crypto Analytics Platform

Plataforma de inteligencia de mercado basada en una **Arquitectura Orientada a Eventos (Kappa)**. El sistema ingiere, procesa y cruza datos de cotizaciones de criptomonedas y análisis de sentimiento de noticias financieras para ofrecer señales de inversión en tiempo real.

## 🎯 Objetivo de la Funcionalidad de Negocio

El objetivo principal de esta plataforma es resolver la asimetría de información en el mercado de criptomonedas proporcionando al usuario una **Señal de Mercado accionable (Favorable, Neutral o Riesgoso)**. 

Para lograrlo, el sistema abandona el análisis de precios aislado y ejecuta un cruce de datos en tiempo real entre:
1. **Métricas cuantitativas:** Volatilidad del precio, Volumen 24H y Ratio de Liquidez (Capitalización vs Volumen).
2. **Métricas cualitativas:** Volumen de impacto mediático y análisis de sentimiento (Bearish/Bullish) procesado mediante Procesamiento de Lenguaje Natural (NLP).

Esta combinación permite detectar de forma automatizada **Alertas de Hype** (euforia o pánico irracional), movimientos silenciosos de "ballenas" y momentos de alta tensión operativa.

## 🏗️ Arquitectura Final del Sistema

El proyecto está diseñado bajo los principios de **Clean Architecture (Puertos y Adaptadores)** para garantizar el desacoplamiento y alta mantenibilidad, dividiéndose en módulos independientes comunicados de forma asíncrona:

* **1. Capa de Ingesta (Feeders & Publishers):**
    * `crypto-api-provider`: Extrae cotizaciones (Precio, Volumen, Market Cap) desde la API REST de CoinGecko.
    * `crypto-scraper-provider`: Ingiere el feed RSS oficial de CoinDesk, limpia el contenido y evalúa el sentimiento usando la API de ApiNinjas.
* **2. Capa de Mensajería (Message Broker):**
    * **ActiveMQ:** Actúa como el bus central de eventos del sistema, canalizando los JSONs a través de *Topics* específicos (`CryptoNews`, precios, etc.).
* **3. Capa de Almacenamiento (Event Store):**
    * `event-store-builder`: Persiste todos los eventos crudos en archivos locales `.events`, garantizando la inmutabilidad de los datos y permitiendo la reconstrucción del estado del sistema en cualquier momento.
* **4. Capa de Negocio (Business Unit & Datamart):**
    * **Procesador de Eventos:** Escucha los mensajes en tiempo real y reconstruye el histórico.
    * **Datamart (SQLite):** Base de datos relacional optimizada con 4 tablas (`crypto_timeline`, `news_feed`, `market_hype_alerts`, `market_signal`) que consolida y precalcula los indicadores complejos.
* **5. Capa de Presentación (REST API & Dashboard):**
    * Servidor web embebido con **Javalin** que expone endpoints REST (`/api/timeline`, `/api/summary`, etc.).
    * Frontend SPA (*Single Page Application*) construido con Vanilla JS, CSS3 (estilo *Glassmorphism* corporativo) y **Chart.js** para visualización reactiva.

## 🚀 Cómo ejecutar cada componente y probar la interfaz

Para levantar la arquitectura End-to-End (E2E), se deben seguir estos pasos en orden para respetar el flujo de datos:

### Prerrequisitos
* Java 21 o superior.
* Apache ActiveMQ ejecutándose localmente en el puerto `61616`.
* Variable de entorno configurada: `API_NINJAS_KEY` con tu clave de acceso.

### Orden de Ejecución

1.  **Iniciar el Broker:** Asegúrate de que Apache ActiveMQ está arrancado.
2.  **Iniciar Event Store:** Ejecuta el `Main` del módulo `event-store-builder` para habilitar el guardado del historial.
3.  **Iniciar los Providers (Scrapers):**
    * Ejecuta el `Main` de `crypto-api-provider`.
    * Ejecuta el `Main` de `crypto-scraper-provider`.
4.  **Iniciar la Business Unit:**
    * Ejecuta el `Main` del módulo principal de negocio. Este proceso creará/leerá la base de datos `datamart.db`, procesará todos los eventos y levantará automáticamente el servidor web de Javalin.
5.  **Probar la Interfaz:**
    * Abre tu navegador web y navega a: [http://localhost:8080](http://localhost:8080)
    * Selecciona la criptomoneda en el menú desplegable superior derecho para visualizar la telemetría cruzada y el termómetro de mercado en vivo.
