package com.example.rentingapp.ui.address

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.rentingapp.databinding.AddressRegistrationBinding
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Marker

class AddressRegistrationFragment : Fragment() {

    private var _binding: AddressRegistrationBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = AddressRegistrationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize the map
        Configuration.getInstance().userAgentValue = requireActivity().packageName
        val mapView = binding.mapView
        mapView.setMultiTouchControls(true)

        // Set default location to Antwerp
        val antwerp = GeoPoint(51.2194, 4.4025)
        val mapController = mapView.controller
        mapController.setZoom(12.0)
        mapController.setCenter(antwerp)

        // Add a marker for Antwerp
        val marker = Marker(mapView)
        marker.position = antwerp
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        mapView.overlays.add(marker)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
