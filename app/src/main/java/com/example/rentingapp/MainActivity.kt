package com.example.rentingapp

import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.navigation.ui.NavigationUI
import com.example.rentingapp.databinding.ActivityMainBinding
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase

class MainActivity : AppCompatActivity() {
    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.appBarMain.toolbar)

        auth = Firebase.auth
        navController = findNavController(R.id.nav_host_fragment_content_main)
        val drawerLayout: DrawerLayout = binding.drawerLayout
        val navView: NavigationView = binding.navView

        // Disable swipe gesture
        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)

        appBarConfiguration = AppBarConfiguration(
            setOf(R.id.nav_home, R.id.nav_search, R.id.nav_rent_near_me, R.id.nav_my_rents, R.id.nav_rent_out, R.id.nav_address_registration),
            drawerLayout
        )

        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        // Check if user is logged in and has address
        val currentUser = auth.currentUser
        if (currentUser != null) {
            checkUserAddress(currentUser.uid)
        }

        // Set user information in the navigation header
        val headerView = navView.getHeaderView(0)
        val userNameTextView = headerView.findViewById<TextView>(R.id.userNameTextView)
        val userEmailTextView = headerView.findViewById<TextView>(R.id.userEmailTextView)

        if (currentUser != null) {
            userEmailTextView.text = currentUser.email

            // Fetch user's name from Firestore
            FirebaseFirestore.getInstance().collection("users").document(currentUser.uid)
                .get()
                .addOnSuccessListener { document ->
                    val firstName = document.getString("firstName") ?: ""
                    val lastName = document.getString("lastName") ?: ""
                    userNameTextView.text = "$firstName $lastName"
                }
        }

        // Handle logout separately
        navView.menu.findItem(R.id.nav_logout).setOnMenuItemClickListener {
            auth.signOut()
            Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show()
            navController.navigate(R.id.loginFragment)
            drawerLayout.closeDrawers()
            true
        }

        // Listen for navigation changes
        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.loginFragment -> {
                    supportActionBar?.hide()
                    drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
                }
                R.id.registerFragment -> {
                    supportActionBar?.hide()
                    drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
                }
                else -> {
                    supportActionBar?.show()
                    if (auth.currentUser != null) {
                        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
                    }
                }
            }
        }
    }

    private fun checkUserAddress(userId: String) {
        FirebaseFirestore.getInstance().collection("users").document(userId)
            .get()
            .addOnSuccessListener { document ->
                val address = document.get("address") as? Map<*, *>
                if (address == null || 
                    address["street"].toString().isNullOrEmpty() || 
                    address["city"].toString().isNullOrEmpty()) {
                    // No address found, redirect to address registration
                    navController.navigate(R.id.nav_address_registration)
                }
            }
            .addOnFailureListener { e ->
                Log.e("MainActivity", "Error checking address: ${e.message}")
                // In case of error, redirect to address registration to be safe
                navController.navigate(R.id.nav_address_registration)
            }
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }
}