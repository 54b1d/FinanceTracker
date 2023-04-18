package com.sabid.financetracker

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.net.Uri
import android.preference.PreferenceManager
import android.util.Log
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.OutputStream
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

/**
 * Created by sabid on 4/27/22.
 * from Khotian Project
 */
class DBHelper(private val context: Context) : SQLiteOpenHelper(
    context.applicationContext, DATABASE_NAME, null, databaseVersion
) {
    companion object {
        //Database Name
        const val DATABASE_NAME = "userdata.db"
        private const val TAG = "DBHelper"

        //Database Version
        const val databaseVersion = 1
    }

    // tables declaration
    private val tTransactions = "transactions"
    private val tTransactionTypes = "transaction_types"
    private val tAccounts = "accounts"
    private val tAccountGroups = "account_groups"
    private val tTransactionsDetailed = "transactions_detailed"

    private val cTransactionsDetailedDate = "transactionDate"
    private val cTransactionsDetailedId = "transactionId"
    private val cTransactionsDetailedAccountId = "transactionAccountId"

    private val cAccountId = "id"
    private val cAccountName = "name"
    private val cAccountParentGroup = "parent_group"

    private val cAccountGroupId = "id"
    private val cAccountGroupName = "name"

    private val cTransactionTypeId = "id"
    private val cTransactionTypeName = "name"

    private val cTransactionId = "id"
    private val cTransactionDate = "date"
    private val cTransactionTransactionType = "transaction_type"
    private val cTransactionAccountId = "account_id"
    private val cTransactionAmount = "amount"
    private val cTransactionNarration = "narration"

    override fun onCreate(db: SQLiteDatabase) {
        // Setup Database

        db.execSQL("CREATE TABLE [$tAccounts] ([$cAccountId] INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL UNIQUE,[$cAccountName] TEXT NOT NULL UNIQUE,[$cAccountParentGroup] INTEGER NOT NULL,FOREIGN KEY([$cAccountParentGroup]) REFERENCES [$tAccountGroups]([$cAccountGroupId]));")

        db.execSQL("CREATE TABLE [$tAccountGroups] ([$cAccountGroupId] INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL UNIQUE, [$cAccountGroupName] TEXT NOT NULL UNIQUE);")

        db.execSQL("CREATE TABLE [$tTransactions] ([$cTransactionId] INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL UNIQUE, [$cTransactionDate] DATE NOT NULL,[$cTransactionTransactionType] INTEGER NOT NULL,[$cTransactionAccountId] INTEGER NOT NULL,[$cTransactionAmount] DOUBLE NOT NULL,[$cTransactionNarration] TEXT,FOREIGN KEY([$cTransactionTransactionType]) REFERENCES [$tTransactionTypes]([$cTransactionTypeId]),FOREIGN KEY([$cTransactionAccountId]) REFERENCES [$tAccounts]([$cAccountId]));")

        db.execSQL("CREATE TABLE [$tTransactionTypes] ([$cTransactionTypeId] INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, [$cTransactionTypeName] TEXT NOT NULL UNIQUE);")

        db.execSQL("INSERT INTO $tTransactionTypes VALUES (1, 'Income'),(2, 'Expense'), (3, 'Donation')")

        db.execSQL(
            """
            CREATE VIEW if not EXISTS transactions_detailed
            AS
            SELECT transactions.id as transactionId, transactions.date as transactionDate, transactions.account_id as transactionAccountId,
            accounts.name as transactionAccountName,
            transactions.transaction_type as transactionType,
            transaction_types.name as transactionTypeName,
            transactions.amount,
            transactions.narration FROM transactions
            inner join accounts on accounts.id = transactions.account_id
            inner join transaction_types on transaction_types.id= transactions.transaction_type
            ORDER by transactionDate ASC;
        """.trimIndent()
        )

        db.execSQL(
            """
            CREATE view summary
            as
            select
            (select ifnull(sum(amount), 0) from transactions where transaction_type=1) as income,
            (SELECT ifnull(sum(amount), 0) from transactions where transaction_type=2) as expense,
            (SELECT ifnull(sum(amount), 0) from transactions where transaction_type=3) as donation;
        """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {}
    fun deleteData(mContext: Context) {
        mContext.deleteDatabase(DATABASE_NAME)
    }

    fun insertAccountGroup(
        groupName: String
    ): Boolean {
        val contentValues = ContentValues()
        contentValues.put(cAccountGroupName, groupName)
        val result = writableDatabase.insert(tAccountGroups, null, contentValues)
        return result != -1L
    }

    fun getAccountGroupFromName(groupName: String): Int {
        var groupId = 0
        val c = readableDatabase.rawQuery(
            "select $cAccountGroupId from $tAccountGroups where $cAccountGroupName=?",
            arrayOf(groupName)
        )
        if (c.moveToFirst()) {
            groupId = c.getInt(0)
        }
        c.close()
        return groupId
    }

    fun insertAccountData(
        accountName: String, accountParentGroupId: Int
    ): Boolean {
        val contentValues = ContentValues()
        contentValues.put(cAccountName, accountName)
        contentValues.put(cAccountParentGroup, accountParentGroupId)
        val result = writableDatabase.insert(tAccounts, null, contentValues)
        return result != -1L
    }

    fun updateAccountData(
        oldAccountName: String, accountName: String, accountParentGroupId: Int
    ): Boolean {
        val contentValues = ContentValues()
        contentValues.put(cAccountName, accountName)
        contentValues.put(cAccountGroupId, accountParentGroupId)
        val cursor = readableDatabase.rawQuery(
            "Select * from $tAccounts where $cAccountName=?", arrayOf(oldAccountName)
        )
        return if (cursor.count > 0) {
            val result = writableDatabase.update(
                tAccounts, contentValues, "$cAccountName=?", arrayOf(oldAccountName)
            ).toLong()
            cursor.close()
            result != -1L
        } else {
            cursor.close()
            false
        }
    }

    fun getAccountByName(name: String): Account? {
        val c = readableDatabase.rawQuery(
            "select * from $tAccounts where $cAccountName = ?", arrayOf(name)
        )
        var account: Account? = null
        if (c.moveToFirst()) {
            account = Account(c.getInt(0), c.getString(1), c.getInt(2))
        }
        c.close()
        return account
    }

    fun getAllAccounts(): List<Account> {
        val c = readableDatabase.rawQuery(
            "select $cAccountId, $cAccountName, $cAccountParentGroup from $tAccounts order by $cAccountName asc",
            null,
            null
        )

        val list = mutableListOf<Account>()

        if (c.moveToFirst()) {
            do {
                list.add(Account(c.getInt(0), c.getString(1), c.getInt(2)))
            } while (c.moveToNext())
        }
        c.close()
        return list
    }

    fun getAllAccountGroups(): List<String> {
        val c = readableDatabase.rawQuery(
            "select $cAccountGroupName from $tAccountGroups order by $cAccountName asc", null, null
        )

        val list = mutableListOf<String>()

        if (c.moveToFirst()) {
            do {
                list.add(c.getString(0))
            } while (c.moveToNext())
        }
        c.close()
        return list
    }

    fun insertTransaction(
        date: String, transactionType: Int, accountId: Int, amount: Double, narration: String
    ): Boolean {
        val contentValues = ContentValues()
        contentValues.put(cTransactionDate, date)
        contentValues.put(cTransactionTransactionType, transactionType)
        contentValues.put(cTransactionAccountId, accountId)
        contentValues.put(cTransactionAmount, amount)
        contentValues.put(cTransactionNarration, narration)
        val result = writableDatabase.insert(tTransactions, null, contentValues)
        return result != -1L
    }

    fun updateTransaction(
        transactionId: Int,
        date: String,
        transactionType: Int,
        accountId: Int,
        amount: Double,
        narration: String
    ): Boolean {
        val contentValues = ContentValues()
        contentValues.put(cTransactionDate, date)
        contentValues.put(cTransactionTransactionType, transactionType)
        contentValues.put(cTransactionAccountId, accountId)
        contentValues.put(cTransactionAmount, amount)
        contentValues.put(cTransactionNarration, narration)
        val result = writableDatabase.update(
            tTransactions, contentValues, "$cTransactionId=?", arrayOf(transactionId.toString())
        ).toLong()
        return result != -1L
    }

    fun deleteTransaction(transactionId: Int): Boolean {
        val result = writableDatabase.delete(
            tTransactions, "$cTransactionId=?", arrayOf(transactionId.toString())
        ).toLong()
        return result != -1L
    }

    fun getTransactionDetails(transactionId: Int): Transaction? {
        var transaction: Transaction? = null
        val c = readableDatabase.rawQuery(
            "select * from $tTransactionsDetailed where $cTransactionsDetailedId=?",
            arrayOf(transactionId.toString())
        )

        if (c.moveToFirst()) {
            transaction = Transaction(
                c.getInt(0),
                c.getString(1),
                c.getInt(2),
                c.getString(3),
                c.getInt(4),
                c.getString(5),
                c.getDouble(6),
                c.getString(7)
            )
        }
        c.close()
        return transaction
    }

    fun getTransactionSummary(): Cursor {
        return readableDatabase.rawQuery("select * from summary", arrayOf())
    }

    fun getAllDetailedTransactions(): List<Transaction> {
        val listTransaction = mutableListOf<Transaction>()
        val c = readableDatabase.rawQuery(
            "select * from $tTransactionsDetailed order by $cTransactionsDetailedDate desc, $cTransactionsDetailedId desc",
            arrayOf()
        )

        if (c.moveToFirst()) {
            do {
                listTransaction.add(
                    Transaction(
                        c.getInt(0),
                        c.getString(1),
                        c.getInt(2),
                        c.getString(3),
                        c.getInt(4),
                        c.getString(5),
                        c.getDouble(6),
                        c.getString(7)
                    )
                )
            } while (c.moveToNext())
        }
        c.close()

        return listTransaction
    }

    fun getAllDetailedTransactionsOf(accountId: Int): List<Transaction> {
        val listTransaction = mutableListOf<Transaction>()
        val c = readableDatabase.rawQuery(
            "select * from $tTransactionsDetailed where $cTransactionsDetailedAccountId=? order by $cTransactionsDetailedDate desc",
            arrayOf(accountId.toString())
        )

        if (c.moveToFirst()) {
            do {
                listTransaction.add(
                    Transaction(
                        c.getInt(0),
                        c.getString(1),
                        c.getInt(2),
                        c.getString(3),
                        c.getInt(4),
                        c.getString(5),
                        c.getDouble(6),
                        c.getString(7)
                    )
                )
            } while (c.moveToNext())
        }
        c.close()

        return listTransaction
    }

    /**
     * Backup APP DATABASE method
     *
     * @param outFileName = File name for Backup
     */
    fun backup(outFileName: String) {
        //database path
        val inFileName = context.getDatabasePath(DATABASE_NAME).absolutePath
        try {
            Log.i(TAG, "Backing up DATABASE")
            val dbFile = File(inFileName)
            val fis = FileInputStream(dbFile)
            // Open the empty db as the output stream
            val output: OutputStream = FileOutputStream("$outFileName.gz")
            // Transfer bytes from the input file to the output file
            val buffer = ByteArray(1024)
            var length: Int
            val gos = GZIPOutputStream(output)
            while (fis.read(buffer).also { length = it } > 0) {
                gos.write(buffer, 0, length)
            }
            // Close the streams
            output.flush()
            gos.flush()
            gos.close()
            output.close()
            fis.close()
            Log.i(TAG, "DATABASE Backup Completed")
        } catch (e: Exception) {
            Log.w(TAG, "DATABASE Backup Failed")
            e.printStackTrace()
        }
    }

    /**
     * Restores APP DATABASE method
     *
     * @param inFileUri = Selected DATABASE Backup File from Storage
     */
    fun importDB(inFileUri: Uri) {
        val outFileName = context.getDatabasePath(DATABASE_NAME).toString()
        try {
            Log.i(TAG, "Restoring DATABASE")
            //File dbFile = new File(inFileUri);
            val fis = context.contentResolver.openInputStream(inFileUri)
            // Open the empty db as the output stream
            val output: OutputStream = FileOutputStream(outFileName)
            // Transfer bytes from the input file to the output file
            val buffer = ByteArray(1024)
            var length: Int
            if (inFileUri.toString().contains(".gz")) {
                val gis = GZIPInputStream(fis)
                Log.i(TAG, "Restoring Compressed Database...")
                while (gis.read(buffer).also { length = it } > 0) {
                    output.write(buffer, 0, length)
                }
                gis.close()
            } else if (fis != null) {
                Log.i(TAG, "Restoring RAW Database...")
                while (fis.read(buffer).also { length = it } > 0) {
                    output.write(buffer, 0, length)
                }
            }
            // Close the streams
            output.flush()
            output.close()
            val editor = PreferenceManager.getDefaultSharedPreferences(
                context
            ).edit()
            editor.putLong("localTimestamp", System.currentTimeMillis())
            editor.apply()
            Log.i(TAG, "DATABASE Restore Completed")
        } catch (e: Exception) {
            Log.w(TAG, "DATABASE Restore Failed")
            e.printStackTrace()
        }
    }

}