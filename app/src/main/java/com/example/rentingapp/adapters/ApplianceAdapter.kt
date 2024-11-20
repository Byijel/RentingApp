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

class ApplianceAdapter(
    private val items: MutableList<RentalItem>,
    private val onItemClick: (RentalItem) -> Unit = {}
) : RecyclerView.Adapter<ApplianceAdapter.ApplianceViewHolder>() {

    inner class ApplianceViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val imageAppliance: ImageView = view.findViewById(R.id.imageAppliance)
        private val textApplianceName: TextView = view.findViewById(R.id.textApplianceName)
        private val textCategory: TextView = view.findViewById(R.id.textCategory)
        private val textDailyRate: TextView = view.findViewById(R.id.textDailyRate)
        private val textDescription: TextView = view.findViewById(R.id.textDescription)
        private val textCondition: TextView = view.findViewById(R.id.textCondition)
        private val textAvailability: TextView = view.findViewById(R.id.textAvailability)
        private val textUsername: TextView = view.findViewById(R.id.textUsername)

        fun bind(appliance: RentalItem) {
            bindTextViews(appliance)
            bindAvailabilityBadge(appliance.availability)
            bindImage(appliance)
            
            itemView.setOnClickListener {
                this@ApplianceAdapter.onItemClick(appliance)
            }
        }

        private fun bindTextViews(appliance: RentalItem) {
            textApplianceName.text = appliance.applianceName
            textCategory.text = appliance.category
            textDailyRate.text = formatPrice(appliance.dailyRate)
            textDescription.text = appliance.description
            textCondition.text = appliance.condition
            textUsername.text = appliance.ownerName
        }

        private fun bindAvailabilityBadge(isAvailable: Boolean) {
            textAvailability.apply {
                text = if (isAvailable) "Available" else "Unavailable"
                background = ContextCompat.getDrawable(
                    context,
                    if (isAvailable) R.drawable.availability_badge_background
                    else R.drawable.unavailable_badge_background
                )
            }
        }

        private fun bindImage(appliance: RentalItem) {
            appliance.image?.let { blob ->
                try {
                    val bitmap = BitmapFactory.decodeByteArray(blob.toBytes(), 0, blob.toBytes().size)
                    imageAppliance.setImageBitmap(bitmap)
                } catch (e: Exception) {
                    setPlaceholderImage()
                }
            } ?: setPlaceholderImage()
        }

        private fun setPlaceholderImage() {
            imageAppliance.setImageResource(R.drawable.ic_appliance_placeholder)
        }

        private fun formatPrice(price: Double): String {
            val numberFormat = NumberFormat.getCurrencyInstance(Locale("nl", "BE"))
            return "${numberFormat.format(price)}/day"
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ApplianceViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_appliance, parent, false)
        return ApplianceViewHolder(view)
    }

    override fun onBindViewHolder(holder: ApplianceViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size
}
