package com.sabid.financetracker

import android.os.Bundle
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.sabid.financetracker.databinding.ActivityLedgerBinding

class LedgerActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLedgerBinding
    private lateinit var db: DBHelper
    private var accountId: Int = 0
    private lateinit var actionBar: ActionBar
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLedgerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        actionBar = supportActionBar!!
        db = DBHelper(applicationContext)
        accountId = intent.getIntExtra("accountId", 0)
        actionBar.title = intent.getStringExtra("accountName")

    }

    override fun onResume() {
        super.onResume()
        reloadRvTransactions()
    }

    private fun reloadRvTransactions() {
        val listTransactions = db.getAllDetailedTransactionsOf(accountId)

        val layoutManager = LinearLayoutManager(this)
        binding.rvLedgerTransactions.layoutManager = layoutManager
        binding.rvLedgerTransactions.adapter =
            TransactionAdapter(this, listTransactions, binding.rvLedgerTransactions)
    }
}