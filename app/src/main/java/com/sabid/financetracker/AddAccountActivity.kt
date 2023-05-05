package com.sabid.financetracker

import android.content.Intent
import android.database.sqlite.SQLiteException
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.sabid.financetracker.databinding.ActivityAddAccountBinding

class AddAccountActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAddAccountBinding
    lateinit var db: DBHelper
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddAccountBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = DBHelper(applicationContext)
        loadEditAutoGroupName()
        binding.btnAddAccount.setOnClickListener {
            addAccountToDB()
        }
        val listAccounts = db.getAllAccounts()
        val accountNames = mutableListOf<String>()
        if (listAccounts.isNotEmpty()) {
            accountNames.clear()
            for (account in listAccounts) {
                accountNames.add(account.name)
            }
        }
        binding.listAccounts.adapter = ArrayAdapter(
            this, android.R.layout.select_dialog_item, accountNames
        )
        binding.listAccounts.setOnItemClickListener { parent, _, position, _ ->
            val account = db.getAccountByName(parent.adapter.getItem(position).toString())
            startActivity(
                Intent(this, LedgerActivity::class.java).putExtra(
                    "accountId", account?.id
                ).putExtra("accountName", account?.name)
            )
        }
    }

    private fun addAccountToDB() {
        val name: String = binding.editAccountName.text.toString().trim()
        val groupName: String = binding.editAccountGroupName.text.toString().trim()
        var groupId = 0
        if (groupName.isNotBlank()) {
            // get group id
            groupId = db.getAccountGroupFromName(groupName)
        }

        if (name.isNotBlank() && groupId != 0) {
            try {
                if (db.insertAccountData(name, groupId)) {
                    Toast.makeText(this, "Added Account Successfully!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Failed", Toast.LENGTH_SHORT).show()
                }
                finish()
            } catch (e: SQLiteException) {
                Toast.makeText(this, "Failed to add account!", Toast.LENGTH_LONG).show()
                e.printStackTrace()
            }
        } else {
            Toast.makeText(this, "Invalid Name or, Account Group", Toast.LENGTH_LONG).show()
        }
    }

    private fun loadEditAutoGroupName() {
        val groupNames = db.getAllAccountGroups()
        val adapter = ArrayAdapter(
            this, android.R.layout.select_dialog_item, groupNames
        )
        binding.editAccountGroupName.threshold = 1
        binding.editAccountGroupName.setAdapter(adapter)
    }
}