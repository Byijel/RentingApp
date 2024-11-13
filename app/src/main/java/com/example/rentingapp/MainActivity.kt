package com.example.rentingapp

import android.os.Bundle
import android.view.Menu
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.navigation.NavigationView
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.drawerlayout.widget.DrawerLayout
import androidx.appcompat.app.AppCompatActivity
import com.example.rentingapp.databinding.ActivityMainBinding
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore

class MainActivity : AppCompatActivity() {
    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private val db = Firebase.firestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.appBarMain.toolbar)

        // Set up the Floating Action Button (FAB) click listener
        binding.appBarMain.fab.setOnClickListener { view ->
            addSampleData() // Changed to add sample data when FAB is clicked
        }
        val drawerLayout: DrawerLayout = binding.drawerLayout
        val navView: NavigationView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_home, R.id.nav_search, R.id.nav_rent_out, R.id.nav_rent_near_me, R.id.nav_my_rents
            ), drawerLayout
        )

        // Set the NavigationView item selected listener
        navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_home -> navController.navigate(R.id.nav_home)
                R.id.nav_search -> navController.navigate(R.id.nav_search)
                R.id.nav_rent_near_me -> navController.navigate(R.id.nav_rent_near_me)
                R.id.nav_my_rents -> navController.navigate(R.id.nav_my_rents)
                R.id.nav_rent_out -> navController.navigate(R.id.nav_rent_out)
            }
            // Close the drawer after selection
            drawerLayout.closeDrawers()
            true
        }

        // Set up the action bar with the navController
        setupActionBarWithNavController(navController, appBarConfiguration)

        // Link the NavigationView with the NavController
        navView.setupWithNavController(navController)
    }

    private fun addSampleData() {
        val appliance = hashMapOf(
            "applianceName" to "Washing Machine",
            "dailyRate" to 15.00,
            "category" to "Laundry",
            "condition" to "Excellent",
            "description" to "7kg Front Load Washing Machine, Energy Efficient",
            "availability" to true
        )

        db.collection("appliances")
            .add(appliance)
            .addOnSuccessListener { documentReference ->
                Snackbar.make(
                    binding.root,
                    "Added appliance with ID: ${documentReference.id}",
                    Snackbar.LENGTH_LONG
                ).show()
            }
            .addOnFailureListener { e ->
                Snackbar.make(
                    binding.root,
                    "Error adding appliance: ${e.message}",
                    Snackbar.LENGTH_LONG
                ).show()
            }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }
}
