package com.example.rentingapp.ui.search

import android.Manifest
import android.content.Context
import android.graphics.Color
import android.location.Location
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.rentingapp.R
import com.example.rentingapp.adapters.ApplianceAdapter
import com.example.rentingapp.databinding.FragmentSearchBinding
import com.example.rentingapp.models.Category
import com.example.rentingapp.models.RentalItem
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.Blob
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon
import com.google.android.material.slider.Slider
import kotlinx.coroutines.Job
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

class Search : Fragment() {
    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!

    private val items = mutableListOf<RentalItem>()
    private val db = FirebaseFirestore.getInstance()

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var userLocation: GeoPoint? = null
    private var radiusOverlay: Polygon? = null
    private lateinit var adapter: ApplianceAdapter
    private val itemCircles = mutableMapOf<String, Polygon>()

    private var searchJob: Job? = null
    private val searchScope = CoroutineScope(
        Dispatchers.Main + Job()
    )

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

        // Load OSMDroid configuration
        Configuration.getInstance().load(
            requireContext(),
            requireContext().getSharedPreferences("osmdroid", Context.MODE_PRIVATE)
        )

        setupMap()
        setupUI()
        loadUserLocation()
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
        val categories = Category.getDisplayNames().toMutableList()
        categories.add(0, "All Categories")
        
        binding.categorySpinner.apply {
            val categoryAdapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                categories
            ).apply {
                setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            }
            
            threshold = 1000 // Prevent filtering
            setAdapter(categoryAdapter)
            setText("All Categories", false)
            
            setOnItemClickListener { _, _, _, _ ->
                performSearch()
            }
        }

        // Setup Distance Slider
        binding.distanceSlider.apply {
            valueFrom = 0.2f
            valueTo = 10f
            stepSize = 0.2f
            value = 1f
            
            // Update label during drag
            addOnChangeListener { _, value, _ ->
                binding.distanceLabel.text = "Distance: %.1f km".format(value)
            }
            
            // Perform search only when drag ends
            addOnSliderTouchListener(object : Slider.OnSliderTouchListener {
                override fun onStartTrackingTouch(slider: Slider) {
                    // Do nothing when starting to drag
                }

                override fun onStopTrackingTouch(slider: Slider) {
                    updateMapOverlays()
                    performSearch()
                }
            })
        }

        // Setup Search Input with debouncing
        binding.searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                searchJob?.cancel()
                searchJob = searchScope.launch {
                    delay(300) // 300ms delay
                    performSearch()
                }
            }
        })

        binding.showUnavailableCheckbox.setOnCheckedChangeListener { _, _ ->
            performSearch()
        }
    }

    private fun performSearch() {
        // Cancel any existing search
        searchJob?.cancel()

        val userLoc = userLocation ?: return
        val searchText = binding.searchInput.text?.toString()?.lowercase() ?: ""
        val selectedCategory = binding.categorySpinner.text?.toString()
        val maxDistance = binding.distanceSlider.value * 1000 // Convert to meters

        // Start with base query
        var query: Query = db.collection("RentOutPosts")

        // Add category filter if selected and not "All Categories"
        if (!selectedCategory.isNullOrEmpty() && selectedCategory != "All Categories") {
            query = query.whereEqualTo("category", selectedCategory)
        }

        // Clear existing items and tracking sets
        items.clear()
        itemCircles.clear()
        adapter.notifyDataSetChanged()

        // Track processed items to prevent duplicates
        val processedItemIds = mutableSetOf<String>()
        val pendingItems = mutableListOf<RentalItem>()
        var pendingQueries = 0
        var completedQueries = 0

        // Execute query
        query.get().addOnSuccessListener { documents ->
            if (documents.isEmpty) {
                adapter.notifyDataSetChanged()
                updateMapOverlays()
                return@addOnSuccessListener
            }

            documents.forEach { document ->
                val name = document.getString("name")?.lowercase() ?: ""
                val description = document.getString("description")?.lowercase() ?: ""

                if ((searchText.isEmpty() || name.contains(searchText) || 
                    description.contains(searchText)) && 
                    !processedItemIds.contains(document.id)) {
                    
                    processedItemIds.add(document.id)
                    document.getString("userId")?.let { userId ->
                        pendingQueries++
                        db.collection("users").document(userId)
                            .get()
                            .addOnSuccessListener { userDoc ->
                                completedQueries++
                                val address = userDoc.get("address") as? Map<*, *>
                                val lat = address?.get("latitude") as? Double
                                val lon = address?.get("longitude") as? Double

                                if (lat != null && lon != null) {
                                    val itemLocation = GeoPoint(lat, lon)
                                    val distance = userLoc.distanceToAsDouble(itemLocation)

                                    if (distance <= maxDistance) {
                                        val isAvailable = document.getBoolean("available") ?: true
                                        if (isAvailable || binding.showUnavailableCheckbox.isChecked) {
                                            val fullName = "${userDoc.getString("firstName")} ${userDoc.getString("lastName")}"
                                            val item = RentalItem(
                                                id = document.id,
                                                applianceName = document.getString("name") ?: "",
                                                dailyRate = document.getDouble("price") ?: 0.0,
                                                category = document.getString("category") ?: "",
                                                condition = document.getString("condition") ?: "",
                                                description = document.getString("description") ?: "",
                                                availability = document.getBoolean("available") ?: true,
                                                ownerName = fullName,
                                                userId = userId,
                                                image = document.get("images")?.let { images ->
                                                    (images as? Map<*, *>)?.values?.firstOrNull() as? Blob
                                                }
                                            )
                                            pendingItems.add(item)
                                        }
                                    }
                                }

                                if (completedQueries >= pendingQueries) {
                                    items.clear()
                                    items.addAll(pendingItems.distinctBy { it.id })
                                    items.sortBy { it.applianceName }
                                    adapter.notifyDataSetChanged()
                                    updateMapOverlays()
                                }
                            }
                            .addOnFailureListener {
                                completedQueries++
                                if (completedQueries >= pendingQueries) {
                                    items.clear()
                                    items.addAll(pendingItems.distinctBy { it.id })
                                    items.sortBy { it.applianceName }
                                    adapter.notifyDataSetChanged()
                                    updateMapOverlays()
                                }
                            }
                    }
                }
            }

            if (pendingQueries == 0) {
                adapter.notifyDataSetChanged()
                updateMapOverlays()
            }
        }
    }

    private fun getCurrentLocation() {
        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                location?.let {
                    userLocation = GeoPoint(it.latitude, it.longitude)
                    binding.mapView.controller.animateTo(userLocation)
                    updateMapOverlays()
                }
            }
        } catch (e: SecurityException) {
            Snackbar.make(binding.root, "Location permission is required", Snackbar.LENGTH_LONG).show()
        }
    }

    private fun loadUserLocation() {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: run {
            // Handle case when user is not logged in
            findNavController().navigate(R.id.loginFragment)
            return
        }

        db.collection("users").document(currentUser.uid)
            .get()
            .addOnSuccessListener { document ->
                if (!document.exists()) {
                    // Handle case when user document doesn't exist
                    findNavController().navigate(R.id.nav_address_registration)
                    return@addOnSuccessListener
                }

                val address = document.get("address") as? Map<*, *>
                val latitude = address?.get("latitude") as? Double
                val longitude = address?.get("longitude") as? Double

                if (latitude == null || longitude == null) {
                    // Handle case when address is incomplete
                    findNavController().navigate(R.id.nav_address_registration)
                    return@addOnSuccessListener
                }

                userLocation = GeoPoint(latitude, longitude)
                userLocation?.let { location ->
                    binding.mapView.controller.setCenter(location)
                    binding.mapView.controller.setZoom(15.0)
                    updateMapOverlays()
                    performSearch()
                }
            }
            .addOnFailureListener { exception ->
                // Handle failure to load user location
                Snackbar.make(binding.root, "Failed to load your location", Snackbar.LENGTH_LONG).show()
                findNavController().navigate(R.id.nav_address_registration)
            }
    }

    private fun setupMap() {
        Configuration.getInstance().userAgentValue = requireActivity().packageName

        binding.mapView.apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(15.0)
        }
    }

    private fun setupDistanceSlider() {
        binding.distanceSlider.addOnChangeListener { slider, value, fromUser ->
            binding.distanceLabel.text = "Distance: %.1f km".format(value)
            updateMapOverlays()
            performSearch()
        }
    }

    private fun updateMapOverlays() {
        val location = userLocation ?: return

        // Clear existing overlays and tracking
        binding.mapView.overlays.clear()
        itemCircles.clear()

        // Add user location marker
        val marker = Marker(binding.mapView).apply {
            position = location
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            title = "Your Location"
            icon = resources.getDrawable(R.drawable.ic_location_on_24, null)
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

        // Add markers for current items only
        items.forEach { item ->
            addItemMarker(item)
        }

        // Refresh the map
        binding.mapView.invalidate()
    }

    private fun addItemMarker(item: RentalItem) {
        // Skip if circle already exists for this item
        if (itemCircles.containsKey(item.id)) return

        val userLoc = userLocation ?: return
        val maxDistance = binding.distanceSlider.value * 1000 // Convert to meters

        item.userId?.let { userId ->
            db.collection("users").document(userId)
                .get()
                .addOnSuccessListener { userDoc ->
                    val address = userDoc.get("address") as? Map<*, *> ?: return@addOnSuccessListener
                    val lat = address["latitude"] as? Double ?: return@addOnSuccessListener
                    val lon = address["longitude"] as? Double ?: return@addOnSuccessListener

                    val itemLocation = GeoPoint(lat, lon)
                    
                    // Only add marker if item is within distance radius
                    if (userLoc.distanceToAsDouble(itemLocation) <= maxDistance) {
                        // Create random offset
                        val random = java.util.Random()
                        val offsetLat = random.nextDouble() * 0.002 - 0.001
                        val offsetLon = random.nextDouble() * 0.002 - 0.001

                        val offsetLocation = GeoPoint(lat + offsetLat, lon + offsetLon)
                        val radius = 100.0 + random.nextDouble() * 100.0
                        val points = ArrayList<GeoPoint>()

                        for (i in 0..360 step 10) {
                            val point = offsetLocation.destinationPoint(radius, i.toDouble())
                            points.add(point)
                        }

                        val circle = Polygon().apply {
                            this.points = points
                            fillColor = Color.argb(150, 255, 165, 0)
                            strokeColor = Color.rgb(255, 165, 0)
                            strokeWidth = 3f
                            title = "${item.applianceName} - €${item.dailyRate}/day"
                        }

                        // Store circle in tracking map and add to map
                        itemCircles[item.id] = circle
                        binding.mapView.overlays.add(circle)
                        binding.mapView.invalidate()
                    }
                }
        }
    }

    override fun onResume() {
        super.onResume()
        binding.mapView.onResume()
        
        userLocation?.let { location ->
            binding.mapView.controller.setCenter(location)
            updateMapOverlays()
        }
    }

    override fun onPause() {
        super.onPause()
        binding.mapView.onPause()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        searchJob?.cancel() // Cancel any pending search
        searchScope.cancel() // Cancel the scope
        binding.mapView.overlays.clear()
        itemCircles.clear()
        binding.mapView.onDetach()
        _binding = null
    }
}