package com.workshop.smartapp.ui

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.workshop.smartapp.data.WorkshopDatabase
import com.workshop.smartapp.model.Customer
import com.workshop.smartapp.model.Diagnostic
import com.workshop.smartapp.model.Invoice
import com.workshop.smartapp.model.Part
import com.workshop.smartapp.model.WorkshopForm
import com.workshop.smartapp.model.WorkshopFormSubmission
import java.util.Locale

private const val WORKSHOP_NAME = "ورشة خبراء هونداي وفورد"
private const val CONTACT = "775008077"
private const val LOCATION = "صنعاء، جولة ٤٥، خلف مطعم الخطيب"

@Composable
fun WorkshopApp(database: WorkshopDatabase) {
    var page by remember { mutableStateOf(com.workshop.smartapp.model.WorkshopPage.HOME) }
    var activeForm by remember { mutableStateOf<WorkshopForm?>(null) }
    var refreshKey by remember { mutableIntStateOf(0) }

    val customers = remember(refreshKey) { database.customers() }
    val diagnostics = remember(refreshKey) { database.diagnostics() }
    val invoices = remember(refreshKey) { database.invoices() }
    val parts = remember(refreshKey) { database.inventory() }

    CompositionLocalProvider(
        LocalLayoutDirection provides LayoutDirection.Rtl
    ) {
        MaterialTheme {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    Text(
                        text = WORKSHOP_NAME,
                        fontSize = 23.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "$LOCATION | للتواصل: $CONTACT",
                        style = MaterialTheme.typography.bodySmall
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                    ) {
                        com.workshop.smartapp.model.WorkshopPage.values().forEach { item ->
                            TextButton(onClick = { page = item }) {
                                Text(text = item.title)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                    ) {
                        when (page) {
                            com.workshop.smartapp.model.WorkshopPage.HOME -> HomePage(
                                customerCount = customers.size,
                                diagnosticCount = diagnostics.size,
                                invoiceCount = invoices.size,
                                partCount = parts.size,
                                navigate = { page = it }
                            )

                            com.workshop.smartapp.model.WorkshopPage.CUSTOMERS ->
                                CustomersPage(customers) {
                                    activeForm = WorkshopForm.CUSTOMER
                                }

                            com.workshop.smartapp.model.WorkshopPage.DIAGNOSTICS ->
                                DiagnosticsPage(diagnostics) {
                                    activeForm = WorkshopForm.DIAGNOSTIC
                                }

                            com.workshop.smartapp.model.WorkshopPage.INVOICES ->
                                InvoicesPage(invoices) {
                                    activeForm = WorkshopForm.INVOICE
                                }

                            com.workshop.smartapp.model.WorkshopPage.INVENTORY ->
                                InventoryPage(parts) {
                                    activeForm = WorkshopForm.PART
                                }
                        }
                    }
                }
            }

            activeForm?.let { form ->
                WorkshopFormDialog(
                    type = form,
                    customers = customers,
                    onDismiss = { activeForm = null },
                    onSave = { submission ->
                        val error = saveForm(
                            form = form,
                            submission = submission,
                            customers = customers,
                            database = database
                        )

                        if (error == null) {
                            refreshKey++
                            activeForm = null
                        }

                        error
                    }
                )
            }
        }
    }
}

private fun saveForm(
    form: WorkshopForm,
    submission: WorkshopFormSubmission,
    customers: List<Customer>,
    database: WorkshopDatabase
): String? {
    val values = submission.fields

    return try {
        when (form) {
            WorkshopForm.CUSTOMER -> {
                if (values[0].isBlank() || values[1].isBlank()) {
                    "اسم العميل ورقم الهاتف مطلوبان"
                } else {
                    database.addCustomer(
                        name = values[0].trim(),
                        phone = values[1].trim(),
                        car = values[2].trim(),
                        plate = values[3].trim(),
                        notes = values[4].trim()
                    )
                    null
                }
            }

            WorkshopForm.DIAGNOSTIC -> {
                val customerId = submission.customerId
                when {
                    customerId == null ->
                        "اختر عميلاً أولاً"

                    customers.none { it.id == customerId } ->
                        "العميل المختار غير موجود"

                    values[0].isBlank() ->
                        "اسم النظام مطلوب"

                    else -> {
                        database.addDiagnostic(
                            customerId = customerId,
                            system = values[0].trim(),
                            codes = values[1].trim(),
                            notes = values[2].trim()
                        )
                        null
                    }
                }
            }

            WorkshopForm.INVOICE -> {
                val customerId = submission.customerId
                val partsCost = values[1].toDoubleOrNull()
                val laborCost = values[2].toDoubleOrNull()

                when {
                    customerId == null ->
                        "اختر عميلاً أولاً"

                    customers.none { it.id == customerId } ->
                        "العميل المختار غير موجود"

                    values[0].isBlank() ->
                        "تفاصيل الصيانة مطلوبة"

                    partsCost == null || laborCost == null ||
                        partsCost < 0 || laborCost < 0 ->
                        "أدخل تكاليف صحيحة غير سالبة"

                    else -> {
                        database.addInvoice(
                            customerId = customerId,
                            details = values[0].trim(),
                            parts = partsCost,
                            labor = laborCost
                        )
                        null
                    }
                }
            }

            WorkshopForm.PART -> {
                val quantity = values[3].toIntOrNull()
                val price = values[4].toDoubleOrNull()

                when {
                    values[0].isBlank() || values[1].isBlank() ->
                        "رقم القطعة واسمها مطلوبان"

                    quantity == null || quantity < 0 ||
                        price == null || price < 0 ->
                        "أدخل كمية وسعراً صحيحين"

                    else -> {
                        database.addPart(
                            number = values[0].trim(),
                            name = values[1].trim(),
                            category = values[2].trim(),
                            quantity = quantity,
                            price = price
                        )
                        null
                    }
                }
            }
        }
    } catch (exception: Exception) {
        "تعذر الحفظ: ${exception.localizedMessage ?: "خطأ غير معروف"}"
    }
}

@Composable
private fun HomePage(
    customerCount: Int,
    diagnosticCount: Int,
    invoiceCount: Int,
    partCount: Int,
    navigate: (com.workshop.smartapp.model.WorkshopPage) -> Unit
) {
    Text(
        text = "إدارة الورشة من هاتفك",
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold
    )
    Spacer(modifier = Modifier.height(12.dp))

    SummaryCard("العملاء والمركبات", customerCount) {
        navigate(com.workshop.smartapp.model.WorkshopPage.CUSTOMERS)
    }
    SummaryCard("تقارير الفحص", diagnosticCount) {
        navigate(com.workshop.smartapp.model.WorkshopPage.DIAGNOSTICS)
    }
    SummaryCard("الفواتير", invoiceCount) {
        navigate(com.workshop.smartapp.model.WorkshopPage.INVOICES)
    }
    SummaryCard("قطع الغيار", partCount) {
        navigate(com.workshop.smartapp.model.WorkshopPage.INVENTORY)
    }
}

@Composable
private fun SummaryCard(
    title: String,
    count: Int,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = "$title: $count", fontWeight = FontWeight.Bold)
            TextButton(onClick = onClick) {
                Text(text = "فتح")
            }
        }
    }
}

@Composable
private fun CustomersPage(
    items: List<Customer>,
    add: () -> Unit
) {
    PageTitle("العملاء والمركبات", "تسجيل عميل", add)

    if (items.isEmpty()) {
        EmptyMessage("لا يوجد عملاء مسجلون بعد.")
    }

    items.forEach { customer ->
        InfoCard(
            "#${customer.id} — ${customer.name}",
            "الهاتف: ${customer.phone}\n" +
                "السيارة: ${customer.car}\n" +
                "اللوحة: ${customer.plate}\n" +
                "ملاحظات: ${customer.notes}\n" +
                "التسجيل: ${customer.createdAt}"
        )
    }
}

@Composable
private fun DiagnosticsPage(
    items: List<Diagnostic>,
    add: () -> Unit
) {
    PageTitle("تقارير فحص الأعطال", "إضافة تقرير", add)

    if (items.isEmpty()) {
        EmptyMessage("لا توجد تقارير فحص مسجلة بعد.")
    }

    items.forEach { diagnostic ->
        InfoCard(
            "تقرير #${diagnostic.id} — ${diagnostic.customerName}",
            "النظام: ${diagnostic.system}\n" +
                "أكواد DTC: ${diagnostic.codes}\n" +
                "ملاحظات الفني: ${diagnostic.notes}\n" +
                "التاريخ: ${diagnostic.date}"
        )
    }
}

@Composable
private fun InvoicesPage(
    items: List<Invoice>,
    add: () -> Unit
) {
    PageTitle("الفواتير والإيرادات", "إصدار فاتورة", add)

    InfoCard(
        "إجمالي الإيرادات المسجلة",
        "${money(items.sumOf { it.total })} ريال"
    )

    if (items.isEmpty()) {
        EmptyMessage("لا توجد فواتير مسجلة بعد.")
    }

    items.forEach { invoice ->
        InfoCard(
            "فاتورة #${invoice.id} — ${invoice.customerName} (${invoice.car})",
            "الأعمال: ${invoice.details}\n" +
                "قطع الغيار: ${money(invoice.parts)} ريال\n" +
                "أجور اليد: ${money(invoice.labor)} ريال\n" +
                "الإجمالي: ${money(invoice.total)} ريال\n" +
                "التاريخ: ${invoice.date}"
        )
    }
}

@Composable
private fun InventoryPage(
    items: List<Part>,
    add: () -> Unit
) {
    PageTitle("مخزون قطع الغيار", "إضافة / تحديث", add)

    if (items.isEmpty()) {
        EmptyMessage("لا توجد قطع غيار مسجلة بعد.")
    }

    items.forEach { part ->
        InfoCard(
            "${part.name} — ${part.number}",
            "التصنيف: ${part.category}\n" +
                "الكمية: ${part.quantity}\n" +
                "سعر الوحدة: ${money(part.price)} ريال"
        )
    }
}

@Composable
private fun PageTitle(
    title: String,
    action: String,
    onAction: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Button(onClick = onAction) {
            Text(text = action)
        }
    }
    Spacer(modifier = Modifier.height(8.dp))
}

@Composable
private fun InfoCard(title: String, details: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(text = title, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = details)
        }
    }
}

@Composable
private fun EmptyMessage(message: String) {
    Text(
        text = message,
        modifier = Modifier.padding(vertical = 14.dp),
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

private fun money(value: Double): String =
    String.format(Locale.getDefault(), "%.2f", value)
