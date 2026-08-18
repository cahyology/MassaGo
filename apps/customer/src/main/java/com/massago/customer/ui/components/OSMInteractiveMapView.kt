package com.massago.customer.ui.components

import android.annotation.SuppressLint
import android.content.Context
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
    initialZoom: Int = 16,
    showCenterPin: Boolean = true,
    therapistLat: Double? = null,
    therapistLng: Double? = null,
    onLocationMoved: ((Double, Double) -> Unit)? = null
) {
    val context = LocalContext.current

    val webView = remember {
        WebView(context).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.cacheMode = WebSettings.LOAD_DEFAULT
            webViewClient = WebViewClient()

            addJavascriptInterface(object {
                @JavascriptInterface
                fun onCenterChanged(lat: Double, lng: Double) {
                    onLocationMoved?.invoke(lat, lng)
                }
            }, "AndroidBridge")
        }
    }

    val htmlContent = remember(initialLat, initialLng, initialZoom, showCenterPin) {
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
                .center-pin {
                    position: absolute;
                    top: 50%;
                    left: 50%;
                    transform: translate(-50%, -100%);
                    z-index: 1000;
                    pointer-events: none;
                }
                .pulse-radar {
                    width: 20px;
                    height: 20px;
                    background: #10B981;
                    border: 3px solid #ffffff;
                    border-radius: 50%;
                    box-shadow: 0 0 10px rgba(16, 185, 129, 0.6);
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

                // CartoDB Voyager High-Res Clean Tiles (Free & Fast in Indonesia)
                L.tileLayer('https://{s}.basemaps.cartocdn.com/rastertiles/voyager/{z}/{x}/{y}{r}.png', {
                    maxZoom: 19,
                    subdomains: 'abcd'
                }).addTo(map);

                // Add current GPS marker
                var pulseIcon = L.divIcon({
                    className: 'pulse-radar',
                    iconSize: [20, 20],
                    iconAnchor: [10, 10]
                });
                var myMarker = L.marker([$initialLat, $initialLng], {icon: pulseIcon}).addTo(map);

                ${if (therapistLat != null && therapistLng != null) """
                    var therapistMarker = L.marker([$therapistLat, $therapistLng]).addTo(map).bindPopup("🛵 Terapis");
                    var route = L.polyline([[$therapistLat, $therapistLng], [$initialLat, $initialLng]], {color: '#10B981', weight: 5, opacity: 0.8}).addTo(map);
                """ else ""}

                map.on('moveend', function() {
                    var center = map.getCenter();
                    if (window.AndroidBridge) {
                        window.AndroidBridge.onCenterChanged(center.lat, center.lng);
                    }
                });

                function recenter(lat, lng) {
                    map.setView([lat, lng], 17);
                    myMarker.setLatLng([lat, lng]);
                }
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
