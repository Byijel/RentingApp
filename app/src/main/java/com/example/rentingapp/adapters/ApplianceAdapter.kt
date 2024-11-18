package com.example.rentingapp.adapters

import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.rentingapp.R
import com.example.rentingapp.RentalItem
import java.text.NumberFormat
import java.util.Locale

class ApplianceAdapter(
    private val appliances: List<RentalItem>
) : RecyclerView.Adapter<ApplianceAdapter.ApplianceViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ApplianceViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_appliance, parent, false)
        return ApplianceViewHolder(view)
    }

    override fun onBindViewHolder(holder: ApplianceViewHolder, position: Int) {
        holder.bind(appliances[position])
    }

    override fun getItemCount() = appliances.size

    inner class ApplianceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imageAppliance: ImageView = itemView.findViewById(R.id.imageAppliance)
        private val textApplianceName: TextView = itemView.findViewById(R.id.textApplianceName)
        private val textCategory: TextView = itemView.findViewById(R.id.textCategory)
        private val textDailyRate: TextView = itemView.findViewById(R.id.textDailyRate)
        private val textCondition: TextView = itemView.findViewById(R.id.textCondition)
        private val textAvailability: TextView = itemView.findViewById(R.id.textAvailability)
        private val textUsername: TextView = itemView.findViewById(R.id.textUsername)

        fun bind(appliance: RentalItem) {
            // Set appliance name
            textApplianceName.text = appliance.applianceName

            // Set category
            textCategory.text = appliance.category

            // Format and set daily rate
            val formattedPrice = NumberFormat.getCurrencyInstance(Locale("nl", "BE"))
                .format(appliance.dailyRate)
            textDailyRate.text = "$formattedPrice/day"

            // Set condition
            textCondition.text = appliance.condition

            // Set availability badge
            textAvailability.apply {
                text = if (appliance.availability) "Available" else "Unavailable"
                setBackgroundResource(
                    if (appliance.availability) 
                        R.drawable.availability_badge_background
                    else 
                        R.drawable.unavailable_badge_background
                )
            }

            // Set username (you'll need to implement this when you have user data)
            textUsername.text = "User123" // Placeholder

            // Load image from Firebase if available
            appliance.image?.let { blob ->
                try {
                    val bytes = blob.toBytes()
                    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    imageAppliance.setImageBitmap(bitmap)
                } catch (e: Exception) {
                    // If image loading fails, use placeholder
                    imageAppliance.setImageResource(R.drawable.ic_appliance_placeholder)
                }
            } ?: run {
                // No image available, use placeholder
                imageAppliance.setImageResource(R.drawable.ic_appliance_placeholder)
            }
        }
    }
}
