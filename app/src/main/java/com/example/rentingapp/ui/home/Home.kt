package com.example.rentingapp.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.rentingapp.databinding.FragmentHomeBinding
import com.example.rentingapp.RentalItem
import com.example.rentingapp.adapters.ApplianceAdapter
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.Blob
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.Query
import com.google.firebase.Timestamp
import com.google.firebase.firestore.firestore
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
    private lateinit var rentedItemsAdapter: ApplianceAdapter
    private lateinit var rentedOutItemsAdapter: ApplianceAdapter
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
        rentedItemsAdapter = ApplianceAdapter(rentedItems)
        binding.recyclerViewRentedItems.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = rentedItemsAdapter
        }

        // Setup Rented Out Items RecyclerView
        rentedOutItemsAdapter = ApplianceAdapter(rentedOutItems)
        binding.recyclerViewRentedOutItems.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = rentedOutItemsAdapter
        }
    }

    private fun loadItems() {
        val currentUserId = Firebase.auth.currentUser?.uid ?: return
        
        // Load items user is renting
        Firebase.firestore.collection("RentedItems")
            .whereEqualTo("renterId", currentUserId)
            .addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener
                rentedItems.clear()
                snapshot?.forEach { document ->
                    // Load the actual item details and owner info
                    loadItemDetails(document, rentedItems, rentedItemsAdapter)
                }
            }

        // Load items user is renting out
        Firebase.firestore.collection("RentOutPosts")
            .whereEqualTo("userId", currentUserId)
            .addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener
                rentedOutItems.clear()
                snapshot?.forEach { document ->
                    loadItemDetails(document, rentedOutItems, rentedOutItemsAdapter)
                }
            }
    }

    private fun loadItemDetails(document: DocumentSnapshot, itemsList: MutableList<RentalItem>, adapter: ApplianceAdapter) {
        val userId = document.getString("userId") ?: return
        Firebase.firestore.collection("users").document(userId).get()
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
                    image = (document.get("images") as? Map<String, Any>)?.values?.firstOrNull() as? Blob,
                    createdAt = document.getTimestamp("createdAt") ?: Timestamp.now(),
                    ownerName = fullName
                )
                itemsList.add(item)
                adapter.notifyDataSetChanged()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}