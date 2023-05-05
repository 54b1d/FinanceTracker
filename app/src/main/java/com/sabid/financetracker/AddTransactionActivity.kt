package com.sabid.financetracker

import android.app.DatePickerDialog
import android.os.Bundle
import android.preference.PreferenceManager
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.DatePicker
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.sabid.financetracker.databinding.ActivityAddTransactionBinding
import java.time.LocalDate
import java.time.format.DateTimeFormatter


class AddTransactionActivity : AppCompatActivity() {
    lateinit var binding: ActivityAddTransactionBinding
    var currentDate: LocalDate = LocalDate.now()
    private val year = currentDate.year
    private val month = currentDate.monthValue
    val day = currentDate.dayOfMonth
    private lateinit var listAccounts: List<Account>
    private var updatingTransactionId: Int = 0
    private lateinit var db: DBHelper
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddTransactionBinding.inflate(layoutInflater)
        setContentView(binding.root)
        db = DBHelper(applicationContext)
        listAccounts = db.getAllAccounts()

        loadEditAutoName()


        val dateFormat = DateTimeFormatter.ofPattern("yyyy-M-d")

        binding.textDate.text = currentDate.toString()
        binding.textDate.setOnClickListener {
            // pop up date picker
            val datePickerDialog = DatePickerDialog(
                this,
                android.R.style.ThemeOverlay_Material_Dialog,
                { _: DatePicker?, year1: Int, month1: Int, day1: Int ->
                    val lt = LocalDate.parse("$year1-${month1 + 1}-$day1", dateFormat)
                    binding.textDate.text = lt.toString()
                },
                year,
                month - 1,
                day
            )
            datePickerDialog.show()
        }

        binding.btnAddTransaction.setOnClickListener { insertTransaction() }
        binding.btnUpdateTransaction.setOnClickListener { updateTransaction() }
        binding.btnDeleteTransaction.setOnClickListener { deleteTransaction() }
        binding.checkboxDonate.setOnCheckedChangeListener { _, isChecked ->
            binding.editDonatablePercent.isEnabled = isChecked
            if (isChecked) binding.editDonatablePercent.visibility =
                View.VISIBLE else binding.editDonatablePercent.visibility = View.INVISIBLE
        }
        binding.rbIncome.setOnCheckedChangeListener { _, isChecked ->
            binding.checkboxDonate.isChecked = isChecked
            // todo set percentage from shared pref if empty
            if (binding.editDonatablePercent.text.isBlank()) {
                binding.editDonatablePercent.setText(
                    PreferenceManager.getDefaultSharedPreferences(this)
                        .getFloat("donatablePercentage", 0f).toDouble().toString()
                )
            }
        }
        binding.rbExpense.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) binding.checkboxDonate.isChecked = false
        }
        binding.rbDonation.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) binding.checkboxDonate.isChecked = false
        }

        if (intent.hasExtra("transaction_id")) {
            fillFields()
        }
    }

    private fun fillFields() {
        updatingTransactionId = intent.getIntExtra("transaction_id", 0)
        binding.btnAddTransaction.visibility = View.GONE
        binding.btnUpdateTransaction.visibility = View.VISIBLE
        binding.btnDeleteTransaction.visibility = View.VISIBLE
        if (updatingTransactionId != 0) {
            val transaction = db.getTransactionDetails(updatingTransactionId)!!
            binding.textDate.text = transaction.date
            binding.editAccountName.setText(transaction.accountName)
            binding.editAmount.setText(transaction.amount.toString())
            binding.editDonatablePercent.setText(transaction.donatePercent.toString())
            binding.checkboxDonate.isChecked = transaction.donatePercent != 0.0
            binding.editNarration.setText(transaction.narration)
            setTransactionType(transaction.transactionType)
        }
    }

    private fun getTransactionTypeId(): Int {
        var transactionType = 0
        if (binding.rbIncome.isChecked) {
            transactionType = 1
        } else if (binding.rbExpense.isChecked) {
            transactionType = 2
        } else if (binding.rbDonation.isChecked) {
            transactionType = 3
        }
        return transactionType
    }

    private fun setTransactionType(id: Int) {
        if (id == 1) {
            binding.rbIncome.isChecked = true
        } else if (id == 2) {
            binding.rbExpense.isChecked = true
        } else if (id == 3) {
            binding.rbDonation.isChecked = true
        }
    }

    private fun insertTransaction() {

        // check for valid account name
        val accountId = db.getAccountByName(binding.editAccountName.text.toString().trim())?.id
        if (accountId != null && getTransactionTypeId() != 0) {
            if (db.insertTransaction(
                    binding.textDate.text.toString(),
                    getTransactionTypeId(),
                    accountId,
                    binding.editAmount.text.toString().toDouble(),
                    if (getTransactionTypeId() == 1 && binding.checkboxDonate.isChecked && binding.editDonatablePercent.text.toString()
                            .trim().isNotBlank()
                    ) binding.editDonatablePercent.text.toString().toDouble() else 0.0,
                    binding.editNarration.text.toString().trim()
                )
            ) {
                Toast.makeText(this, "Added Transaction", Toast.LENGTH_SHORT).show()
                finish()
            } else {
                Toast.makeText(this, "Failed to add", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(
                this, "Select Transaction Type and\nInsert valid info", Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun updateTransaction() {
        // check for valid account name
        val accountId = db.getAccountByName(binding.editAccountName.text.toString().trim())?.id
        if (accountId != null && getTransactionTypeId() != 0) {
            if (db.updateTransaction(
                    updatingTransactionId,
                    binding.textDate.text.toString(),
                    getTransactionTypeId(),
                    accountId,
                    binding.editAmount.text.toString().toDouble(),
                    if (binding.checkboxDonate.isChecked && binding.editDonatablePercent.text.toString()
                            .trim().isNotBlank()
                    ) binding.editDonatablePercent.text.toString().toDouble() else 0.0,
                    binding.editNarration.text.toString().trim()
                )
            ) {
                Toast.makeText(this, "Updated Transaction", Toast.LENGTH_SHORT).show()
                finish()
            } else {
                Toast.makeText(this, "Failed to update", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(
                this, "Select Transaction Type and\nInsert valid info", Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun deleteTransaction() {
        if (db.deleteTransaction(updatingTransactionId)) {
            Toast.makeText(this, "Transaction Deleted", Toast.LENGTH_SHORT).show()
            finish()
        } else {
            Toast.makeText(this, "Unable to delete", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadEditAutoName() {
        val accountNames = mutableListOf<String>()
        accountNames.add("No Account")
        //Toast.makeText(this, "${listAccounts.size}", Toast.LENGTH_SHORT).show()
        if (listAccounts.isNotEmpty()) {
            Log.d(javaClass.name, "loadEditAutoName: size of accounts list ${listAccounts.size}")
            accountNames.clear()
            for (account in listAccounts) {
                accountNames.add(account.name)
            }
        }
        val adapter = ArrayAdapter(
            this, android.R.layout.select_dialog_item, accountNames
        )
        binding.editAccountName.threshold = 1
        binding.editAccountName.setAdapter(adapter)
    }
}