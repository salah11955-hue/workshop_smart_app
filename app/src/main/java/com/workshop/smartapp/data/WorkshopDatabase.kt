
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
    SQLiteOpenHelper(context, "workshop_data.db", null, 2) {

    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.setForeignKeyConstraintsEnabled(true)
    }

    override fun onCreate(db: SQLiteDatabase) {
        createCoreTables(db)
        createVehicleAndVisitTables(db)
        addVehicleLinksToRecords(db)
    }

    private fun createCoreTables(db: SQLiteDatabase) {
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

    private fun createVehicleAndVisitTables(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS vehicles (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                customer_id INTEGER NOT NULL,
                make_model TEXT NOT NULL,
                plate TEXT,
                vin TEXT,
                year TEXT,
                color TEXT,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY(customer_id) REFERENCES customers(id)
                    ON DELETE CASCADE
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS visits (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                customer_id INTEGER NOT NULL,
                vehicle_id INTEGER NOT NULL,
                odometer INTEGER DEFAULT 0,
                complaint TEXT,
                notes TEXT,
                status TEXT DEFAULT 'مفتوحة',
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY(customer_id) REFERENCES customers(id),
                FOREIGN KEY(vehicle_id) REFERENCES vehicles(id)
            )
            """.trimIndent()
        )
    }

    private fun addVehicleLinksToRecords(db: SQLiteDatabase) {
        db.execSQL("ALTER TABLE diagnostics ADD COLUMN vehicle_id INTEGER")
        db.execSQL("ALTER TABLE diagnostics ADD COLUMN visit_id INTEGER")
        db.execSQL("ALTER TABLE invoices ADD COLUMN vehicle_id INTEGER")
        db.execSQL("ALTER TABLE invoices ADD COLUMN visit_id INTEGER")
    }

    override fun onUpgrade(
        db: SQLiteDatabase,
        oldVersion: Int,
        newVersion: Int
    ) {
        if (oldVersion < 2) {
            createVehicleAndVisitTables(db)
            addVehicleLinksToRecords(db)

            // نقل بيانات السيارات القديمة إلى جدول المركبات دون حذف بيانات العملاء.
            db.execSQL(
                """
                INSERT INTO vehicles (
                    customer_id, make_model, plate
                )
                SELECT id, COALESCE(car_model, ''), COALESCE(plate_number, '')
                FROM customers
                WHERE TRIM(COALESCE(car_model, '')) <> ''
                   OR TRIM(COALESCE(plate_number, '')) <> ''
                """.trimIndent()
            )

            // ربط التقارير والفواتير القديمة بالمركبة التي نُقلت من سجل العميل.
            db.execSQL(
                """
                UPDATE diagnostics
                SET vehicle_id = (
                    SELECT MIN(v.id)
                    FROM vehicles v
                    WHERE v.customer_id = diagnostics.customer_id
                )
                WHERE EXISTS (
                    SELECT 1 FROM vehicles v
                    WHERE v.customer_id = diagnostics.customer_id
                )
                """.trimIndent()
            )

            db.execSQL(
                """
                UPDATE invoices
                SET vehicle_id = (
                    SELECT MIN(v.id)
                    FROM vehicles v
                    WHERE v.customer_id = invoices.customer_id
                )
                WHERE EXISTS (
                    SELECT 1 FROM vehicles v
                    WHERE v.customer_id = invoices.customer_id
                )
                """.trimIndent()
            )
        }
    }

    // العملاء

    fun addCustomer(
        name: String,
        phone: String,
        car: String,
        plate: String,
        notes: String
    ) {
        val db = writableDatabase
        db.beginTransaction()

        try {
            val customerValues = ContentValues().apply {
                put("name", name)
                put("phone", phone)
                put("car_model", car)
                put("plate_number", plate)
                put("notes", notes)
            }

            val customerId = db.insertOrThrow(
                "customers",
                null,
                customerValues
            )

            // الاحتفاظ ببيانات السيارة القديمة مع إنشاء سجل مركبة مستقل.
            if (car.isNotBlank() || plate.isNotBlank()) {
                val vehicleValues = ContentValues().apply {
                    put("customer_id", customerId)
                    put("make_model", car)
                    put("plate", plate)
                }
                db.insertOrThrow("vehicles", null, vehicleValues)
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

    // المركبات

    fun addVehicle(
        customerId: Long,
        makeModel: String,
        plate: String,
        vin: String = "",
        year: String = "",
        color: String = ""
    ): Long {
        val values = ContentValues().apply {
            put("customer_id", customerId)
            put("make_model", makeModel)
            put("plate", plate)
            put("vin", vin)
            put("year", year)
            put("color", color)
        }

        return writableDatabase.insertOrThrow(
            "vehicles",
            null,
            values
        )
    }

    fun vehicles(customerId: Long): List<VehicleRecord> =
        readableDatabase.rawQuery(
            """
            SELECT id, customer_id, make_model, plate, vin, year, color
            FROM vehicles
            WHERE customer_id = ?
            ORDER BY id DESC
            """.trimIndent(),
            arrayOf(customerId.toString())
        ).use { cursor ->
            cursor.mapRows {
                VehicleRecord(
                    id = it.long("id"),
                    customerId = it.long("customer_id"),
                    makeModel = it.text("make_model"),
                    plate = it.text("plate"),
                    vin = it.text("vin"),
                    year = it.text("year"),
                    color = it.text("color")
                )
            }
        }

    // زيارات الصيانة

    fun addVisit(
        customerId: Long,
        vehicleId: Long,
        odometer: Long,
        complaint: String,
        notes: String,
        status: String = "مفتوحة"
    ): Long {
        val values = ContentValues().apply {
            put("customer_id", customerId)
            put("vehicle_id", vehicleId)
            put("odometer", odometer)
            put("complaint", complaint)
            put("notes", notes)
            put("status", status)
        }

        return writableDatabase.insertOrThrow(
            "visits",
            null,
            values
        )
    }

    fun visits(customerId: Long): List<VisitRecord> =
        readableDatabase.rawQuery(
            """
            SELECT id, customer_id, vehicle_id, odometer,
                   complaint, notes, status, created_at
            FROM visits
            WHERE customer_id = ?
            ORDER BY id DESC
            """.trimIndent(),
            arrayOf(customerId.toString())
        ).use { cursor ->
            cursor.mapRows {
                VisitRecord(
                    id = it.long("id"),
                    customerId = it.long("customer_id"),
                    vehicleId = it.long("vehicle_id"),
                    odometer = it.long("odometer"),
                    complaint = it.text("complaint"),
                    notes = it.text("notes"),
                    status = it.text("status"),
                    date = it.text("created_at")
                )
            }
        }

    // تقارير الفحص

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

        writableDatabase.insertOrThrow(
            "diagnostics",
            null,
            values
        )
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

    // الفواتير

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

        writableDatabase.insertOrThrow(
            "invoices",
            null,
            values
        )
    }

    fun invoices(): List<Invoice> =
        readableDatabase.rawQuery(
            """
            SELECT i.id,
                   c.name AS customer_name,
                   COALESCE(v.make_model, c.car_model, '') AS car_model,
                   i.service_details,
                   i.parts_cost,
                   i.labor_cost,
                   i.total_amount,
                   i.invoice_date
            FROM invoices i
            LEFT JOIN customers c ON c.id = i.customer_id
            LEFT JOIN vehicles v ON v.id = i.vehicle_id
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

    // المخزون

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

// سجلات داخلية لنتائج المركبات والزيارات.
data class VehicleRecord(
    val id: Long,
    val customerId: Long,
    val makeModel: String,
    val plate: String,
    val vin: String,
    val year: String,
    val color: String
)

data class VisitRecord(
    val id: Long,
    val customerId: Long,
    val vehicleId: Long,
    val odometer: Long,
    val complaint: String,
    val notes: String,
    val status: String,
    val date: String
)

private inline fun <T> Cursor.mapRows(
    block: (Cursor) -> T
): List<T> {
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
