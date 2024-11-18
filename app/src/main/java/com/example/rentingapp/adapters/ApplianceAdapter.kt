package com.example.rentingapp.adapters

import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.rentingapp.R
import com.example.rentingapp.RentalItem
import java.text.NumberFormat
import java.util.Locale

class ApplianceAdapter(private val appliances: List<RentalItem>) :
    RecyclerView.Adapter<ApplianceAdapter.ApplianceViewHolder>() {

    class ApplianceViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val imageAppliance: ImageView = view.findViewById(R.id.imageAppliance)
        private val textApplianceName: TextView = view.findViewById(R.id.textApplianceName)
        private val textCategory: TextView = view.findViewById(R.id.textCategory)
        private val textDailyRate: TextView = view.findViewById(R.id.textDailyRate)
        private val textDescription: TextView = view.findViewById(R.id.textDescription)
        private val textCondition: TextView = view.findViewById(R.id.textCondition)
        private val textAvailability: TextView = view.findViewById(R.id.textAvailability)
        private val textUsername: TextView = view.findViewById(R.id.textUsername)

        fun bind(appliance: RentalItem) {
            textApplianceName.text = appliance.applianceName
            textCategory.text = appliance.category
            
            // Format price with Euro symbol and Belgian locale
            val numberFormat = NumberFormat.getCurrencyInstance(Locale("nl", "BE"))
            textDailyRate.text = "${numberFormat.format(appliance.dailyRate)}/day"
            
            textDescription.text = appliance.description
            textCondition.text = appliance.condition

            // Set availability badge
            textAvailability.apply {
                text = if (appliance.availability) "Available" else "Unavailable"
                background = ContextCompat.getDrawable(
                    context,
                    if (appliance.availability) R.drawable.availability_badge_background
                    else R.drawable.unavailable_badge_background
                )
            }

            // Set temporary username (to be replaced with actual user data)
            textUsername.text = "User123"

            // Load image or set placeholder
            appliance.image?.let { blob ->
                try {
                    val bitmap = BitmapFactory.decodeByteArray(blob.toBytes(), 0, blob.toBytes().size)
                    imageAppliance.setImageBitmap(bitmap)
                } catch (e: Exception) {
                    imageAppliance.setImageResource(R.drawable.ic_appliance_placeholder)
                }
            } ?: imageAppliance.setImageResource(R.drawable.ic_appliance_placeholder)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ApplianceViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_appliance, parent, false)
        return ApplianceViewHolder(view)
    }

    override fun onBindViewHolder(holder: ApplianceViewHolder, position: Int) {
        holder.bind(appliances[position])
    }

    override fun getItemCount() = appliances.size
}
