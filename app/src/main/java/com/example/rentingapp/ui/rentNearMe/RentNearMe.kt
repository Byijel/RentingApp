package com.example.rentingapp.ui.rentNearMe

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.rentingapp.databinding.FragmentRentNearMeBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.snackbar.Snackbar
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon

class RentNearMe : Fragment() {
    private var _binding: FragmentRentNearMeBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var currentLocationMarker: Marker? = null
    private var radiusOverlay: Polygon? = null
    private var currentLocation: GeoPoint = GeoPoint(51.2173, 4.4209) // Default: Antwerp Central

    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) -> {
                // Precise location access granted
                getCurrentLocation()
            }
            permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false) -> {
                // Only approximate location access granted
                getCurrentLocation()
            }
            else -> {
                // No location access granted
                Snackbar.make(binding.root, "Location permission is required", Snackbar.LENGTH_LONG).show()
            }
        }
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
        
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        
        setupMap()
        setupDistanceSlider()
        setupLocationButton()
    }

    private fun setupMap() {
        Configuration.getInstance().userAgentValue = requireActivity().packageName
        
        binding.mapView.apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(15.0)
            controller.setCenter(currentLocation)
        }
        
        updateMapOverlays()
    }

    private fun setupDistanceSlider() {
        binding.distanceSlider.addOnChangeListener { slider, value, fromUser ->
            binding.distanceText.text = "Distance: %.1f km".format(value)
            updateMapOverlays()
        }
    }

    private fun setupLocationButton() {
        binding.fabMyLocation.setOnClickListener {
            checkLocationPermission()
        }
    }

    private fun checkLocationPermission() {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                getCurrentLocation()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) -> {
                Snackbar.make(
                    binding.root,
                    "Location permission is needed to show your position on the map",
                    Snackbar.LENGTH_INDEFINITE
                ).setAction("OK") {
                    requestLocationPermission()
                }.show()
            }
            else -> {
                requestLocationPermission()
            }
        }
    }

    private fun requestLocationPermission() {
        locationPermissionRequest.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    private fun getCurrentLocation() {
        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                location?.let {
                    currentLocation = GeoPoint(it.latitude, it.longitude)
                    binding.mapView.controller.animateTo(currentLocation)
                    updateMapOverlays()
                }
            }
        } catch (e: SecurityException) {
            Snackbar.make(binding.root, "Location permission is required", Snackbar.LENGTH_LONG).show()
        }
    }

    private fun updateMapOverlays() {
        // Clear existing overlays
        binding.mapView.overlays.clear()
        
        // Add location marker
        currentLocationMarker = Marker(binding.mapView).apply {
            position = currentLocation
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        }
        binding.mapView.overlays.add(currentLocationMarker)
        
        // Add radius circle
        val radiusPoints = ArrayList<GeoPoint>()
        val radius = binding.distanceSlider.value * 1000 // Convert km to meters
        for (i in 0..360 step 10) {
            val point = currentLocation.destinationPoint(radius.toDouble(), i.toDouble())
            radiusPoints.add(point)
        }
        
        radiusOverlay = Polygon().apply {
            points = radiusPoints
            fillColor = Color.argb(50, 0, 0, 255)
            strokeColor = Color.BLUE
            strokeWidth = 2f
        }
        binding.mapView.overlays.add(radiusOverlay)
        
        binding.mapView.invalidate()
    }

    override fun onResume() {
        super.onResume()
        binding.mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        binding.mapView.onPause()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}