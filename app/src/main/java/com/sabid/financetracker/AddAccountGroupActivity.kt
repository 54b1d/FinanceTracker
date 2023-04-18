package com.sabid.financetracker

import android.database.sqlite.SQLiteException
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.sabid.financetracker.databinding.ActivityAddAccountGroupBinding

class AddAccountGroupActivity : AppCompatActivity() {
    lateinit var binding: ActivityAddAccountGroupBinding
    lateinit var db: DBHelper
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddAccountGroupBinding.inflate(layoutInflater)
        setContentView(binding.root)
        db = DBHelper(applicationContext)
        binding.btnAddGroup.setOnClickListener {
            val groupName = binding.editGroupName.text.toString().trim()
            if (groupName.isNotBlank()) {
                try {
                    if (db.insertAccountGroup(groupName)) {
                        Toast.makeText(this, "Insert successful", Toast.LENGTH_SHORT).show()
                        finish()
                    } else {
                        Toast.makeText(this, "Failed to insert group name", Toast.LENGTH_LONG)
                            .show()
                    }
                } catch (e: SQLiteException) {
                    e.printStackTrace()
                }
            } else {
                Toast.makeText(this, "Type an account group name", Toast.LENGTH_LONG).show()
            }
        }
    }
}