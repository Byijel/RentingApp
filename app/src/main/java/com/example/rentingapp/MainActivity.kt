package com.example.rentingapp

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.navigation.ui.NavigationUI
import androidx.core.view.GravityCompat
import com.example.rentingapp.databinding.ActivityMainBinding
import com.example.rentingapp.services.FirestoreImageService
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.Blob
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase

class MainActivity : AppCompatActivity() {
    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var imageService: FirestoreImageService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.appBarMain.toolbar)

        // Setup toolbar logo click
        val toolbarLogo: ImageView = findViewById(R.id.toolbar_logo)
        toolbarLogo.setOnClickListener {
            // Navigate to home or perform specific action
            navController.navigateUp()
            navController.navigate(R.id.nav_home)
        }

        auth = Firebase.auth
        navController = findNavController(R.id.nav_host_fragment_content_main)
        val drawerLayout: DrawerLayout = binding.drawerLayout
        val navView: NavigationView = binding.navView

        // Disable swipe gesture
        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED, GravityCompat.START)

        appBarConfiguration = AppBarConfiguration(
            setOf(R.id.nav_home, R.id.nav_search, R.id.nav_rent_out, R.id.nav_address_registration),
            drawerLayout
        )

        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        // Initialize Firebase components
        db = FirebaseFirestore.getInstance()
        imageService = FirestoreImageService(this)

        // Check if user is logged in and has address
        val cUser = auth.currentUser
        if (cUser != null) {
            checkUserAddress(cUser.uid)
        }

        refreshNavigationHeader()

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

    override fun onStart() {
        super.onStart()
        Log.d("MainActivity", "onStart called")
        
        // Check if user is logged in
        val currentUser = auth.currentUser
        if (currentUser != null) {
            Log.d("MainActivity", "User is logged in, refreshing navigation header")
            refreshNavigationHeader()
        } else {
            Log.d("MainActivity", "No user logged in")
        }
    }

    private fun loadProfileImageInHeader(profileImageView: ImageView) {
        val currentUser = auth.currentUser

        Log.d("MainActivity", "Loading profile image - Current user: ${currentUser?.uid}")

        currentUser?.let { user ->
            db.collection("users").document(user.uid)
                .get()
                .addOnSuccessListener { document ->
                    Log.d("MainActivity", "User document retrieved successfully")
                    Log.d("MainActivity", "Document data: ${document.data}")
                    
                    val imageBlob = document.get("profileImage") as? Blob
                    Log.d("MainActivity", "Profile image blob: $imageBlob")

                    if (imageBlob != null) {
                        Log.d("MainActivity", "Profile image blob size: ${imageBlob.toBytes().size} bytes")
                        val bitmap = imageService.blobToBitmap(imageBlob)
                        Log.d("MainActivity", "Converted bitmap: ${bitmap != null}")
                        
                        if (bitmap != null) {
                            Log.d("MainActivity", "Bitmap dimensions: ${bitmap.width}x${bitmap.height}")
                            // Create a circular drawable
                            val roundedDrawable = RoundedBitmapDrawableFactory.create(resources, bitmap)
                            roundedDrawable.isCircular = true
                            profileImageView.setImageDrawable(roundedDrawable)
                        } else {
                            Log.e("MainActivity", "Failed to convert blob to bitmap")
                            profileImageView.setImageResource(R.drawable.ic_profile_placeholder)
                        }
                    } else {
                        Log.e("MainActivity", "No profile image blob found")
                        profileImageView.setImageResource(R.drawable.ic_profile_placeholder)
                    }
                }
                .addOnFailureListener { exception ->
                    Log.e("MainActivity", "Error fetching user document", exception)
                    // Set a default circular profile image if fetch fails
                    profileImageView.setImageResource(R.drawable.ic_profile_placeholder)
                }
        } ?: run {
            Log.e("MainActivity", "No current user found")
            profileImageView.setImageResource(R.drawable.ic_profile_placeholder)
        }
    }

    private fun refreshNavigationHeader() {
        val navView: NavigationView = binding.navView
        val headerView = navView.getHeaderView(0)
        
        if (headerView == null) {
            Log.e("MainActivity", "Navigation header view is null")
            return
        }

        val userNameTextView = headerView.findViewById<TextView>(R.id.userNameTextView)
        val userEmailTextView = headerView.findViewById<TextView>(R.id.userEmailTextView)
        val profileImageView = headerView.findViewById<ImageView>(R.id.nav_header_profile_image)

        // Reset views to default state
        userNameTextView.text = ""
        userEmailTextView.text = ""
        profileImageView.setImageResource(R.drawable.ic_profile_placeholder)

        val currentUser = auth.currentUser
        Log.d("MainActivity", "Current user: ${currentUser?.uid}")

        if (currentUser != null) {
            userEmailTextView.text = currentUser.email
            Log.d("MainActivity", "User email set: ${currentUser.email}")

            // Fetch user's name from Firestore using the authenticated user's ID
            db.collection("users").document(currentUser.uid)
                .get()
                .addOnSuccessListener { document ->
                    Log.d("MainActivity", "User document retrieved")
                    if (document.exists()) {
                        val firstName = document.getString("firstName") ?: ""
                        val lastName = document.getString("lastName") ?: ""
                        val fullName = "$firstName $lastName"
                        
                        userNameTextView.text = fullName
                        Log.d("MainActivity", "User name set: $fullName")
                        
                        // Load profile image only after we confirm we have the correct user document
                        loadProfileImageInHeader(profileImageView)
                    } else {
                        Log.e("MainActivity", "User document does not exist for ID: ${currentUser.uid}")
                        Toast.makeText(this, "Error loading user profile", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("MainActivity", "Error fetching user data: ${e.message}")
                    Toast.makeText(this, "Error loading user profile", Toast.LENGTH_SHORT).show()
                }
        } else {
            Log.e("MainActivity", "No current user found during header refresh")
        }
    }

    private fun checkUserAddress(userId: String) {
        FirebaseFirestore.getInstance().collection("users").document(userId)
            .get()
            .addOnSuccessListener { document ->
                val hasAddress = document.getBoolean("hasAddress") ?: false
                
                if (!hasAddress) {
                    // Optionally show a dialog or toast suggesting to complete address
                    runOnUiThread {
                        Toast.makeText(
                            this, 
                            "Please complete your profile by adding an address", 
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e("MainActivity", "Error checking address: ${e.message}")
                // Log the error but don't disrupt user flow
            }
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }
}