package com.example.rentingapp.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.rentingapp.databinding.FragmentHomeBinding
import com.example.rentingapp.RentalItem
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore

class HomeFragment : Fragment() {

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
        Firebase.firestore.collection("appliances")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    return@addSnapshotListener
                }

                appliances.clear()
                snapshot?.forEach { document ->
                    val appliance = RentalItem(
                        id = document.id,
                        applianceName = document.getString("applianceName") ?: "",
                        dailyRate = document.getDouble("dailyRate") ?: 0.0,
                        category = document.getString("category") ?: "",
                        condition = document.getString("condition") ?: "",
                        description = document.getString("description") ?: "",
                        availability = document.getBoolean("availability") ?: true
                    )
                    appliances.add(appliance)
                }
                applianceAdapter.notifyDataSetChanged()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}