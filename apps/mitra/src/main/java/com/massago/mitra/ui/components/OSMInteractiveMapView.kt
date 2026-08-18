package com.massago.mitra.ui.components

import android.annotation.SuppressLint
import android.webkit.JavascriptInterface
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun OSMInteractiveMapView(
    modifier: Modifier = Modifier,
    initialLat: Double = -6.2088,
    initialLng: Double = 106.8456,
    initialZoom: Int = 15,
    isOnline: Boolean = true,
    clientLat: Double? = null,
    clientLng: Double? = null
) {
    val context = LocalContext.current

    val webView = remember {
        WebView(context).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.cacheMode = WebSettings.LOAD_DEFAULT
            webViewClient = WebViewClient()
        }
    }

    val htmlContent = remember(initialLat, initialLng, initialZoom, isOnline, clientLat, clientLng) {
        """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="utf-8" />
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
            <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />
            <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
            <style>
                html, body, #map { height: 100%; width: 100%; margin: 0; padding: 0; background: #e2e8f0; }
                .pulse-radar {
                    width: 22px;
                    height: 22px;
                    background: #10B981;
                    border: 3px solid #ffffff;
                    border-radius: 50%;
                    box-shadow: 0 0 12px rgba(16, 185, 129, 0.7);
                }
            </style>
        </head>
        <body>
            <div id="map"></div>
            <script>
                var map = L.map('map', {
                    zoomControl: false,
                    attributionControl: false
                }).setView([$initialLat, $initialLng], $initialZoom);

                // CartoDB Voyager High-Resolution OpenStreetMap Tiles
                L.tileLayer('https://{s}.basemaps.cartocdn.com/rastertiles/voyager/{z}/{x}/{y}{r}.png', {
                    maxZoom: 19,
                    subdomains: 'abcd'
                }).addTo(map);

                // Mitra Scooter Pin
                var pulseIcon = L.divIcon({
                    className: 'pulse-radar',
                    iconSize: [22, 22],
                    iconAnchor: [11, 11]
                });
                var myMarker = L.marker([$initialLat, $initialLng], {icon: pulseIcon}).addTo(map).bindPopup("🛵 Posisi Anda (Mitra)");

                ${if (isOnline) """
                    L.circle([$initialLat, $initialLng], {
                        color: '#10B981',
                        fillColor: '#10B981',
                        fillOpacity: 0.12,
                        radius: 2500
                    }).addTo(map);
                """ else ""}

                ${if (clientLat != null && clientLng != null) """
                    var clientMarker = L.marker([$clientLat, $clientLng]).addTo(map).bindPopup("📍 Alamat Pelanggan");
                    var route = L.polyline([[$initialLat, $initialLng], [$clientLat, $clientLng]], {color: '#10B981', weight: 6, opacity: 0.85}).addTo(map);
                """ else ""}
            </script>
        </body>
        </html>
        """.trimIndent()
    }

    LaunchedEffect(htmlContent) {
        webView.loadDataWithBaseURL("https://leafletjs.com", htmlContent, "text/html", "UTF-8", null)
    }

    AndroidView(
        factory = { webView },
        modifier = modifier.fillMaxSize()
    )

    DisposableEffect(Unit) {
        onDispose {
            webView.destroy()
        }
    }
}
