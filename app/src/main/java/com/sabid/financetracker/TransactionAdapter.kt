package com.sabid.financetracker

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class TransactionAdapter(
    var context: Context,
    var transactionList: List<Transaction>,
    var rvTransactions: RecyclerView
) : RecyclerView.Adapter<TransactionAdapter.ViewHolder>() {
    private val onClickListener: View.OnClickListener = MyOnClickListener()

    override fun onCreateViewHolder(ViewGroup: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(context)
        val view = inflater.inflate(R.layout.item_transaction, ViewGroup, false)
        view.setOnClickListener(onClickListener)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        val transaction = transactionList[position]

        when (transaction.transactionType) {
            1 -> viewHolder.itemView.setBackgroundResource(R.drawable.round_border_income)
            2 -> viewHolder.itemView.setBackgroundResource(R.drawable.round_border_expense)
            3 -> viewHolder.itemView.setBackgroundResource(R.drawable.round_border_donate)
        }

        viewHolder.textDate.text = transaction.date
        viewHolder.textAccountName.text = transaction.accountName
        viewHolder.textAccountGroup.text = transaction.transactionTypeName
        viewHolder.textAmount.text = transaction.amount.toString()
        viewHolder.textNarration.text = transaction.narration
        viewHolder.textAccountName.setOnClickListener {
            val intent = Intent(context, LedgerActivity::class.java)
            intent.putExtra("accountId", transaction.accountId)
            intent.putExtra("accountName", transaction.accountName)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int {
        return transactionList.size
    }

    fun setFilteredList(filteredTransactionList: List<Transaction>) {
        transactionList = filteredTransactionList
        notifyDataSetChanged()
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var textDate: TextView
        var textAccountName: TextView
        var textAccountGroup: TextView
        var textAmount: TextView
        var textNarration: TextView

        init {
            textAccountName = itemView.findViewById(R.id.text_account_name)
            textAccountGroup = itemView.findViewById(R.id.text_transaction_type)
            textAmount = itemView.findViewById(R.id.text_amount)
            textDate = itemView.findViewById(R.id.text_date)
            textNarration = itemView.findViewById(R.id.text_narration)
        }
    }

    private inner class MyOnClickListener : View.OnClickListener {
        override fun onClick(view: View) {
            val itemPosition = rvTransactions.getChildLayoutPosition(view)
            val transactionId = transactionList[itemPosition].id
            val intent = Intent(context, AddTransactionActivity::class.java)
            intent.putExtra("transaction_id", transactionId)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            if (transactionId != 0) {
                context.startActivity(intent)
            }
        }
    }

    companion object {
        private const val TAG = "TransactionsAdapter"
    }
}