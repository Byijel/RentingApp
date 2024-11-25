package com.example.rentingapp.ui.rentNearMe

import android.graphics.Color
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.rentingapp.R
import com.example.rentingapp.databinding.FragmentRentNearMeBinding
import com.google.android.material.slider.Slider
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon

class RentNearMe : Fragment() {
    private var _binding: FragmentRentNearMeBinding? = null
    private val binding get() = _binding!!
    private lateinit var mapView: MapView
    private lateinit var locationMarker: Marker
    private var radiusOverlay: Polygon? = null
    
    // Test location in Antwerp (Centraal Station)
    private val currentLocation = GeoPoint(51.2172, 4.4210)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Configuration.getInstance().userAgentValue = requireActivity().packageName
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRentNearMeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initializeMap()
        setupDistanceSlider()
        updateAddressText()
    }

    private fun initializeMap() {
        mapView = binding.mapView
        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.setMultiTouchControls(true)
        
        mapView.controller.apply {
            setZoom(15.0)  // Closer zoom level
            setCenter(currentLocation)
        }

        // Add marker for current location
        locationMarker = Marker(mapView).apply {
            position = currentLocation
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            title = "Current Location"
            icon = resources.getDrawable(R.drawable.ic_location_on_24, null)
        }
        mapView.overlays.add(locationMarker)

        // Initial circle overlay
        updateRadiusOverlay(5.0) // 5 km initial radius
    }

    private fun setupDistanceSlider() {
        binding.distanceSlider.addOnChangeListener { slider, value, fromUser ->
            updateRadiusOverlay(value.toDouble())
            binding.distanceLabel.text = "Search Radius: ${value.toInt()} km"
        }
    }

    private fun updateAddressText() {
        binding.addressText.text = "Current Location: Antwerp Central Station"
    }

    private fun updateRadiusOverlay(radiusKm: Double) {
        // Remove existing overlay
        radiusOverlay?.let { mapView.overlays.remove(it) }

        // Create circle points
        val points = ArrayList<GeoPoint>()
        val radiusMeters = radiusKm * 1000
        for (angle in 0..360 step 10) {
            val radian = Math.toRadians(angle.toDouble())
            val lat = currentLocation.latitude + (radiusMeters / 111320.0) * Math.sin(radian)
            val lon = currentLocation.longitude + (radiusMeters / (111320.0 * Math.cos(Math.toRadians(currentLocation.latitude)))) * Math.cos(radian)
            points.add(GeoPoint(lat, lon))
        }

        // Create and add new overlay
        radiusOverlay = Polygon().also { polygon ->
            polygon.setPoints(points)
            polygon.fillColor = Color.argb(50, 0, 0, 255)
            polygon.outlinePaint.color = Color.BLUE
            polygon.outlinePaint.strokeWidth = 2.0f
            mapView.overlays.add(polygon)
        }
        
        mapView.invalidate()
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}