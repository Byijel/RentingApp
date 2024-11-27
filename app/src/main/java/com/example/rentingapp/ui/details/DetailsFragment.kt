package com.example.rentingapp.ui.details

import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.rentingapp.R
import com.example.rentingapp.databinding.FragmentDetailsBinding
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.Date

class DetailsFragment : Fragment() {
    private lateinit var binding: FragmentDetailsBinding
    private val args: DetailsFragmentArgs by navArgs()
    private val db = FirebaseFirestore.getInstance()
    private val auth = Firebase.auth
    private var selectedStartDate: Calendar? = null
    private var selectedEndDate: Calendar? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        loadItemDetails()
        
        // Only setup calendar if not owner and item is available
        val currentUserId = Firebase.auth.currentUser?.uid
        val isOwner = currentUserId == args.item.userId
        
        if (!isOwner && args.item.availability) {
            setupCalendarView()
            binding.apply {
                calendar.visibility = View.VISIBLE
                selectedDates.visibility = View.VISIBLE
                totalPrice.visibility = View.VISIBLE
                rentButton.visibility = View.VISIBLE
            }
        } else if (!args.item.availability) {
            binding.rentStatusMessage.apply {
                text = "This item is not available"
                visibility = View.VISIBLE
            }
        }
        
        setupButtons()
    }

    private fun loadItemDetails() {
        binding.apply {
            itemName.text = args.item.applianceName
            itemPrice.text = String.format("€%.2f/day", args.item.dailyRate)
            itemDescription.text = args.item.description
            itemCondition.text = args.item.condition
            ownerName.text = "Owner: ${args.item.ownerName}"
            
            // Load image
            args.item.image?.let { blob ->
                try {
                    val bitmap = BitmapFactory.decodeByteArray(
                        blob.toBytes(), 
                        0, 
                        blob.toBytes().size
                    )
                    itemImage.setImageBitmap(bitmap)
                } catch (e: Exception) {
                    itemImage.setImageResource(R.drawable.ic_appliance_placeholder)
                }
            } ?: itemImage.setImageResource(R.drawable.ic_appliance_placeholder)
        }
    }

    private fun setupCalendarView() {
        binding.calendar.apply {
            // Set minimum date to today
            minDate = Calendar.getInstance().timeInMillis

            // Load existing rentals to disable dates
            loadExistingRentals()

            // Set date change listener
            setOnDateChangeListener { _, year, month, dayOfMonth ->
                handleDateSelection(year, month, dayOfMonth)
            }
        }
    }

    private fun loadExistingRentals() {
        db.collection("rentals")
            .whereEqualTo("itemId", args.item.id)
            .get()
            .addOnSuccessListener { documents ->
                // Create a list of rented date ranges
                val rentedDates = documents.mapNotNull { doc ->
                    val start = doc.getTimestamp("startDate")?.toDate()
                    val end = doc.getTimestamp("endDate")?.toDate()
                    if (start != null && end != null) start to end else null
                }
                
                // Disable rented dates in the calendar
                // Implementation depends on the calendar library used
            }
    }

    private fun handleDateSelection(year: Int, month: Int, dayOfMonth: Int) {
        val selectedDate = Calendar.getInstance().apply {
            set(year, month, dayOfMonth)
        }

        if (selectedStartDate == null) {
            selectedStartDate = selectedDate
            binding.selectedDates.text = "Start: ${formatDate(selectedDate)}"
        } else if (selectedEndDate == null && selectedDate.after(selectedStartDate)) {
            selectedEndDate = selectedDate
            binding.selectedDates.text = "Start: ${formatDate(selectedStartDate!!)} \nEnd: ${formatDate(selectedDate)}"
            updateTotalPrice()
        } else {
            // Reset selection
            selectedStartDate = selectedDate
            selectedEndDate = null
            binding.selectedDates.text = "Start: ${formatDate(selectedDate)}"
        }
    }

    private fun updateTotalPrice() {
        if (selectedStartDate != null && selectedEndDate != null) {
            val days = ((selectedEndDate!!.timeInMillis - selectedStartDate!!.timeInMillis) / (1000 * 60 * 60 * 24)).toInt() + 1
            val totalPrice = days * args.item.dailyRate
            binding.totalPrice.text = String.format("Total: €%.2f", totalPrice)
        }
    }

    private fun setupButtons() {
        val currentUserId = Firebase.auth.currentUser?.uid
        val isOwner = currentUserId == args.item.userId

        // First check if item is being rented
        checkIfItemIsRented { isRented ->
            binding.apply {
                rentButton.visibility = if (!isOwner && !isRented) View.VISIBLE else View.GONE
                toggleAvailabilityButton.visibility = if (isOwner && !isRented) View.VISIBLE else View.GONE
                removeListingButton.visibility = if (isOwner && !isRented) View.VISIBLE else View.GONE

                if (isRented) {
                    val message = if (isOwner) "This item is currently being rented" else "This item is not available"
                    rentStatusMessage.text = message
                    rentStatusMessage.visibility = View.VISIBLE
                } else {
                    rentStatusMessage.visibility = View.GONE
                    toggleAvailabilityButton.text = if (args.item.availability) "Make Unavailable" else "Make Available"
                }

                toggleAvailabilityButton.setOnClickListener {
                    toggleItemAvailability()
                }

                removeListingButton.setOnClickListener {
                    removeItem()
                }
            }
        }
    }

    private fun checkIfItemIsRented(callback: (Boolean) -> Unit) {
        Firebase.firestore.collection("RentedItems")
            .whereEqualTo("itemId", args.item.id)
            .get()
            .addOnSuccessListener { documents ->
                // Check if there are any active rentals
                val isRented = documents.any { doc ->
                    val endDate = doc.getTimestamp("endDate")?.toDate()
                    // Item is considered rented if end date is in the future
                    endDate?.after(Date()) ?: false
                }
                callback(isRented)
            }
            .addOnFailureListener {
                // In case of error, assume item is not rented
                callback(false)
            }
    }

    private fun toggleItemAvailability() {
        val newAvailability = !args.item.availability
        Firebase.firestore.collection("RentOutPosts")
            .document(args.item.id)
            .update("available", newAvailability)
            .addOnSuccessListener {
                Toast.makeText(
                    context,
                    if (newAvailability) "Item is now available" else "Item is now unavailable",
                    Toast.LENGTH_SHORT
                ).show()
                findNavController().navigateUp()
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Error updating availability: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun removeItem() {
        AlertDialog.Builder(requireContext())
            .setTitle("Remove Listing")
            .setMessage("Are you sure you want to remove this listing? This action cannot be undone.")
            .setPositiveButton("Remove") { _, _ ->
                Firebase.firestore.collection("RentOutPosts")
                    .document(args.item.id)
                    .delete()
                    .addOnSuccessListener {
                        Toast.makeText(context, "Listing removed successfully", Toast.LENGTH_SHORT).show()
                        findNavController().navigateUp()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(context, "Error removing listing: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun formatDate(date: Calendar): String {
        return SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(date.time)
    }
}