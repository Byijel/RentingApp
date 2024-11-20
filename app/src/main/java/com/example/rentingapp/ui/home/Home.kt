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
import com.google.firebase.firestore.Blob
import com.google.firebase.firestore.Query
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
    private lateinit var applianceAdapter: ApplianceAdapter
    private val appliances = mutableListOf<RentalItem>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)

        setupRecyclerView()
        loadAppliances()

        return binding.root
    }

    private fun setupRecyclerView() {
        applianceAdapter = ApplianceAdapter(appliances)
        binding.recyclerViewAppliances.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = applianceAdapter
        }
    }

    private fun loadAppliances() {
        Firebase.firestore.collection("RentOutPosts")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    return@addSnapshotListener
                }

                appliances.clear()
                snapshot?.forEach { document ->
                    val userId = document.getString("userId") ?: return@forEach
                    Firebase.firestore.collection("users").document(userId).get()
                        .addOnSuccessListener { userDocument ->
                            val fullName = "${userDocument.getString("firstName")} ${userDocument.getString("lastName")}"
                            val appliance = RentalItem(
                                id = document.id,
                                applianceName = document.getString("name") ?: "",
                                dailyRate = document.getDouble("price") ?: 0.0,
                                category = document.getString("category") ?: "",
                                condition = document.getString("condition") ?: "",
                                description = document.getString("description") ?: "",
                                availability = document.getBoolean("available") ?: true,
                                image = (document.get("images") as? Map<String, Any>)?.values?.firstOrNull() as? Blob,
                                createdAt = document.getTimestamp("createdAt") ?: com.google.firebase.Timestamp.now(),
                                ownerName = fullName
                            )
                            appliances.add(appliance)
                            applianceAdapter.notifyDataSetChanged()
                        }
                }
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}