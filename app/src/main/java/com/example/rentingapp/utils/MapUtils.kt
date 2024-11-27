package com.example.rentingapp.utils

import android.content.Context
import androidx.core.content.ContextCompat
import com.example.rentingapp.R
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon
import java.util.Random

object MapUtils {

    fun createMarker(mapView: MapView, location: GeoPoint, title: String): Marker {
        return Marker(mapView).apply {
            position = location
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            this.title = title
        }
    }

    fun createCircleOverlay(context: Context, center: GeoPoint, radius: Float, colorResId: Int): Polygon {
        val points = ArrayList<GeoPoint>()
        for (i in 0..360 step 10) {
            val point = center.destinationPoint(radius.toDouble(), i.toDouble())
            points.add(point)
        }

        return Polygon().apply {
            this.points = points
            this.fillColor = ContextCompat.getColor(context, colorResId)
            this.strokeColor = ContextCompat.getColor(context, colorResId)
            this.strokeWidth = 2f
        }
    }

    fun createRandomCircle(center: GeoPoint): Polygon {
        val random = Random()
        val radius = 100.0 + random.nextDouble() * 50.0 // Random radius between 100m and 150m
        val offsetLat = random.nextDouble() * 0.001 - 0.0005 // Random offset within ~55m
        val offsetLon = random.nextDouble() * 0.001 - 0.0005

        val offsetCenter = GeoPoint(center.latitude + offsetLat, center.longitude + offsetLon)

        val points = ArrayList<GeoPoint>()
        for (i in 0..360 step 10) {
            val point = offsetCenter.destinationPoint(radius, i.toDouble())
            points.add(point)
        }

        return Polygon().apply {
            this.points = points
            this.fillColor = android.graphics.Color.argb(80, 255, 165, 0) // Semi-transparent orange
            this.strokeColor = android.graphics.Color.rgb(255, 165, 0) // Solid orange
            this.strokeWidth = 2f
        }
    }

    fun isLocationWithinRadius(center: GeoPoint, location: GeoPoint, radius: Float): Boolean {
        val distance = center.distanceToAsDouble(location)
        return distance <= radius
    }

    fun clearMapMarkers(mapView: MapView, markers: Collection<Polygon>) {
        markers.forEach { mapView.overlays.remove(it) }
        mapView.invalidate()
    }

    fun setupMap(mapView: MapView, userLocation: GeoPoint, context: Context) {
        Configuration.getInstance().userAgentValue = context.packageName
        
        mapView.apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(15.0)
        }

        // Center map on user's location
        mapView.controller.setCenter(userLocation)

        // Add marker for user's location
        val userMarker = Marker(mapView).apply {
            position = userLocation
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            title = "Your Location"
        }
        mapView.overlays.add(userMarker)
    }

    fun updateCircleOverlay(mapView: MapView, radiusOverlay: Polygon?, userLocation: GeoPoint, radius: Float, context: Context): Polygon {
        // Remove existing circle overlay
        radiusOverlay?.let { mapView.overlays.remove(it) }

        // Create new circle overlay
        val newOverlay = createCircleOverlay(context, userLocation, radius, R.color.blue_main_20)
        mapView.overlays.add(newOverlay)
        mapView.invalidate()

        return newOverlay
    }
}
