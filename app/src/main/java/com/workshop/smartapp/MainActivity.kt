package com.workshop.smartapp

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

private const val WORKSHOP_NAME = "ورشة خبراء هونداي وفورد"
private const val CONTACT = "775008077"
private const val LOCATION = "صنعاء، جولة ٤٥، خلف مطعم الخطيب"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { WorkshopApp(WorkshopDatabase(this)) }
    }
}

private data class Customer(val id: Long, val name: String, val phone: String, val car: String, val plate: String, val notes: String, val createdAt: String)
private data class Diagnostic(val id: Long, val customerName: String, val system: String, val codes: String, val notes: String, val date: String)
private data class Invoice(val id: Long, val customerName: String, val car: String, val details: String, val parts: Double, val labor: Double, val total: Double, val date: String)
private data class Part(val number: String, val name: String, val category: String, val quantity: Int, val price: Double)

private class WorkshopDatabase(context: Context) : SQLiteOpenHelper(context, "workshop_data.db", null, 1) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE customers (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, phone TEXT NOT NULL, car_model TEXT, plate_number TEXT, notes TEXT, created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)")
        db.execSQL("CREATE TABLE invoices (id INTEGER PRIMARY KEY AUTOINCREMENT, customer_id INTEGER NOT NULL, service_details TEXT, parts_cost REAL, labor_cost REAL, total_amount REAL, invoice_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP, FOREIGN KEY(customer_id) REFERENCES customers(id))")
        db.execSQL("CREATE TABLE diagnostics (id INTEGER PRIMARY KEY AUTOINCREMENT, customer_id INTEGER NOT NULL, system_type TEXT, dtc_codes TEXT, technician_notes TEXT, check_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP, FOREIGN KEY(customer_id) REFERENCES customers(id))")
        db.execSQL("CREATE TABLE inventory (id INTEGER PRIMARY KEY AUTOINCREMENT, part_number TEXT UNIQUE, part_name TEXT NOT NULL, category TEXT, quantity INTEGER DEFAULT 0, unit_price REAL DEFAULT 0.0, last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit

    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.setForeignKeyConstraintsEnabled(true)
    }

    fun addCustomer(name: String, phone: String, car: String, plate: String, notes: String) {
        val values = ContentValues().apply {
            put("name", name); put("phone", phone); put("car_model", car); put("plate_number", plate); put("notes", notes)
        }
        writableDatabase.insertOrThrow("customers", null, values)
    }

    fun addDiagnostic(customerId: Long, system: String, codes: String, notes: String) {
        val values = ContentValues().apply {
            put("customer_id", customerId); put("system_type", system); put("dtc_codes", codes); put("technician_notes", notes)
        }
        writableDatabase.insertOrThrow("diagnostics", null, values)
    }

    fun addInvoice(customerId: Long, details: String, parts: Double, labor: Double) {
        val values = ContentValues().apply {
            put("customer_id", customerId); put("service_details", details); put("parts_cost", parts); put("labor_cost", labor); put("total_amount", parts + labor)
        }
        writableDatabase.insertOrThrow("invoices", null, values)
    }

    fun addPart(number: String, name: String, category: String, quantity: Int, price: Double) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            val cursor = db.rawQuery("SELECT id FROM inventory WHERE part_number = ?", arrayOf(number))
            val exists = cursor.use { it.moveToFirst() }
            if (exists) {
                db.execSQL("UPDATE inventory SET part_name = ?, category = ?, quantity = quantity + ?, unit_price = ?, last_updated = CURRENT_TIMESTAMP WHERE part_number = ?", arrayOf(name, category, quantity, price, number))
            } else {
                val values = ContentValues().apply {
                    put("part_number", number); put("part_name", name); put("category", category); put("quantity", quantity); put("unit_price", price)
                }
                db.insertOrThrow("inventory", null, values)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun customers(): List<Customer> = readableDatabase.rawQuery("SELECT id, name, phone, car_model, plate_number, notes, created_at FROM customers ORDER BY id DESC", null).use { cursor ->
        cursor.rows { Customer(it.long("id"), it.text("name"), it.text("phone"), it.text("car_model"), it.text("plate_number"), it.text("notes"), it.text("created_at")) }
    }

    fun diagnostics(): List<Diagnostic> = readableDatabase.rawQuery("SELECT d.id, c.name AS customer_name, d.system_type, d.dtc_codes, d.technician_notes, d.check_date FROM diagnostics d LEFT JOIN customers c ON c.id = d.customer_id ORDER BY d.id DESC", null).use { cursor ->
        cursor.rows { Diagnostic(it.long("id"), it.text("customer_name"), it.text("system_type"), it.text("dtc_codes"), it.text("technician_notes"), it.text("check_date")) }
    }

    fun invoices(): List<Invoice> = readableDatabase.rawQuery("SELECT i.id, c.name AS customer_name, c.car_model, i.service_details, i.parts_cost, i.labor_cost, i.total_amount, i.invoice_date FROM invoices i LEFT JOIN customers c ON c.id = i.customer_id ORDER BY i.id DESC", null).use { cursor ->
        cursor.rows { Invoice(it.long("id"), it.text("customer_name"), it.text("car_model"), it.text("service_details"), it.double("parts_cost"), it.double("labor_cost"), it.double("total_amount"), it.text("invoice_date")) }
    }

    fun inventory(): List<Part> = readableDatabase.rawQuery("SELECT part_number, part_name, category, quantity, unit_price FROM inventory ORDER BY part_name", null).use { cursor ->
        cursor.rows { Part(it.text("part_number"), it.text("part_name"), it.text("category"), it.int("quantity"), it.double("unit_price")) }
    }
}

private inline fun <T> Cursor.rows(block: (Cursor) -> T): List<T> {
    val result = mutableListOf<T>()
    while (moveToNext()) result.add(block(this))
    return result
}
private fun Cursor.text(column: String): String = getString(getColumnIndexOrThrow(column)) ?: ""
private fun Cursor.long(column: String): Long = getLong(getColumnIndexOrThrow(column))
private fun Cursor.int(column: String): Int = getInt(getColumnIndexOrThrow(column))
private fun Cursor.double(column: String): Double = getDouble(getColumnIndexOrThrow(column))

private enum class Page(val title: String) {
    HOME("الرئيسية"), CUSTOMERS("العملاء والمركبات"), DIAGNOSTICS("فحص الأعطال"), INVOICES("الفواتير والتقارير"), INVENTORY("المخزون")
}
private enum class FormType(val title: String, val labels: List<Pair<String, Boolean>>) {
    CUSTOMER("تسجيل عميل ومركبة", listOf("اسم العميل" to false, "رقم الهاتف" to false, "موديل السيارة" to false, "رقم اللوحة" to false, "ملاحظات" to false)),
    DIAGNOSTIC("تسجيل تقرير فحص", listOf("معرف العميل" to true, "النظام المعني" to false, "أكواد الأعطال DTC" to false, "ملاحظات الفني" to false)),
    INVOICE("إصدار فاتورة صيانة", listOf("معرف العميل" to true, "تفاصيل الصيانة" to false, "تكلفة قطع الغيار بالريال" to true, "أجور اليد العاملة بالريال" to true)),
    PART("إضافة أو تحديث قطعة غيار", listOf("رقم القطعة" to false, "اسم القطعة" to false, "التصنيف" to false, "الكمية المضافة" to true, "سعر الوحدة بالريال" to true))
}

@Composable
private fun WorkshopApp(db: WorkshopDatabase) {
    var page by remember { mutableStateOf(Page.HOME) }
    var form by remember { mutableStateOf<FormType?>(null) }
    var refresh by remember { mutableIntStateOf(0) }
    val customers = remember(refresh) { db.customers() }
    val diagnostics = remember(refresh) { db.diagnostics() }
    val invoices = remember(refresh) { db.invoices() }
    val parts = remember(refresh) { db.inventory() }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        MaterialTheme {
            Surface(Modifier.fillMaxSize()) {
                Column(Modifier.fillMaxSize().padding(16.dp)) {
                    Text(WORKSHOP_NAME, fontSize = 23.sp, fontWeight = FontWeight.Bold)
                    Text("$LOCATION | للتواصل: $CONTACT", style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(12.dp))
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                        Page.values().forEach { item -> TextButton(onClick = { page = item }) { Text(item.title) } }
                    }
                    Spacer(Modifier.height(8.dp))
                    Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())) {
                        when (page) {
                            Page.HOME -> HomePage(customers.size, diagnostics.size, invoices.size, parts.size) { page = it }
                            Page.CUSTOMERS -> CustomersPage(customers) { form = FormType.CUSTOMER }
                            Page.DIAGNOSTICS -> DiagnosticsPage(diagnostics) { form = FormType.DIAGNOSTIC }
                            Page.INVOICES -> InvoicesPage(invoices) { form = FormType.INVOICE }
                            Page.INVENTORY -> InventoryPage(parts) { form = FormType.PART }
                        }
                    }
                }
            }
        }
        form?.let { activeForm ->
            FormDialog(activeForm, { form = null }) { values ->
                try {
                    val error = when (activeForm) {
                        FormType.CUSTOMER -> when {
                            values[0].isBlank() || values[1].isBlank() -> "اسم العميل ورقم الهاتف مطلوبان"
                            else -> { db.addCustomer(values[0].trim(), values[1].trim(), values[2].trim(), values[3].trim(), values[4].trim()); null }
                        }
                        FormType.DIAGNOSTIC -> {
                            val id = values[0].toLongOrNull()
                            when {
                                id == null -> "أدخل معرف عميل صحيحاً"
                                customers.none { customer -> customer.id == id } -> "العميل غير موجود؛ سجّل العميل أولاً"
                                values[1].isBlank() -> "اسم النظام مطلوب"
                                else -> { db.addDiagnostic(id, values[1].trim(), values[2].trim(), values[3].trim()); null }
                            }
                        }
                        FormType.INVOICE -> {
                            val id = values[0].toLongOrNull()
                            val partsCost = values[2].toDoubleOrNull()
                            val laborCost = values[3].toDoubleOrNull()
                            when {
                                id == null -> "أدخل معرف عميل صحيحاً"
                                customers.none { customer -> customer.id == id } -> "العميل غير موجود؛ سجّل العميل أولاً"
                                values[1].isBlank() -> "تفاصيل الصيانة مطلوبة"
                                partsCost == null || laborCost == null || partsCost < 0 || laborCost < 0 -> "أدخل تكاليف صحيحة غير سالبة"
                                else -> { db.addInvoice(id, values[1].trim(), partsCost, laborCost); null }
                            }
                        }
                        FormType.PART -> {
                            val quantity = values[3].toIntOrNull()
                            val price = values[4].toDoubleOrNull()
                            when {
                                values[0].isBlank() || values[1].isBlank() -> "رقم القطعة واسمها مطلوبان"
                                quantity == null || quantity < 0 || price == null || price < 0 -> "أدخل كمية وسعراً صحيحين"
                                else -> { db.addPart(values[0].trim(), values[1].trim(), values[2].trim(), quantity, price); null }
                            }
                        }
                    }
                    if (error == null) { refresh++; form = null }
                    error
                } catch (exception: Exception) {
                    "تعذر الحفظ: ${exception.localizedMessage ?: "خطأ غير معروف"}"
                }
            }
        }
    }
}

@Composable
private fun HomePage(customerCount: Int, diagnosticCount: Int, invoiceCount: Int, partCount: Int, navigate: (Page) -> Unit) {
    Text("إدارة الورشة من هاتفك", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(12.dp))
    SummaryCard("العملاء والمركبات", customerCount, "تسجيل بيانات العملاء والسيارات") { navigate(Page.CUSTOMERS) }
    SummaryCard("تقارير الفحص", diagnosticCount, "أكواد الأعطال وملاحظات الفني") { navigate(Page.DIAGNOSTICS) }
    SummaryCard("الفواتير", invoiceCount, "إصدار الفواتير ومتابعة الإيرادات") { navigate(Page.INVOICES) }
    SummaryCard("قطع الغيار", partCount, "إدارة الكميات والأسعار") { navigate(Page.INVENTORY) }
}

@Composable
private fun SummaryCard(title: String, count: Int, subtitle: String, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
        Column(Modifier.fillMaxWidth().padding(14.dp)) {
            Text("$title: $count", fontWeight = FontWeight.Bold)
            Text(subtitle)
            TextButton(onClick = onClick) { Text("فتح") }
        }
    }
}

@Composable
private fun CustomersPage(items: List<Customer>, add: () -> Unit) {
    PageTitle("العملاء والمركبات", add, "تسجيل عميل")
    if (items.isEmpty()) EmptyMessage("لا يوجد عملاء مسجلون بعد.")
    items.forEach { item -> InfoCard("#${item.id} — ${item.name}", "الهاتف: ${item.phone}\nالسيارة: ${item.car}\nاللوحة: ${item.plate}\nملاحظات: ${item.notes}\nالتسجيل: ${item.createdAt}") }
}

@Composable
private fun DiagnosticsPage(items: List<Diagnostic>, add: () -> Unit) {
    PageTitle("تقارير فحص الأعطال", add, "إضافة تقرير")
    if (items.isEmpty()) EmptyMessage("لا توجد تقارير فحص مسجلة بعد.")
    items.forEach { item -> InfoCard("تقرير #${item.id} — ${item.customerName}", "النظام: ${item.system}\nأكواد DTC: ${item.codes}\nملاحظات الفني: ${item.notes}\nالتاريخ: ${item.date}") }
}

@Composable
private fun InvoicesPage(items: List<Invoice>, add: () -> Unit) {
    PageTitle("الفواتير والإيرادات", add, "إصدار فاتورة")
    InfoCard("إجمالي الإيرادات المسجلة", "${money(items.sumOf { it.total })} ريال")
    if (items.isEmpty()) EmptyMessage("ل�� توجد فواتير مسجلة بعد.")
    items.forEach { item -> InfoCard("فاتورة #${item.id} — ${item.customerName} (${item.car})", "الأعمال: ${item.details}\nقطع الغيار: ${money(item.parts)} ريال\nأجور اليد: ${money(item.labor)} ريال\nالإجمالي: ${money(item.total)} ريال\nالتاريخ: ${item.date}") }
}

@Composable
private fun InventoryPage(items: List<Part>, add: () -> Unit) {
    PageTitle("مخزون قطع الغيار", add, "إضافة / تحديث")
    if (items.isEmpty()) EmptyMessage("لا توجد قطع غيار مسجلة بعد.")
    items.forEach { item -> InfoCard("${item.name} — ${item.number}", "التصنيف: ${item.category}\nالكمية: ${item.quantity}\nسعر الوحدة: ${money(item.price)} ريال") }
}

@Composable
private fun PageTitle(title: String, onAdd: () -> Unit, action: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Button(onClick = onAdd) { Text(action) }
    }
    Spacer(Modifier.height(8.dp))
}

@Composable
private fun InfoCard(title: String, details: String) {
    Card(Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
        Column(Modifier.padding(14.dp)) {
            Text(title, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(details)
        }
    }
}

@Composable
private fun EmptyMessage(message: String) {
    Text(message, Modifier.padding(vertical = 14.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun FormDialog(type: FormType, onDismiss: () -> Unit, onSave: (List<String>) -> String?) {
    val values = remember(type) { mutableStateListOf<String>().apply { addAll(type.labels.map { "" }) } }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(type.title) },
        text = {
            Column(Modifier.fillMaxWidth().height(440.dp).verticalScroll(rememberScrollState())) {
                type.labels.forEachIndexed { index, field ->
                    OutlinedTextField(
                        value = values[index],
                        onValueChange = { values[index] = it },
                        label = { Text(field.first) },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = if (field.second) KeyboardType.Decimal else KeyboardType.Text),
                        singleLine = !field.first.contains("ملاحظات") && !field.first.contains("تفاصيل")
                    )
                }
                
                    error?.let {
                    Text(
                        text = it,
                        modifier = Modifier.padding(top = 8.dp),
                        color = MaterialTheme.colorScheme.error
                    )
                }
        },
        confirmButton = { TextButton(onClick = { error = onSave(values.toList()) }) { Text("حفظ") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } }
    )
}

private fun money(value: Double): String = String.format(Locale.getDefault(), "%.2f", value)
