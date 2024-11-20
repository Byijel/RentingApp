package com.example.rentingapp.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.rentingapp.RentalItem
import com.example.rentingapp.databinding.ItemApplianceBinding

class ApplianceAdapter(private val appliances: List<RentalItem>) :
    RecyclerView.Adapter<ApplianceAdapter.ApplianceViewHolder>() {

    class ApplianceViewHolder(private val binding: ItemApplianceBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(appliance: RentalItem) {
            binding.textApplianceName.text = appliance.applianceName
            binding.textDailyRate.text = "€${appliance.dailyRate}/day"
            binding.textCategory.text = appliance.category
            binding.textCondition.text = "Condition: ${appliance.condition}"
            binding.textAvailability.text = if (appliance.availability) "Available" else "Currently Rented"
            binding.textAvailability.setTextColor(
                if (appliance.availability)
                    binding.root.context.getColor(android.R.color.holo_green_dark)
                else
                    binding.root.context.getColor(android.R.color.holo_red_dark)
            )
            binding.textUsername.text = appliance.ownerName // Use owner's full name
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ApplianceViewHolder {
        val binding = ItemApplianceBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ApplianceViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ApplianceViewHolder, position: Int) {
        holder.bind(appliances[position])
    }

    override fun getItemCount() = appliances.size
}