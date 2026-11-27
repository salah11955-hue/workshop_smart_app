
package com.workshop.smartapp.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.workshop.smartapp.model.Customer
import com.workshop.smartapp.model.Diagnostic
import com.workshop.smartapp.model.Invoice
import com.workshop.smartapp.model.Part

class WorkshopDatabase(context: Context) :
    SQLiteOpenHelper(context, "workshop_data.db", null, 1) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE customers (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                phone TEXT NOT NULL,
                car_model TEXT,
                plate_number TEXT,
                notes TEXT,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE invoices (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                customer_id INTEGER NOT NULL,
                service_details TEXT,
                parts_cost REAL,
                labor_cost REAL,
                total_amount REAL,
                invoice_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY(customer_id) REFERENCES customers(id)
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE diagnostics (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                customer_id INTEGER NOT NULL,
                system_type TEXT,
                dtc_codes TEXT,
                technician_notes TEXT,
                check_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY(customer_id) REFERENCES customers(id)
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE inventory (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                part_number TEXT UNIQUE,
                part_name TEXT NOT NULL,
                category TEXT,
                quantity INTEGER DEFAULT 0,
                unit_price REAL DEFAULT 0.0,
                last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // زد إصدار قاعدة البيانات وأضف migrations هنا عند تغيير المخطط.
    }

    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.setForeignKeyConstraintsEnabled(true)
    }

    fun addCustomer(
        name: String,
        phone: String,
        car: String,
        plate: String,
        notes: String
    ) {
        val values = ContentValues().apply {
            put("name", name)
            put("phone", phone)
            put("car_model", car)
            put("plate_number", plate)
            put("notes", notes)
        }
        writableDatabase.insertOrThrow("customers", null, values)
    }

    fun addDiagnostic(
        customerId: Long,
        system: String,
        codes: String,
        notes: String
    ) {
        val values = ContentValues().apply {
            put("customer_id", customerId)
            put("system_type", system)
            put("dtc_codes", codes)
            put("technician_notes", notes)
        }
        writableDatabase.insertOrThrow("diagnostics", null, values)
    }

    fun addInvoice(
        customerId: Long,
        details: String,
        parts: Double,
        labor: Double
    ) {
        val values = ContentValues().apply {
            put("customer_id", customerId)
            put("service_details", details)
            put("parts_cost", parts)
            put("labor_cost", labor)
            put("total_amount", parts + labor)
        }
        writableDatabase.insertOrThrow("invoices", null, values)
    }

    fun addPart(
        number: String,
        name: String,
        category: String,
        quantity: Int,
        price: Double
    ) {
        val db = writableDatabase
        db.beginTransaction()

        try {
            val cursor = db.rawQuery(
                "SELECT id FROM inventory WHERE part_number = ?",
                arrayOf(number)
            )
            val exists = cursor.use { it.moveToFirst() }

            if (exists) {
                db.execSQL(
                    """
                    UPDATE inventory
                    SET part_name = ?,
                        category = ?,
                        quantity = quantity + ?,
                        unit_price = ?,
                        last_updated = CURRENT_TIMESTAMP
                    WHERE part_number = ?
                    """.trimIndent(),
                    arrayOf(name, category, quantity, price, number)
                )
            } else {
                val values = ContentValues().apply {
                    put("part_number", number)
                    put("part_name", name)
                    put("category", category)
                    put("quantity", quantity)
                    put("unit_price", price)
                }
                db.insertOrThrow("inventory", null, values)
            }

            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun customers(): List<Customer> =
        readableDatabase.rawQuery(
            """
            SELECT id, name, phone, car_model, plate_number, notes, created_at
            FROM customers
            ORDER BY id DESC
            """.trimIndent(),
            null
        ).use { cursor ->
            cursor.mapRows {
                Customer(
                    id = it.long("id"),
                    name = it.text("name"),
                    phone = it.text("phone"),
                    car = it.text("car_model"),
                    plate = it.text("plate_number"),
                    notes = it.text("notes"),
                    createdAt = it.text("created_at")
                )
            }
        }

    fun diagnostics(): List<Diagnostic> =
        readableDatabase.rawQuery(
            """
            SELECT d.id,
                   c.name AS customer_name,
                   d.system_type,
                   d.dtc_codes,
                   d.technician_notes,
                   d.check_date
            FROM diagnostics d
            LEFT JOIN customers c ON c.id = d.customer_id
            ORDER BY d.id DESC
            """.trimIndent(),
            null
        ).use { cursor ->
            cursor.mapRows {
                Diagnostic(
                    id = it.long("id"),
                    customerName = it.text("customer_name"),
                    system = it.text("system_type"),
                    codes = it.text("dtc_codes"),
                    notes = it.text("technician_notes"),
                    date = it.text("check_date")
                )
            }
        }

    fun invoices(): List<Invoice> =
        readableDatabase.rawQuery(
            """
            SELECT i.id,
                   c.name AS customer_name,
                   c.car_model,
                   i.service_details,
                   i.parts_cost,
                   i.labor_cost,
                   i.total_amount,
                   i.invoice_date
            FROM invoices i
            LEFT JOIN customers c ON c.id = i.customer_id
            ORDER BY i.id DESC
            """.trimIndent(),
            null
        ).use { cursor ->
            cursor.mapRows {
                Invoice(
                    id = it.long("id"),
                    customerName = it.text("customer_name"),
                    car = it.text("car_model"),
                    details = it.text("service_details"),
                    parts = it.double("parts_cost"),
                    labor = it.double("labor_cost"),
                    total = it.double("total_amount"),
                    date = it.text("invoice_date")
                )
            }
        }

    fun inventory(): List<Part> =
        readableDatabase.rawQuery(
            """
            SELECT part_number, part_name, category, quantity, unit_price
            FROM inventory
            ORDER BY part_name
            """.trimIndent(),
            null
        ).use { cursor ->
            cursor.mapRows {
                Part(
                    number = it.text("part_number"),
                    name = it.text("part_name"),
                    category = it.text("category"),
                    quantity = it.int("quantity"),
                    price = it.double("unit_price")
                )
            }
        }
}

private inline fun <T> Cursor.mapRows(block: (Cursor) -> T): List<T> {
    val result = mutableListOf<T>()
    while (moveToNext()) {
        result.add(block(this))
    }
    return result
}

private fun Cursor.text(column: String): String =
    getString(getColumnIndexOrThrow(column)) ?: ""

private fun Cursor.long(column: String): Long =
    getLong(getColumnIndexOrThrow(column))

private fun Cursor.int(column: String): Int =
    getInt(getColumnIndexOrThrow(column))

private fun Cursor.double(column: String): Double =
    getDouble(getColumnIndexOrThrow(column))
