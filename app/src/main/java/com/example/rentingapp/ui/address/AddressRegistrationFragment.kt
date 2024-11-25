package com.example.rentingapp.ui.address

import android.Manifest
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.rentingapp.databinding.AddressRegistrationBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.snackbar.Snackbar
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Marker
import java.util.Locale

class AddressRegistrationFragment : Fragment() {

    private var _binding: AddressRegistrationBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var currentLocationMarker: Marker? = null
    private var currentLocation: GeoPoint = GeoPoint(51.2194, 4.4025) // Default: Antwerp
    private var isAddressValidated = false

    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) -> {
                getCurrentLocation()
            }
            permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false) -> {
                getCurrentLocation()
            }
            else -> {
                Snackbar.make(binding.root, "Location permission is required", Snackbar.LENGTH_LONG).show()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = AddressRegistrationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        
        setupMapView()
        setupTextWatchers()
        setupSearchButton()
        setupRegisterButton()
        checkLocationPermission()
    }

    private fun setupMapView() {
        Configuration.getInstance().userAgentValue = requireActivity().packageName
        binding.mapView.apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(15.0)
            controller.setCenter(currentLocation)
        }
        updateMapMarker()
    }

    private fun updateMapMarker() {
        currentLocationMarker?.let { binding.mapView.overlays.remove(it) }
        
        currentLocationMarker = Marker(binding.mapView).apply {
            position = currentLocation
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        }
        binding.mapView.overlays.add(currentLocationMarker)
        binding.mapView.invalidate()
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
                    updateMapMarker()
                    lookupAddress(it.latitude, it.longitude)
                }
            }
        } catch (e: SecurityException) {
            Snackbar.make(binding.root, "Location permission is required", Snackbar.LENGTH_LONG).show()
        }
    }

    private fun lookupAddress(latitude: Double, longitude: Double) {
        try {
            val geocoder = Geocoder(requireContext(), Locale.getDefault())
            val addresses = geocoder.getFromLocation(latitude, longitude, 1)
            
            addresses?.firstOrNull()?.let { address ->
                binding.apply {
                    editTextCountry.setText(address.countryName ?: "")
                    editTextCity.setText(address.locality ?: "")
                    editTextZipCode.setText(address.postalCode ?: "")
                    editTextStreet.setText(address.thoroughfare ?: "")
                    editTextNumber.setText(address.subThoroughfare ?: "")
                }
                isAddressValidated = true
                updateButtonStates()
            }
        } catch (e: Exception) {
            Snackbar.make(binding.root, "Could not determine address", Snackbar.LENGTH_SHORT).show()
            isAddressValidated = false
            updateButtonStates()
        }
    }

    private fun setupTextWatchers() {
        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                isAddressValidated = false
                updateButtonStates()
            }
        }

        with(binding) {
            editTextCountry.addTextChangedListener(textWatcher)
            editTextCity.addTextChangedListener(textWatcher)
            editTextZipCode.addTextChangedListener(textWatcher)
            editTextStreet.addTextChangedListener(textWatcher)
            editTextNumber.addTextChangedListener(textWatcher)
        }
    }

    private fun validateFields(): Boolean {
        with(binding) {
            val isCountryFilled = editTextCountry.text?.isNotBlank() == true
            val isCityFilled = editTextCity.text?.isNotBlank() == true
            val isZipCodeFilled = editTextZipCode.text?.isNotBlank() == true
            val isStreetFilled = editTextStreet.text?.isNotBlank() == true
            val isNumberFilled = editTextNumber.text?.isNotBlank() == true

            return isCountryFilled && isCityFilled && isZipCodeFilled && 
                   isStreetFilled && isNumberFilled
        }
    }

    private fun updateButtonStates() {
        val areFieldsFilled = validateFields()
        binding.buttonSearch.isEnabled = areFieldsFilled && !isAddressValidated
        binding.buttonRegisterAddress.isEnabled = areFieldsFilled && isAddressValidated
    }

    private fun setupSearchButton() {
        binding.buttonSearch.setOnClickListener {
            if (validateFields()) {
                // Perform geocoding with the entered address
                val addressString = with(binding) {
                    "${editTextStreet.text} ${editTextNumber.text}, " +
                    "${editTextCity.text} ${editTextZipCode.text}, " +
                    editTextCountry.text
                }
                
                try {
                    val geocoder = Geocoder(requireContext(), Locale.getDefault())
                    val addresses = geocoder.getFromLocationName(addressString, 1)
                    
                    addresses?.firstOrNull()?.let { address ->
                        currentLocation = GeoPoint(address.latitude, address.longitude)
                        binding.mapView.controller.animateTo(currentLocation)
                        updateMapMarker()
                        isAddressValidated = true
                        updateButtonStates()
                        Snackbar.make(binding.root, "Address found!", Snackbar.LENGTH_SHORT).show()
                    } ?: run {
                        isAddressValidated = false
                        updateButtonStates()
                        Snackbar.make(binding.root, "Address not found", Snackbar.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    isAddressValidated = false
                    updateButtonStates()
                    Snackbar.make(binding.root, "Error searching address", Snackbar.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun setupRegisterButton() {
        binding.buttonRegisterAddress.setOnClickListener {
            if (validateFields() && isAddressValidated) {
                // TODO: Implement address registration
                // You can access the validated address and coordinates here
                val address = with(binding) {
                    mapOf(
                        "country" to editTextCountry.text.toString(),
                        "city" to editTextCity.text.toString(),
                        "zipCode" to editTextZipCode.text.toString(),
                        "street" to editTextStreet.text.toString(),
                        "number" to editTextNumber.text.toString(),
                        "latitude" to currentLocation.latitude,
                        "longitude" to currentLocation.longitude
                    )
                }
                // Handle the address registration
                Snackbar.make(binding.root, "Address registered successfully!", Snackbar.LENGTH_SHORT).show()
            }
        }
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
