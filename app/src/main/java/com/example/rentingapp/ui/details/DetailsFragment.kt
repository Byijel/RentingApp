package com.example.rentingapp.ui.details

import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.rentingapp.R
import com.example.rentingapp.databinding.FragmentDetailsBinding
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

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
        setupCalendarView()
        setupRentButton()
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

    private fun setupRentButton() {
        binding.rentButton.setOnClickListener {
            if (selectedStartDate == null || selectedEndDate == null) {
                Toast.makeText(context, "Please select start and end dates", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val rental = hashMapOf(
                "itemId" to args.item.id,
                "userId" to auth.currentUser?.uid,
                "startDate" to Timestamp(selectedStartDate!!.time),
                "endDate" to Timestamp(selectedEndDate!!.time),
                "totalPrice" to ((selectedEndDate!!.timeInMillis - selectedStartDate!!.timeInMillis) / (1000 * 60 * 60 * 24) + 1) * args.item.dailyRate
            )

            db.collection("rentals")
                .add(rental)
                .addOnSuccessListener {
                    Toast.makeText(context, "Rental confirmed!", Toast.LENGTH_SHORT).show()
                    findNavController().navigateUp()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun formatDate(date: Calendar): String {
        return SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(date.time)
    }
}