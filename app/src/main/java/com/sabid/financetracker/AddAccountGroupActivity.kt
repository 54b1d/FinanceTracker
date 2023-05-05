package com.sabid.financetracker

import android.R
import android.content.Intent
import android.database.sqlite.SQLiteException
import android.os.Bundle
import android.widget.ArrayAdapter
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

        val listAccountGroups = db.getAllAccountGroups()
        val groupNames = mutableListOf<String>()
        if (listAccountGroups.isNotEmpty()) {
            groupNames.clear()
            for (group in listAccountGroups) {
                groupNames.add(group)
            }
        }
        binding.listAccountGroups.adapter = ArrayAdapter(
            this, R.layout.select_dialog_item, groupNames
        )
    }
}