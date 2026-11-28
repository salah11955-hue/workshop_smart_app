
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
    SQLiteOpenHelper(context, "workshop_data.db", null, 3) {

    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.setForeignKeyConstraintsEnabled(true)
    }

    override fun onCreate(db: SQLiteDatabase) {
        createCoreTables(db)
        createVehicleAndVisitTables(db)
        addVehicleLinksToRecords(db)
        createAuditTable(db)
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

    private fun createAuditTable(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS audit_log (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                action TEXT NOT NULL,
                entity_type TEXT NOT NULL,
                entity_id TEXT,
                description TEXT NOT NULL,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(
        db: SQLiteDatabase,
        oldVersion: Int,
        newVersion: Int
    ) {
        if (oldVersion < 2) {
            createVehicleAndVisitTables(db)
            addVehicleLinksToRecords(db)

            db.execSQL(
                """
                INSERT INTO vehicles (customer_id, make_model, plate)
                SELECT id, COALESCE(car_model, ''), COALESCE(plate_number, '')
                FROM customers
                WHERE TRIM(COALESCE(car_model, '')) <> ''
                   OR TRIM(COALESCE(plate_number, '')) <> ''
                """.trimIndent()
            )

            db.execSQL(
                """
                UPDATE diagnostics
                SET vehicle_id = (
                    SELECT MIN(v.id) FROM vehicles v
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
                    SELECT MIN(v.id) FROM vehicles v
                    WHERE v.customer_id = invoices.customer_id
                )
                WHERE EXISTS (
                    SELECT 1 FROM vehicles v
                    WHERE v.customer_id = invoices.customer_id
                )
                """.trimIndent()
            )
        }

        if (oldVersion < 3) {
            createAuditTable(db)
        }
    }

    // ---------------------------------------------------------
    // سجل العمليات
    // ---------------------------------------------------------

    private fun log(
        db: SQLiteDatabase,
        action: String,
        entity: String,
        id: String?,
        description: String
    ) {
        val values = ContentValues().apply {
            put("action", action)
            put("entity_type", entity)
            put("entity_id", id)
            put("description", description)
        }
        db.insert("audit_log", null, values)
    }

    fun auditLog(limit: Int = 200): List<AuditRecord> =
        readableDatabase.rawQuery(
            """
            SELECT id, action, entity_type, entity_id,
                   description, created_at
            FROM audit_log
            ORDER BY id DESC
            LIMIT ?
            """.trimIndent(),
            arrayOf(limit.coerceIn(1, 1000).toString())
        ).use { cursor ->
            cursor.mapRows {
                AuditRecord(
                    id = it.long("id"),
                    action = it.text("action"),
                    entityType = it.text("entity_type"),
                    entityId = it.text("entity_id"),
                    description = it.text("description"),
                    date = it.text("created_at")
                )
            }
        }

    // ---------------------------------------------------------
    // العملاء
    // ---------------------------------------------------------

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
            val values = ContentValues().apply {
                put("name", name.trim())
                put("phone", phone.trim())
                put("car_model", car.trim())
                put("plate_number", plate.trim())
                put("notes", notes.trim())
            }

            val customerId = db.insertOrThrow("customers", null, values)

            if (car.isNotBlank() || plate.isNotBlank()) {
                val vehicle = ContentValues().apply {
                    put("customer_id", customerId)
                    put("make_model", car.trim())
                    put("plate", plate.trim())
                }
                db.insertOrThrow("vehicles", null, vehicle)
            }

            log(db, "إضافة", "عميل", customerId.toString(), "إضافة العميل: $name")
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun updateCustomer(
        id: Long,
        name: String,
        phone: String,
        car: String,
        plate: String,
        notes: String
    ): Boolean {
        val db = writableDatabase
        db.beginTransaction()

        try {
            val values = ContentValues().apply {
                put("name", name.trim())
                put("phone", phone.trim())
                put("car_model", car.trim())
                put("plate_number", plate.trim())
                put("notes", notes.trim())
            }

            val changed = db.update(
                "customers",
                values,
                "id = ?",
                arrayOf(id.toString())
            ) > 0

            if (!changed) return false

            // تحديث بيانات السيارة القديمة الأولى فقط.
            val firstVehicle = db.rawQuery(
                "SELECT id FROM vehicles WHERE customer_id = ? ORDER BY id ASC LIMIT 1",
                arrayOf(id.toString())
            ).use { if (it.moveToFirst()) it.getLong(0) else null }

            if (firstVehicle != null) {
                val vehicleValues = ContentValues().apply {
                    put("make_model", car.trim())
                    put("plate", plate.trim())
                }
                db.update(
                    "vehicles",
                    vehicleValues,
                    "id = ?",
                    arrayOf(firstVehicle.toString())
                )
            } else if (car.isNotBlank() || plate.isNotBlank()) {
                val vehicleValues = ContentValues().apply {
                    put("customer_id", id)
                    put("make_model", car.trim())
                    put("plate", plate.trim())
                }
                db.insertOrThrow("vehicles", null, vehicleValues)
            }

            log(db, "تعديل", "عميل", id.toString(), "تعديل بيانات العميل: $name")
            db.setTransactionSuccessful()
            return true
        } finally {
            db.endTransaction()
        }
    }

    /**
     * لا يحذف العميل إذا كانت له تقارير أو فواتير أو مركبات أو زيارات.
     * يعيد false عند وجود سجلات مرتبطة.
     */
    fun deleteCustomer(id: Long): Boolean {
        val db = writableDatabase
        db.beginTransaction()

        try {
            val related = db.rawQuery(
                """
                SELECT
                    (SELECT COUNT(*) FROM diagnostics WHERE customer_id = ?) +
                    (SELECT COUNT(*) FROM invoices WHERE customer_id = ?) +
                    (SELECT COUNT(*) FROM visits WHERE customer_id = ?) +
                    (SELECT COUNT(*) FROM vehicles WHERE customer_id = ?)
                """.trimIndent(),
                arrayOf(id.toString(), id.toString(), id.toString(), id.toString())
            ).use { if (it.moveToFirst()) it.getInt(0) else 0 }

            if (related > 0) return false

            val deleted = db.delete(
                "customers",
                "id = ?",
                arrayOf(id.toString())
            ) > 0

            if (deleted) {
                log(db, "حذف", "عميل", id.toString(), "حذف العميل رقم $id")
            }

            db.setTransactionSuccessful()
            return deleted
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

    // ---------------------------------------------------------
    // المركبات
    // ---------------------------------------------------------

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
            put("make_model", makeModel.trim())
            put("plate", plate.trim())
            put("vin", vin.trim())
            put("year", year.trim())
            put("color", color.trim())
        }

        val id = writableDatabase.insertOrThrow("vehicles", null, values)
        log(
            writableDatabase, "إضافة", "مركبة", id.toString(),
            "إضافة مركبة $makeModel للعميل رقم $customerId"
        )
        return id
    }

    fun updateVehicle(
        id: Long,
        makeModel: String,
        plate: String,
        vin: String,
        year: String,
        color: String
    ): Boolean {
        val values = ContentValues().apply {
            put("make_model", makeModel.trim())
            put("plate", plate.trim())
            put("vin", vin.trim())
            put("year", year.trim())
            put("color", color.trim())
        }

        val db = writableDatabase
        val changed = db.update(
            "vehicles",
            values,
            "id = ?",
            arrayOf(id.toString())
        ) > 0

        if (changed) {
            log(db, "تعديل", "مركبة", id.toString(), "تعديل بيانات المركبة رقم $id")
        }
        return changed
    }

    fun deleteVehicle(id: Long): Boolean {
        val db = writableDatabase
        val related = db.rawQuery(
            """
            SELECT
                (SELECT COUNT(*) FROM visits WHERE vehicle_id = ?) +
                (SELECT COUNT(*) FROM diagnostics WHERE vehicle_id = ?) +
                (SELECT COUNT(*) FROM invoices WHERE vehicle_id = ?)
            """.trimIndent(),
            arrayOf(id.toString(), id.toString(), id.toString())
        ).use { if (it.moveToFirst()) it.getInt(0) else 0 }

        if (related > 0) return false

        val deleted = db.delete(
            "vehicles",
            "id = ?",
            arrayOf(id.toString())
        ) > 0

        if (deleted) {
            log(db, "حذف", "مركبة", id.toString(), "حذف المركبة رقم $id")
        }
        return deleted
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

    // ---------------------------------------------------------
    // زيارات الصيانة
    // ---------------------------------------------------------

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

        val id = writableDatabase.insertOrThrow("visits", null, values)
        log(writableDatabase, "إضافة", "زيارة", id.toString(), "فتح زيارة صيانة رقم $id")
        return id
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

    // ---------------------------------------------------------
    // تقارير الفحص
    // ---------------------------------------------------------

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

        val id = writableDatabase.insertOrThrow("diagnostics", null, values)
        log(
            writableDatabase, "إضافة", "تقرير فحص", id.toString(),
            "إضافة تقرير فحص للعميل رقم $customerId"
        )
    }

    fun updateDiagnostic(
        id: Long,
        system: String,
        codes: String,
        notes: String
    ): Boolean {
        val values = ContentValues().apply {
            put("system_type", system.trim())
            put("dtc_codes", codes.trim())
            put("technician_notes", notes.trim())
        }

        val db = writableDatabase
        val changed = db.update(
            "diagnostics",
            values,
            "id = ?",
            arrayOf(id.toString())
        ) > 0

        if (changed) {
            log(db, "تعديل", "تقرير فحص", id.toString(), "تعديل تقرير الفحص رقم $id")
        }
        return changed
    }

    fun deleteDiagnostic(id: Long): Boolean {
        val db = writableDatabase
        val deleted = db.delete(
            "diagnostics",
            "id = ?",
            arrayOf(id.toString())
        ) > 0

        if (deleted) {
            log(db, "حذف", "تقرير فحص", id.toString(), "حذف تقرير الفحص رقم $id")
        }
        return deleted
    }

    fun diagnostics(): List<Diagnostic> =
        readableDatabase.rawQuery(
            """
            SELECT d.id, c.name AS customer_name, d.system_type,
                   d.dtc_codes, d.technician_notes, d.check_date
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

    // ---------------------------------------------------------
    // الفواتير
    // ---------------------------------------------------------

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

        val id = writableDatabase.insertOrThrow("invoices", null, values)
        log(
            writableDatabase, "إضافة", "فاتورة", id.toString(),
            "إضافة فاتورة للعميل رقم $customerId"
        )
    }

    fun updateInvoice(
        id: Long,
        details: String,
        parts: Double,
        labor: Double
    ): Boolean {
        val values = ContentValues().apply {
            put("service_details", details.trim())
            put("parts_cost", parts)
            put("labor_cost", labor)
            put("total_amount", parts + labor)
        }

        val db = writableDatabase
        val changed = db.update(
            "invoices",
            values,
            "id = ?",
            arrayOf(id.toString())
        ) > 0

        if (changed) {
            log(db, "تعديل", "فاتورة", id.toString(), "تعديل الفاتورة رقم $id")
        }
        return changed
    }

    fun deleteInvoice(id: Long): Boolean {
        val db = writableDatabase
        val deleted = db.delete(
            "invoices",
            "id = ?",
            arrayOf(id.toString())
        ) > 0

        if (deleted) {
            log(db, "حذف", "فاتورة", id.toString(), "حذف الفاتورة رقم $id")
        }
        return deleted
    }

    fun invoices(): List<Invoice> =
        readableDatabase.rawQuery(
            """
            SELECT i.id, c.name AS customer_name,
                   COALESCE(v.make_model, c.car_model, '') AS car_model,
                   i.service_details, i.parts_cost, i.labor_cost,
                   i.total_amount, i.invoice_date
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

    // ---------------------------------------------------------
    // المخزون
    // ---------------------------------------------------------

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
                    SET part_name = ?, category = ?,
                        quantity = quantity + ?, unit_price = ?,
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

            log(db, "إضافة/تحديث", "قطعة غيار", number, "تحديث قطعة الغيار: $name")
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun updatePart(
        oldNumber: String,
        number: String,
        name: String,
        category: String,
        quantity: Int,
        price: Double
    ): Boolean {
        val values = ContentValues().apply {
            put("part_number", number.trim())
            put("part_name", name.trim())
            put("category", category.trim())
            put("quantity", quantity)
            put("unit_price", price)
            
        }

        val db = writableDatabase
        val changed = db.update(
            "inventory",
            values,
            "part_number = ?",
            arrayOf(oldNumber)
        ) > 0
if (changed) {
    db.execSQL(
        "UPDATE inventory SET last_updated = CURRENT_TIMESTAMP WHERE part_number = ?",
        arrayOf(number.trim())
    )
}
        if (changed) {
            log(db, "تعديل", "قطعة غيار", number, "تعديل قطعة الغيار: $name")
        }
        return changed
    }

    fun deletePart(number: String): Boolean {
        val db = writableDatabase
        val deleted = db.delete(
            "inventory",
            "part_number = ?",
            arrayOf(number)
        ) > 0

        if (deleted) {
            log(db, "حذف", "قطعة غيار", number, "حذف قطعة الغيار $number")
        }
        return deleted
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

// -------------------------------------------------------------
// نماذج نتائج المركبات والزيارات وسجل العمليات
// -------------------------------------------------------------

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

data class AuditRecord(
    val id: Long,
    val action: String,
    val entityType: String,
    val entityId: String,
    val description: String,
    val date: String
)

private inline fun <T> Cursor.mapRows(block: (Cursor) -> T): List<T> {
    val result = mutableListOf<T>()
    while (moveToNext()) result.add(block(this))
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
