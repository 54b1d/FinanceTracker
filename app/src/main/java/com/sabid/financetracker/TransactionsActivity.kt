package com.sabid.financetracker

import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.preference.PreferenceManager
import android.text.InputType
import android.view.Menu
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import com.sabid.financetracker.databinding.ActivityTransactionsBinding
import java.util.Locale
import kotlin.math.roundToInt

class TransactionsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityTransactionsBinding
    private lateinit var db: DBHelper
    lateinit var listTransactions: List<Transaction>
    lateinit var adapter: TransactionAdapter
    lateinit var layoutManager: LinearLayoutManager
    lateinit var actionBar: ActionBar
    private var editor: SharedPreferences.Editor? = null

    private var isAllFabsVisible: Boolean? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTransactionsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        db = DBHelper(applicationContext)
        setSupportActionBar(binding.toolbar)
        actionBar = supportActionBar!!
        actionBar.title = "Finance Tracker"

        editor = PreferenceManager.getDefaultSharedPreferences(this).edit()

        listTransactions = db.getAllDetailedTransactions()
        adapter = TransactionAdapter(this, listTransactions, binding.rvTransactions)
        binding.rvTransactions.layoutManager = LinearLayoutManager(this)
        binding.rvTransactions.adapter = adapter

        binding.addGroupFab.visibility = View.GONE
        binding.addAccountFab.visibility = View.GONE
        binding.addGroupActionText.visibility = View.GONE
        binding.addAccountActionText.visibility = View.GONE
        isAllFabsVisible = false
        binding.addFab.shrink()

        binding.addFab.setOnClickListener {
            startActivity(Intent(this, AddTransactionActivity::class.java))
        }
        binding.addFab.setOnLongClickListener {
            isAllFabsVisible = if (!isAllFabsVisible!!) {

                binding.addGroupFab.show()
                binding.addAccountFab.show()
                binding.addGroupActionText.visibility = View.VISIBLE
                binding.addAccountActionText.visibility = View.VISIBLE

                binding.addFab.extend()

                true
            } else {

                binding.addGroupFab.hide()
                binding.addAccountFab.hide()
                binding.addGroupActionText.visibility = View.GONE
                binding.addAccountActionText.visibility = View.GONE

                binding.addFab.shrink()

                false
            }
            true
        }

        binding.addAccountFab.setOnClickListener {
            startActivity(Intent(this, AddAccountActivity::class.java))
        }

        binding.addGroupFab.setOnClickListener {
            startActivity(Intent(this, AddAccountGroupActivity::class.java))
        }
        binding.textDonatable.setOnClickListener {
            setDonatablePercentage()
        }

    }

    private fun setDonatablePercentage() {
        val v = EditText(this)
        v.inputType = InputType.TYPE_NUMBER_FLAG_SIGNED
        v.hint = "0.10"

        val builder = AlertDialog.Builder(this)
        builder.setTitle("Set Donatable Percentage")
            .setView(v)
            .setNegativeButton(getString(android.R.string.cancel)) { d: DialogInterface, _: Int -> d.dismiss() }
            .setPositiveButton(getString(android.R.string.ok)) { _: DialogInterface?, _: Int ->
                val percentage = v.text.toString()
                if (percentage.isNotBlank() && percentage.toDoubleOrNull() != null) {
                    editor?.let {
                        it.putFloat("donatablePercentage", percentage.toFloat())
                        it.apply()
                    }
                    Toast.makeText(this, "Set to $percentage", Toast.LENGTH_SHORT).show()
                    reloadSummary()
                } else {
                    Toast.makeText(this, "Invalid", Toast.LENGTH_SHORT).show()
                }
            }
            .show()
    }

    override fun onResume() {
        super.onResume()
        reloadRvTransactions()
        reloadSummary()
    }

    private fun reloadSummary() {
        val c = db.getTransactionSummary()
        c.moveToFirst()
        val income = c.getDouble(0)
        val expense = c.getDouble(1)
        val donation = c.getDouble(2)
        c.close()
        val donationPercentage = PreferenceManager.getDefaultSharedPreferences(this)
            .getFloat("donatablePercentage", 0.1F)
        val donatable = income * donationPercentage
        val donationLeft = donatable - donation
        val leftOver = income - expense - donation
        binding.textIncome.text = "Income: $income"
        binding.textExpense.text = "Expense: $expense"
        binding.textDonated.text = "Donated: $donation"
        binding.textDonatable.text =
            "Donatable ($donationPercentage): ${donatable.roundToInt()}"
        binding.textDonationLeft.text = "Donation Left: ${donationLeft.roundToInt()}"
        binding.textLeft.text = "Leftover: $leftOver"
    }

    private fun reloadRvTransactions() {
        listTransactions = db.getAllDetailedTransactions()
        adapter = TransactionAdapter(this, listTransactions, binding.rvTransactions)
        binding.rvTransactions.adapter = adapter
        adapter.notifyDataSetChanged()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_transactions_activity, menu)
        val searchItem = menu.findItem(R.id.action_filter_transactions)
        val searchAccount = searchItem.actionView as SearchView?
        searchAccount!!.clearFocus()
        searchAccount.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String): Boolean {
                filterTransactionsList(newText)
                return true
            }
        })
        return super.onCreateOptionsMenu(menu)
    }

    private fun filterTransactionsList(text: String) {
        val filterPattern = text.lowercase(Locale.getDefault()).trim { it <= ' ' }
        val filteredTransactionList: MutableList<Transaction> = ArrayList()
        for (transaction in listTransactions) {
            if (transaction.accountName.lowercase(Locale.getDefault())
                    .contains(filterPattern) || transaction.transactionTypeName.lowercase(Locale.getDefault())
                    .contains(filterPattern) || transaction.date.lowercase(Locale.getDefault())
                    .contains(filterPattern) || transaction.narration.lowercase(Locale.getDefault())
                    .contains(filterPattern) || String.format("%.0f", transaction.amount)
                    .contains(filterPattern)
            ) {
                filteredTransactionList.add(transaction)
            }
        }
        if (filteredTransactionList.isNotEmpty()) {
            //pass filtered list to adapter
            adapter.setFilteredList(filteredTransactionList)
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (isAllFabsVisible == true) {
            binding.addGroupFab.hide()
            binding.addAccountFab.hide()
            binding.addGroupActionText.visibility = View.GONE
            binding.addAccountActionText.visibility = View.GONE
            binding.addFab.shrink()
            isAllFabsVisible = false
        } else {
            onBackPressedDispatcher.onBackPressed()
        }
    }
}