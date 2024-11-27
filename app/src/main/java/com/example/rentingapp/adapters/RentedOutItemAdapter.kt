package com.example.rentingapp.adapters

import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.rentingapp.R
import com.example.rentingapp.models.RentalItem
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit

class RentedOutItemAdapter(
    private val items: List<RentalItem>,
    private val onItemClick: (RentalItem) -> Unit
) : RecyclerView.Adapter<RentedOutItemAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val imageRentedOutItem: ImageView = view.findViewById(R.id.imageRentedOutItem)
        private val textItemName: TextView = view.findViewById(R.id.textItemName)
        private val textRenter: TextView = view.findViewById(R.id.textRenter)
        private val textDates: TextView = view.findViewById(R.id.textDates)
        private val textRemainingDays: TextView = view.findViewById(R.id.textRemainingDays)

        fun bind(item: RentalItem) {
            textItemName.text = item.applianceName
            
            if (item.renterName != null) {
                textRenter.text = "Rented by: ${item.renterName}"
                
                // Format dates
                val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                val startDate = item.startDate?.toDate()
                val endDate = item.endDate?.toDate()

                if (startDate != null && endDate != null) {
                    textDates.text = "From: ${dateFormat.format(startDate)} To: ${dateFormat.format(endDate)}"
                    
                    // Calculate remaining days
                    val today = Calendar.getInstance().time
                    val remainingDays = TimeUnit.DAYS.convert(
                        endDate.time - today.time,
                        TimeUnit.MILLISECONDS
                    )

                    textRemainingDays.text = when {
                        remainingDays < 0 -> "Rental expired"
                        remainingDays == 0L -> "Last day"
                        else -> "$remainingDays days remaining"
                    }
                }
            } else {
                // Show placeholder text for non-rented items
                textRenter.text = "Rented by: Not rented"
                textDates.text = "No active rental"
                textRemainingDays.text = "Not currently rented"
            }

            // Load image
            item.image?.let { blob ->
                try {
                    val bitmap = BitmapFactory.decodeByteArray(
                        blob.toBytes(),
                        0,
                        blob.toBytes().size
                    )
                    imageRentedOutItem.setImageBitmap(bitmap)
                } catch (e: Exception) {
                    imageRentedOutItem.setImageResource(R.drawable.ic_appliance_placeholder)
                }
            } ?: imageRentedOutItem.setImageResource(R.drawable.ic_appliance_placeholder)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_rented_out_appliance, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
        holder.itemView.setOnClickListener { onItemClick(items[position]) }
    }

    override fun getItemCount() = items.size
} 