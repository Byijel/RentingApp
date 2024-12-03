package com.example.rentingapp.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.rentingapp.databinding.FragmentHomeBinding
import com.example.rentingapp.adapters.ApplianceAdapter
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.Blob
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.Query
import com.google.firebase.Timestamp
import com.google.firebase.firestore.firestore
import androidx.navigation.fragment.findNavController
import com.example.rentingapp.R
import com.example.rentingapp.models.RentalItem
import com.example.rentingapp.adapters.RentedItemAdapter
import com.example.rentingapp.adapters.RentedOutItemAdapter

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [Home.newInstance] factory method to
 * create an instance of this fragment.
 */
class Home : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var rentedItemsAdapter: RentedItemAdapter
    private lateinit var rentedOutItemsAdapter: RentedOutItemAdapter
    private val rentedItems = mutableListOf<RentalItem>()
    private val rentedOutItems = mutableListOf<RentalItem>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)

        setupRecyclerViews()
        loadItems()

        return binding.root
    }

    private fun setupRecyclerViews() {
        // Setup Rented Items RecyclerView
        rentedItemsAdapter = RentedItemAdapter(rentedItems) { item ->
            val bundle = Bundle().apply {
                putParcelable("item", item)
            }
            findNavController().navigate(R.id.action_home_to_details, bundle)
        }
        binding.recyclerViewRentedItems.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = rentedItemsAdapter
        }

        // Setup Rented Out Items RecyclerView with new adapter
        rentedOutItemsAdapter = RentedOutItemAdapter(rentedOutItems) { item ->
            val bundle = Bundle().apply {
                putParcelable("item", item)
            }
            findNavController().navigate(R.id.action_home_to_details, bundle)
        }
        binding.recyclerViewRentedOutItems.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = rentedOutItemsAdapter
        }
    }

    private fun loadItems() {
        val currentUserId = Firebase.auth.currentUser?.uid ?: return
        
        // Track unique item IDs to prevent duplicates
        val uniqueItemIds = mutableSetOf<String>()

        // Load items user is renting
        Firebase.firestore.collection("RentedItems")
            .whereEqualTo("renterId", currentUserId)
            .addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener
                rentedItems.clear()
                snapshot?.forEach { document ->
                    if (!uniqueItemIds.contains(document.id)) {
                        uniqueItemIds.add(document.id)
                        // Load the actual item details and owner info
                        loadItemDetails(document, rentedItems, rentedItemsAdapter)
                    }
                }
            }

        // Load items user is renting out
        Firebase.firestore.collection("RentOutPosts")
            .whereEqualTo("userId", currentUserId)
            .addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener
                rentedOutItems.clear()
                snapshot?.forEach { document ->
                    if (!uniqueItemIds.contains(document.id)) {
                        uniqueItemIds.add(document.id)
                        loadItemDetails(document, rentedOutItems, rentedOutItemsAdapter)
                    }
                }
            }
    }

    private fun loadItemDetails(
        document: DocumentSnapshot,
        itemsList: MutableList<RentalItem>,
        adapter: ApplianceAdapter
    ) {
        // For rented items, we need to get the item details from RentOutPosts
        val itemId = document.getString("itemId") ?: return
        
        Firebase.firestore.collection("RentOutPosts").document(itemId)
            .get()
            .addOnSuccessListener { itemDoc ->
                // Get the owner's details
                val ownerId = itemDoc.getString("userId") ?: return@addOnSuccessListener
                Firebase.firestore.collection("users").document(ownerId)
                    .get()
                    .addOnSuccessListener { userDocument ->
                        val fullName = "${userDocument.getString("firstName")} ${userDocument.getString("lastName")}"
                        
                        val item = RentalItem(
                            id = itemDoc.id,
                            applianceName = itemDoc.getString("name") ?: "",
                            dailyRate = itemDoc.getDouble("price") ?: 0.0,
                            category = itemDoc.getString("category") ?: "",
                            condition = itemDoc.getString("condition") ?: "",
                            description = itemDoc.getString("description") ?: "",
                            availability = itemDoc.getBoolean("available") ?: true,
                            image = (itemDoc.get("images") as? Map<String, Any>)?.values?.firstOrNull() as? Blob,
                            createdAt = itemDoc.getTimestamp("createdAt") ?: Timestamp.now(),
                            ownerName = fullName,
                            userId = itemDoc.getString("userId") ?: ""
                        )
                        itemsList.add(item)
                        adapter.notifyDataSetChanged()
                    }
            }
    }

    private fun loadItemDetails(
        document: DocumentSnapshot,
        itemsList: MutableList<RentalItem>,
        adapter: RentedItemAdapter
    ) {
        val itemId = document.getString("itemId") ?: return
        
        Firebase.firestore.collection("RentOutPosts").document(itemId)
            .get()
            .addOnSuccessListener { itemDoc ->
                val ownerId = itemDoc.getString("userId") ?: return@addOnSuccessListener
                Firebase.firestore.collection("users").document(ownerId)
                    .get()
                    .addOnSuccessListener { userDocument ->
                        val fullName = "${userDocument.getString("firstName")} ${userDocument.getString("lastName")}"
                        
                        val item = RentalItem(
                            id = itemDoc.id,
                            applianceName = itemDoc.getString("name") ?: "",
                            dailyRate = itemDoc.getDouble("price") ?: 0.0,
                            category = itemDoc.getString("category") ?: "",
                            condition = itemDoc.getString("condition") ?: "",
                            description = itemDoc.getString("description") ?: "",
                            availability = itemDoc.getBoolean("available") ?: true,
                            image = (itemDoc.get("images") as? Map<String, Any>)?.values?.firstOrNull() as? Blob,
                            createdAt = itemDoc.getTimestamp("createdAt") ?: Timestamp.now(),
                            ownerName = fullName,
                            startDate = document.getTimestamp("startDate"),
                            endDate = document.getTimestamp("endDate"),
                            userId = itemDoc.getString("userId") ?: ""
                        )
                        itemsList.add(item)
                        adapter.notifyDataSetChanged()
                    }
            }
    }

    private fun loadItemDetails(
        document: DocumentSnapshot,
        itemsList: MutableList<RentalItem>,
        adapter: RentedOutItemAdapter
    ) {
        val itemId = document.id
        
        Firebase.firestore.collection("RentedItems")
            .whereEqualTo("itemId", itemId)
            .get()
            .addOnSuccessListener { rentals ->
                val rental = rentals.documents.firstOrNull()
                val renterId = rental?.getString("renterId")
                
                if (renterId != null) {
                    Firebase.firestore.collection("users").document(renterId)
                        .get()
                        .addOnSuccessListener { renterDoc ->
                            val renterName = "${renterDoc.getString("firstName")} ${renterDoc.getString("lastName")}"
                            createAndAddItem(document, rental, renterName, itemsList, adapter)
                        }
                } else {
                    createAndAddItem(document, rental, null, itemsList, adapter)
                }
            }
    }

    private fun createAndAddItem(
        itemDoc: DocumentSnapshot,
        rentalDoc: DocumentSnapshot?,
        renterName: String?,
        itemsList: MutableList<RentalItem>,
        adapter: RentedOutItemAdapter
    ) {
        val item = RentalItem(
            id = itemDoc.id,
            applianceName = itemDoc.getString("name") ?: "",
            dailyRate = itemDoc.getDouble("price") ?: 0.0,
            category = itemDoc.getString("category") ?: "",
            condition = itemDoc.getString("condition") ?: "",
            description = itemDoc.getString("description") ?: "",
            availability = itemDoc.getBoolean("available") ?: true,
            image = (itemDoc.get("images") as? Map<String, Any>)?.values?.firstOrNull() as? Blob,
            createdAt = itemDoc.getTimestamp("createdAt") ?: Timestamp.now(),
            renterName = renterName,
            startDate = rentalDoc?.getTimestamp("startDate"),
            endDate = rentalDoc?.getTimestamp("endDate"),
            userId = itemDoc.getString("userId") ?: ""
        )
        itemsList.add(item)
        adapter.notifyDataSetChanged()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}