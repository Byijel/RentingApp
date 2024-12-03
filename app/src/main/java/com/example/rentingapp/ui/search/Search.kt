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
        
        // Add rental item circles - only for unique items
        val uniqueItems = items.distinctBy { it.id }
        uniqueItems.forEach { item ->
            addItemMarker(item)
        }
        
        binding.mapView.invalidate()
    }

    private fun addItemMarker(item: RentalItem) {
        item.userId?.let { userId ->
            db.collection("users").document(userId)
                .get()
                .addOnSuccessListener { userDoc ->
                    val address = userDoc.get("address") as? Map<*, *> ?: return@addOnSuccessListener
                    val lat = address["latitude"] as? Double ?: return@addOnSuccessListener
                    val lon = address["longitude"] as? Double ?: return@addOnSuccessListener
                    
                    // Create random offset (within ~100m)
                    val random = java.util.Random()
                    val offsetLat = random.nextDouble() * 0.002 - 0.001  // Increased offset range
                    val offsetLon = random.nextDouble() * 0.002 - 0.001  // Increased offset range
                    
                    // Apply offset to location
                    val offsetLocation = GeoPoint(lat + offsetLat, lon + offsetLon)
                    
                    // Create circle with random radius between 100m and 200m (bigger circles)
                    val radius = 100.0 + random.nextDouble() * 100.0
                    val points = ArrayList<GeoPoint>()
                    
                    // Generate circle points
                    for (i in 0..360 step 10) {
                        val point = offsetLocation.destinationPoint(radius, i.toDouble())
                        points.add(point)
                    }
                    
                    // Create and add circle overlay
                    val circle = Polygon().apply {
                        this.points = points
                        fillColor = Color.argb(150, 255, 165, 0)  // More opaque orange (alpha increased from 80 to 150)
                        strokeColor = Color.rgb(255, 165, 0)      // Solid orange
                        strokeWidth = 3f                          // Slightly thicker border
                        
                        // Optional: Add title/info window
                        title = "${item.applianceName} - €${item.dailyRate}/day"
                    }
                    
                    binding.mapView.overlays.add(circle)
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
        val processedItems = mutableSetOf<String>()
        val pendingItems = mutableListOf<RentalItem>()

        var query = db.collection("RentOutPosts")
            .whereNotEqualTo("userId", currentUserId)

        if (!selectedCategory.isNullOrEmpty()) {
            query = query.whereEqualTo("category", selectedCategory)
        }

        query.get().addOnSuccessListener { documents ->
            var completedQueries = 0
            val totalQueries = documents.size()

            for (document in documents) {
                if (processedItems.contains(document.id)) continue
                
                val name = document.getString("name")?.lowercase() ?: ""
                val description = document.getString("description")?.lowercase() ?: ""

                if (searchText.isEmpty() || name.contains(searchText) || 
                    description.contains(searchText)) {
                    
                    document.getString("userId")?.let { userId ->
                        db.collection("users").document(userId)
                            .get()
                            .addOnSuccessListener { userDoc ->
                                completedQueries++
                                
                                val address = userDoc.get("address") as? Map<*, *> ?: return@addOnSuccessListener
                                val lat = address["latitude"] as? Double ?: return@addOnSuccessListener
                                val lon = address["longitude"] as? Double ?: return@addOnSuccessListener
                                val itemLocation = GeoPoint(lat, lon)

                                if (userLoc.distanceToAsDouble(itemLocation) <= binding.distanceSlider.value * 1000) {
                                    if (!processedItems.contains(document.id)) {
                                        processedItems.add(document.id)
                                        
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
                                        pendingItems.add(item)
                                    }
                                }
                                
                                // Only update UI when all queries are complete
                                if (completedQueries >= totalQueries) {
                                    items.clear()
                                    items.addAll(pendingItems.distinctBy { it.id })
                                    items.sortBy { it.applianceName }
                                    adapter.notifyDataSetChanged()
                                    updateMapOverlays()
                                }
                            }
                            .addOnFailureListener {
                                completedQueries++
                            }
                    }
                } else {
                    completedQueries++
                }
            }
            
            // Handle case when there are no items
            if (totalQueries == 0) {
                items.clear()
                adapter.notifyDataSetChanged()
                updateMapOverlays()
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