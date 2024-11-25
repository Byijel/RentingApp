package com.example.rentingapp.ui.search

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.rentingapp.R
import com.example.rentingapp.RentalItem
import com.example.rentingapp.adapters.ApplianceAdapter
import com.example.rentingapp.models.Category
import com.google.android.material.slider.Slider
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon
import kotlin.math.round

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [Search.newInstance] factory method to
 * create an instance of this fragment.
 */
class Search : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null

    private lateinit var searchInput: TextInputEditText
    private lateinit var categorySpinner: AutoCompleteTextView
    private lateinit var distanceSlider: Slider
    private lateinit var distanceLabel: TextView
    private lateinit var searchResults: androidx.recyclerview.widget.RecyclerView
    private lateinit var resultsAdapter: ApplianceAdapter
    private lateinit var mapView: MapView
    private lateinit var userLocation: GeoPoint
    private var radiusOverlay: Polygon? = null
    
    private val db = FirebaseFirestore.getInstance()
    private val items = mutableListOf<RentalItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_search, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        initializeViews(view)
        setupCategorySpinner()
        setupDistanceSlider()
        setupSearchInput()
        setupRecyclerView()
        setupMap()
    }

    private fun initializeViews(view: View) {
        searchInput = view.findViewById(R.id.searchInput)
        categorySpinner = view.findViewById(R.id.categorySpinner)
        distanceSlider = view.findViewById(R.id.distanceSlider)
        distanceLabel = view.findViewById(R.id.distanceLabel)
        searchResults = view.findViewById(R.id.searchResults)
        mapView = view.findViewById(R.id.mapView)
    }

    private fun setupCategorySpinner() {
        val categories = Category.getDisplayNames()
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, categories)
        categorySpinner.setAdapter(adapter)
        
        categorySpinner.setOnItemClickListener { _, _, _, _ ->
            performSearch()
        }
    }

    private fun setupDistanceSlider() {
        distanceSlider.apply {
            valueFrom = 0.2f  // 200m minimum
            valueTo = 10f     // 10km maximum
            stepSize = 0.2f   // 200m steps
            value = 1f        // Default to 1km
        }

        distanceSlider.addOnChangeListener { _, value, _ ->
            // Format the distance text based on the value
            val distanceText = if (value < 1f) {
                "${(value * 1000).toInt()}m" // Show in meters if less than 1km
            } else {
                "%.1fkm".format(value) // Show in kilometers with one decimal place
            }
            distanceLabel.text = "Maximum Distance: $distanceText"
            updateCircleOverlay()
            performSearch()
        }
    }

    private fun setupSearchInput() {
        searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                performSearch()
            }
        })
    }

    private fun setupRecyclerView() {
        resultsAdapter = ApplianceAdapter(items) { item ->
            val bundle = Bundle().apply {
                putParcelable("item", item)
            }
            findNavController().navigate(R.id.action_search_to_details, bundle)
        }
        searchResults.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = resultsAdapter
        }
    }

    private fun setupMap() {
        Configuration.getInstance().userAgentValue = requireActivity().packageName
        
        mapView.apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(15.0)
        }

        // Fetch user's address from Firestore
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            FirebaseFirestore.getInstance().collection("users").document(currentUser.uid)
                .get()
                .addOnSuccessListener { document ->
                    val address = document.get("address") as? Map<*, *>
                    if (address != null) {
                        val latitude = address["latitude"] as? Double ?: 0.0
                        val longitude = address["longitude"] as? Double ?: 0.0
                        userLocation = GeoPoint(latitude, longitude)

                        // Center map on user's location
                        mapView.controller.setCenter(userLocation)

                        // Add marker for user's location
                        val userMarker = Marker(mapView).apply {
                            position = userLocation
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                            title = "Your Location"
                        }
                        mapView.overlays.add(userMarker)

                        // Initial circle overlay
                        updateCircleOverlay()

                        loadRentalLocations()
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(context, "Failed to load user location: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun updateCircleOverlay() {
        // Remove existing circle overlay
        radiusOverlay?.let { mapView.overlays.remove(it) }

        // Create new circle overlay
        val radiusPoints = ArrayList<GeoPoint>()
        val radius = distanceSlider.value * 1000 // Convert km to meters
        for (i in 0..360 step 10) {
            val point = userLocation.destinationPoint(radius.toDouble(), i.toDouble())
            radiusPoints.add(point)
        }

        radiusOverlay = Polygon().apply {
            points = radiusPoints
            fillColor = android.graphics.Color.argb(50, 0, 0, 255)
            strokeColor = android.graphics.Color.BLUE
            strokeWidth = 2f
        }
        mapView.overlays.add(radiusOverlay)
        mapView.invalidate()
    }

    private fun loadRentalLocations() {
        // Fetch rental locations from Firestore and add them to the map
        FirebaseFirestore.getInstance().collection("RentOutPosts")
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    val userId = document.getString("userId") ?: continue
                    FirebaseFirestore.getInstance().collection("users").document(userId)
                        .get()
                        .addOnSuccessListener { userDoc ->
                            val address = userDoc.get("address") as? Map<*, *>
                            if (address != null) {
                                val latitude = address["latitude"] as? Double ?: 0.0
                                val longitude = address["longitude"] as? Double ?: 0.0
                                val rentalLocation = GeoPoint(latitude, longitude)

                                // Add marker for rental location
                                val rentalMarker = Marker(mapView).apply {
                                    position = rentalLocation
                                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                                    title = document.getString("name") ?: "Rental"
                                    icon = resources.getDrawable(R.drawable.ic_location_on_24, null)
                                }
                                mapView.overlays.add(rentalMarker)
                                mapView.invalidate()
                            }
                        }
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Failed to load rental locations: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun performSearch() {
        val searchText = searchInput.text?.toString()?.lowercase() ?: ""
        val selectedCategory = categorySpinner.text?.toString()
        val maxDistance = distanceSlider.value

        // Start with the base query
        var query: Query = db.collection("RentOutPosts")

        // Add category filter if selected
        if (!selectedCategory.isNullOrEmpty()) {
            query = query.whereEqualTo("category", selectedCategory)
        }

        // Clear existing items
        items.clear()
        resultsAdapter.notifyDataSetChanged()

        // Execute the query
        query.get().addOnSuccessListener { documents ->
            // Count how many documents we need to process
            val totalDocuments = documents.count { doc ->
                val name = doc.getString("name")?.lowercase() ?: ""
                val description = doc.getString("description")?.lowercase() ?: ""
                (searchText.isEmpty() || 
                    name.contains(searchText) || 
                    description.contains(searchText)) &&
                    isWithinDistance(doc, maxDistance)
            }
            
            if (totalDocuments == 0) {
                // No results found
                resultsAdapter.notifyDataSetChanged()
                return@addOnSuccessListener
            }

            // Process each document
            documents.forEach { document ->
                val name = document.getString("name")?.lowercase() ?: ""
                val description = document.getString("description")?.lowercase() ?: ""
                
                if ((searchText.isEmpty() || 
                    name.contains(searchText) || 
                    description.contains(searchText)) &&
                    isWithinDistance(document, maxDistance)) {
                    
                    val userId = document.getString("userId")
                    if (userId != null) {
                        db.collection("users").document(userId)
                            .get()
                            .addOnSuccessListener { userDocument ->
                                val fullName = "${userDocument.getString("firstName")} ${userDocument.getString("lastName")}"
                                
                                val item = RentalItem(
                                    id = document.id,
                                    applianceName = document.getString("name") ?: "",
                                    dailyRate = document.getDouble("price") ?: 0.0,
                                    category = document.getString("category") ?: "",
                                    condition = document.getString("condition") ?: "",
                                    description = document.getString("description") ?: "",
                                    availability = document.getBoolean("available") ?: true,
                                    ownerName = fullName,
                                    image = document.get("images")?.let { images ->
                                        (images as? Map<*, *>)?.values?.firstOrNull() as? com.google.firebase.firestore.Blob
                                    }
                                )
                                
                                // Add item and sort the list
                                items.add(item)
                                items.sortBy { it.applianceName }
                                resultsAdapter.notifyDataSetChanged()
                            }
                            .addOnFailureListener { exception ->
                                // Handle the error case by still adding the item but with empty owner
                                val item = RentalItem(
                                    id = document.id,
                                    applianceName = document.getString("name") ?: "",
                                    dailyRate = document.getDouble("price") ?: 0.0,
                                    category = document.getString("category") ?: "",
                                    condition = document.getString("condition") ?: "",
                                    description = document.getString("description") ?: "",
                                    availability = document.getBoolean("available") ?: true,
                                    ownerName = "Unknown",
                                    image = document.get("images")?.let { images ->
                                        (images as? Map<*, *>)?.values?.firstOrNull() as? com.google.firebase.firestore.Blob
                                    }
                                )
                                items.add(item)
                                items.sortBy { it.applianceName }
                                resultsAdapter.notifyDataSetChanged()
                            }
                    }
                }
            }
        }
    }

    // Placeholder function for distance calculation
    private fun isWithinDistance(document: com.google.firebase.firestore.DocumentSnapshot, maxDistance: Float): Boolean {
        // TODO: Implement actual distance calculation when location data is available
        // For now, return true to show all results
        return true
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment Search.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            Search().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}