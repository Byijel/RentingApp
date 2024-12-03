package com.example.rentingapp.ui.search

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.rentingapp.R
import com.example.rentingapp.adapters.ApplianceAdapter
import com.example.rentingapp.databinding.FragmentSearchBinding
import com.example.rentingapp.models.Category
import com.example.rentingapp.models.RentalItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.Blob
import com.google.firebase.firestore.FirebaseFirestore
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon

class Search : Fragment() {
    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!
    
    private val items = mutableListOf<RentalItem>()
    private val db = FirebaseFirestore.getInstance()
    private var userLocation: GeoPoint? = null
    private var radiusOverlay: Polygon? = null
    private lateinit var adapter: ApplianceAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        Configuration.getInstance().load(
            requireContext(),
            requireContext().getSharedPreferences("osmdroid", Context.MODE_PRIVATE)
        )
        
        setupMap()
        setupUI()
        loadUserLocation()
    }

    private fun setupMap() {
        binding.mapView.apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(15.0)
        }
    }

    private fun setupUI() {
        // Setup RecyclerView
        adapter = ApplianceAdapter(items) { item ->
            findNavController().navigate(
                R.id.action_search_to_details,
                Bundle().apply { putParcelable("item", item) }
            )
        }
        binding.searchResults.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = this@Search.adapter
        }

        // Setup Category Spinner
        binding.categorySpinner.apply {
            setAdapter(ArrayAdapter(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                Category.getDisplayNames()
            ))
            setOnItemClickListener { _, _, _, _ -> performSearch() }
        }

        // Setup Distance Slider
        binding.distanceSlider.apply {
            valueFrom = 0.2f
            valueTo = 10f
            stepSize = 0.2f
            value = 1f
            addOnChangeListener { _, value, _ ->
                binding.distanceLabel.text = "Maximum Distance: ${formatDistance(value)}"
                updateMapOverlays()
                performSearch()
            }
        }

        // Setup Search Input
        binding.searchInput.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) { performSearch() }
        })
    }

    private fun formatDistance(value: Float): String {
        return if (value < 1f) "${(value * 1000).toInt()}m"
        else "%.1fkm".format(value)
    }

    private fun loadUserLocation() {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return
        
        db.collection("users").document(currentUser.uid)
            .get()
            .addOnSuccessListener { document ->
                val address = document.get("address") as? Map<*, *> ?: return@addOnSuccessListener
                val latitude = address["latitude"] as? Double ?: return@addOnSuccessListener
                val longitude = address["longitude"] as? Double ?: return@addOnSuccessListener
                
                userLocation = GeoPoint(latitude, longitude)
                userLocation?.let { location ->
                    binding.mapView.controller.setCenter(location)
                    updateMapOverlays()
                    performSearch()
                }
            }
    }

    private fun updateMapOverlays() {
        val location = userLocation ?: return
        
        // Clear existing overlays
        binding.mapView.overlays.clear()
        
        // Add user location marker
        val marker = Marker(binding.mapView).apply {
            position = location
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            title = "Your Location"
        }
        binding.mapView.overlays.add(marker)
        
        // Add radius circle
        val radiusPoints = ArrayList<GeoPoint>()
        val radius = binding.distanceSlider.value * 1000 // Convert km to meters
        for (i in 0..360 step 10) {
            val point = location.destinationPoint(radius.toDouble(), i.toDouble())
            radiusPoints.add(point)
        }
        
        radiusOverlay = Polygon().apply {
            points = radiusPoints
            fillColor = Color.argb(50, 0, 0, 255)
            strokeColor = Color.BLUE
            strokeWidth = 2f
        }
        binding.mapView.overlays.add(radiusOverlay)
        
        // Add rental item markers
        items.forEach { item ->
            // You would need to store location info in your RentalItem class
            // This is just a placeholder for the concept
            addItemMarker(item)
        }
        
        binding.mapView.invalidate()
    }

    private fun addItemMarker(item: RentalItem) {
        // Get item location from Firestore
        item.userId?.let { userId ->
            db.collection("users").document(userId)
                .get()
                .addOnSuccessListener { userDoc ->
                    val address = userDoc.get("address") as? Map<*, *> ?: return@addOnSuccessListener
                    val lat = address["latitude"] as? Double ?: return@addOnSuccessListener
                    val lon = address["longitude"] as? Double ?: return@addOnSuccessListener
                    
                    val marker = Marker(binding.mapView).apply {
                        position = GeoPoint(lat, lon)
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        title = item.applianceName
                        snippet = "€${item.dailyRate}/day"
                    }
                    binding.mapView.overlays.add(marker)
                    binding.mapView.invalidate()
                }
        }
    }

    private fun performSearch() {
        val userLoc = userLocation ?: return
        val searchText = binding.searchInput.text?.toString()?.lowercase() ?: ""
        val selectedCategory = binding.categorySpinner.text?.toString()
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        // Clear previous results
        items.clear()
        
        // Query Firestore
        var query = db.collection("RentOutPosts")
            .whereNotEqualTo("userId", currentUserId)

        if (!selectedCategory.isNullOrEmpty()) {
            query = query.whereEqualTo("category", selectedCategory)
        }

        query.get().addOnSuccessListener { documents ->
            for (document in documents) {
                val name = document.getString("name")?.lowercase() ?: ""
                val description = document.getString("description")?.lowercase() ?: ""

                if (searchText.isEmpty() || name.contains(searchText) || 
                    description.contains(searchText)) {
                    
                    document.getString("userId")?.let { userId ->
                        db.collection("users").document(userId)
                            .get()
                            .addOnSuccessListener { userDoc ->
                                val address = userDoc.get("address") as? Map<*, *> ?: return@addOnSuccessListener
                                val lat = address["latitude"] as? Double ?: return@addOnSuccessListener
                                val lon = address["longitude"] as? Double ?: return@addOnSuccessListener
                                val itemLocation = GeoPoint(lat, lon)

                                if (userLoc.distanceToAsDouble(itemLocation) <= binding.distanceSlider.value * 1000) {
                                    val item = RentalItem(
                                        id = document.id,
                                        applianceName = document.getString("name") ?: "",
                                        dailyRate = document.getDouble("price") ?: 0.0,
                                        category = document.getString("category") ?: "",
                                        condition = document.getString("condition") ?: "",
                                        description = document.getString("description") ?: "",
                                        availability = document.getBoolean("available") ?: true,
                                        ownerName = "${userDoc.getString("firstName")} ${userDoc.getString("lastName")}",
                                        userId = userId,
                                        image = document.get("images")?.let { images ->
                                            (images as? Map<*, *>)?.values?.firstOrNull() as? Blob
                                        }
                                    )
                                    items.add(item)
                                    items.sortBy { it.applianceName }
                                    adapter.notifyDataSetChanged()
                                    updateMapOverlays()
                                }
                            }
                    }
                }
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
        binding.mapView.onDetach()
        _binding = null
    }
}