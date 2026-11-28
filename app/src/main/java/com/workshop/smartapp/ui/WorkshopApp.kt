
package com.workshop.smartapp.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.workshop.smartapp.model.WorkshopPage
import java.util.Locale

private const val WORKSHOP_NAME = "ورشة خبراء هونداي وفورد"
private const val CONTACT = "775008077"
private const val LOCATION = "صنعاء، جولة ٤٥، خلف مطعم الخطيب"

private data class EditorState(
    val form: WorkshopForm,
    val recordId: Long? = null,
    val oldPartNumber: String? = null,
    val initialValues: List<String> = emptyList()
)

private data class DeleteRequest(
    val form: WorkshopForm,
    val recordId: Long? = null,
    val partNumber: String? = null,
    val title: String
)

@Composable
fun WorkshopApp(database: WorkshopDatabase) {
    var page by remember { mutableStateOf(WorkshopPage.HOME) }
    var editor by remember { mutableStateOf<EditorState?>(null) }
    var deleteRequest by remember { mutableStateOf<DeleteRequest?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    var refreshKey by remember { mutableIntStateOf(0) }

    val customers = remember(refreshKey) { database.customers() }
    val diagnostics = remember(refreshKey) { database.diagnostics() }
    val invoices = remember(refreshKey) { database.invoices() }
    val parts = remember(refreshKey) { database.inventory() }

    fun refresh() {
        refreshKey++
    }

    fun openAdd(form: WorkshopForm) {
        editor = EditorState(form = form)
    }

    fun openCustomerEdit(item: Customer) {
        editor = EditorState(
            form = WorkshopForm.CUSTOMER,
            recordId = item.id,
            initialValues = listOf(
                item.name,
                item.phone,
                item.car,
                item.plate,
                item.notes
            )
        )
    }

    fun openDiagnosticEdit(item: Diagnostic) {
        editor = EditorState(
            form = WorkshopForm.DIAGNOSTIC,
            recordId = item.id,
            initialValues = listOf(
                item.system,
                item.codes,
                item.notes
            )
        )
    }

    fun openInvoiceEdit(item: Invoice) {
        editor = EditorState(
            form = WorkshopForm.INVOICE,
            recordId = item.id,
            initialValues = listOf(
                item.details,
                item.parts.toString(),
                item.labor.toString()
            )
        )
    }

    fun openPartEdit(item: Part) {
        editor = EditorState(
            form = WorkshopForm.PART,
            oldPartNumber = item.number,
            initialValues = listOf(
                item.number,
                item.name,
                item.category,
                item.quantity.toString(),
                item.price.toString()
            )
        )
    }

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

                    Spacer(Modifier.height(12.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                    ) {
                        WorkshopPage.values().forEach { item ->
                            TextButton(onClick = { page = item }) {
                                Text(
                                    text = item.title,
                                    fontWeight = if (page == item) {
                                        FontWeight.Bold
                                    } else {
                                        FontWeight.Normal
                                    }
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                    ) {
                        when (page) {
                            WorkshopPage.HOME -> HomePage(
                                customerCount = customers.size,
                                diagnosticCount = diagnostics.size,
                                invoiceCount = invoices.size,
                                partCount = parts.size,
                                navigate = { page = it }
                            )

                            WorkshopPage.CUSTOMERS -> CustomersPage(
                                items = customers,
                                add = { openAdd(WorkshopForm.CUSTOMER) },
                                edit = ::openCustomerEdit,
                                delete = {
                                    deleteRequest = DeleteRequest(
                                        form = WorkshopForm.CUSTOMER,
                                        recordId = it.id,
                                        title = "العميل ${it.name}"
                                    )
                                }
                            )

                            WorkshopPage.DIAGNOSTICS -> DiagnosticsPage(
                                items = diagnostics,
                                add = { openAdd(WorkshopForm.DIAGNOSTIC) },
                                edit = ::openDiagnosticEdit,
                                delete = {
                                    deleteRequest = DeleteRequest(
                                        form = WorkshopForm.DIAGNOSTIC,
                                        recordId = it.id,
                                        title = "تقرير الفحص رقم ${it.id}"
                                    )
                                }
                            )

                            WorkshopPage.INVOICES -> InvoicesPage(
                                items = invoices,
                                add = { openAdd(WorkshopForm.INVOICE) },
                                edit = ::openInvoiceEdit,
                                delete = {
                                    deleteRequest = DeleteRequest(
                                        form = WorkshopForm.INVOICE,
                                        recordId = it.id,
                                        title = "الفاتورة رقم ${it.id}"
                                    )
                                }
                            )

                            WorkshopPage.INVENTORY -> InventoryPage(
                                items = parts,
                                add = { openAdd(WorkshopForm.PART) },
                                edit = ::openPartEdit,
                                delete = {
                                    deleteRequest = DeleteRequest(
                                        form = WorkshopForm.PART,
                                        partNumber = it.number,
                                        title = "قطعة الغيار ${it.name}"
                                    )
                                }
                            )
                        }
                    }
                }
            }

            editor?.let { current ->
                WorkshopFormDialog(
                    type = current.form,
                    customers = customers,
                    initialValues = current.initialValues,
                    showCustomerPicker = current.recordId == null &&
                        current.form.requiresCustomer,
                    dialogTitle = if (current.recordId != null ||
                        current.oldPartNumber != null) {
                        "تعديل ${current.form.title}"
                    } else {
                        current.form.title
                    },
                    saveLabel = if (current.recordId != null ||
                        current.oldPartNumber != null) "حفظ التعديلات" else "حفظ",
                    onDismiss = { editor = null },
                    onSave = { submission ->
                        val error = saveForm(
                            form = current.form,
                            submission = submission,
                            customers = customers,
                            database = database,
                            recordId = current.recordId,
                            oldPartNumber = current.oldPartNumber
                        )

                        if (error == null) {
                            refresh()
                            editor = null
                            message = "تم حفظ البيانات بنجاح"
                        }
                        error
                    }
                )
            }

            deleteRequest?.let { request ->
                AlertDialog(
                    onDismissRequest = { deleteRequest = null },
                    title = { Text("تأكيد الحذف") },
                    text = {
                        Text("هل أنت متأكد من حذف ${request.title}؟ لا يمكن التراجع عن هذه العملية.")
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                val result = try {
                                    when (request.form) {
                                        WorkshopForm.CUSTOMER ->
                                            database.deleteCustomer(
                                                request.recordId ?: -1L
                                            )

                                        WorkshopForm.DIAGNOSTIC ->
                                            database.deleteDiagnostic(
                                                request.recordId ?: -1L
                                            )

                                        WorkshopForm.INVOICE ->
                                            database.deleteInvoice(
                                                request.recordId ?: -1L
                                            )

                                        WorkshopForm.PART ->
                                            database.deletePart(
                                                request.partNumber.orEmpty()
                                            )
                                    }
                                } catch (e: Exception) {
                                    false
                                }

                                deleteRequest = null

                                if (result) {
                                    refresh()
                                    message = "تم الحذف بنجاح"
                                } else {
                                    message = when (request.form) {
                                        WorkshopForm.CUSTOMER ->
                                            "تعذر حذف العميل: توجد مركبات أو زيارات أو تقارير أو فواتير مرتبطة به. احذف السجلات التابعة أولاً."

                                        else ->
                                            "تعذر إتمام الحذف. قد يكون السجل غير موجود أو مرتبطاً ببيانات أخرى."
                                    }
                                }
                            }
                        ) {
                            Text("حذف")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { deleteRequest = null }) {
                            Text("إلغاء")
                        }
                    }
                )
            }

            message?.let { text ->
                AlertDialog(
                    onDismissRequest = { message = null },
                    title = { Text("إشعار") },
                    text = { Text(text) },
                    confirmButton = {
                        TextButton(onClick = { message = null }) {
                            Text("حسناً")
                        }
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
    database: WorkshopDatabase,
    recordId: Long? = null,
    oldPartNumber: String? = null
): String? {
    val values = submission.fields

    return try {
        when (form) {
            WorkshopForm.CUSTOMER -> {
                if (values.size < 5) {
                    "بيانات النموذج غير مكتملة"
                } else if (values[0].isBlank() || values[1].isBlank()) {
                    "اسم العميل ورقم الهاتف مطلوبان"
                } else {
                    if (recordId == null) {
                        database.addCustomer(
                            values[0].trim(),
                            values[1].trim(),
                            values[2].trim(),
                            values[3].trim(),
                            values[4].trim()
                        )
                    } else {
                        val updated = database.updateCustomer(
                            recordId,
                            values[0].trim(),
                            values[1].trim(),
                            values[2].trim(),
                            values[3].trim(),
                            values[4].trim()
                        )
                        if (!updated) return "لم يتم العثور على العميل المطلوب تعديله"
                    }
                    null
                }
            }

            WorkshopForm.DIAGNOSTIC -> {
                if (values.size < 3) {
                    return "بيانات التقرير غير مكتملة"
                }

                if (values[0].isBlank()) {
                    return "اسم النظام مطلوب"
                }

                if (recordId == null) {
                    val customerId = submission.customerId
                        ?: return "اختر عميلاً أولاً"

                    if (customers.none { it.id == customerId }) {
                        return "العميل المختار غير موجود"
                    }

                    database.addDiagnostic(
                        customerId,
                        values[0].trim(),
                        values[1].trim(),
                        values[2].trim()
                    )
                } else {
                    if (!database.updateDiagnostic(
                            recordId,
                            values[0].trim(),
                            values[1].trim(),
                            values[2].trim()
                        )
                    ) {
                        return "لم يتم العثور على التقرير المطلوب تعديله"
                    }
                }
                null
            }

            WorkshopForm.INVOICE -> {
                if (values.size < 3) {
                    return "بيانات الفاتورة غير مكتملة"
                }

                val partsCost = values[1].toDoubleOrNull()
                val laborCost = values[2].toDoubleOrNull()

                if (values[0].isBlank()) return "تفاصيل الصيانة مطلوبة"
                if (partsCost == null || laborCost == null ||
                    partsCost < 0 || laborCost < 0
                ) {
                    return "أدخل تكاليف صحيحة غير سالبة"
                }

                if (recordId == null) {
                    val customerId = submission.customerId
                        ?: return "اختر عميلاً أولاً"

                    if (customers.none { it.id == customerId }) {
                        return "العميل المختار غير موجود"
                    }

                    database.addInvoice(
                        customerId,
                        values[0].trim(),
                        partsCost,
                        laborCost
                    )
                } else {
                    if (!database.updateInvoice(
                            recordId,
                            values[0].trim(),
                            partsCost,
                            laborCost
                        )
                    ) {
                        return "لم يتم العثور على الفاتورة المطلوبة"
                    }
                }
                null
            }

            WorkshopForm.PART -> {
                if (values.size < 5) {
                    return "بيانات القطعة غير مكتملة"
                }

                val quantity = values[3].toIntOrNull()
                val price = values[4].toDoubleOrNull()

                if (values[0].isBlank() || values[1].isBlank()) {
                    return "رقم القطعة واسمها مطلوبان"
                }

                if (quantity == null || quantity < 0 ||
                    price == null || price < 0
                ) {
                    return "أدخل كمية وسعراً صحيحين غير سالبين"
                }

                if (oldPartNumber == null) {
                    database.addPart(
                        values[0].trim(),
                        values[1].trim(),
                        values[2].trim(),
                        quantity,
                        price
                    )
                } else {
                    if (!database.updatePart(
                            oldPartNumber,
                            values[0].trim(),
                            values[1].trim(),
                            values[2].trim(),
                            quantity,
                            price
                        )
                    ) {
                        return "لم يتم العثور على قطعة الغيار المطلوبة"
                    }
                }
                null
            }
        }
    } catch (e: Exception) {
        "تعذر حفظ البيانات: ${e.localizedMessage ?: "خطأ غير معروف"}"
    }
}

@Composable
private fun HomePage(
    customerCount: Int,
    diagnosticCount: Int,
    invoiceCount: Int,
    partCount: Int,
    navigate: (WorkshopPage) -> Unit
) {
    Text(
        "إدارة الورشة من هاتفك",
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold
    )

    Spacer(Modifier.height(12.dp))

    SummaryCard("العملاء والمركبات", customerCount) {
        navigate(WorkshopPage.CUSTOMERS)
    }
    SummaryCard("تقارير الفحص", diagnosticCount) {
        navigate(WorkshopPage.DIAGNOSTICS)
    }
    SummaryCard("الفواتير", invoiceCount) {
        navigate(WorkshopPage.INVOICES)
    }
    SummaryCard("قطع الغيار", partCount) {
        navigate(WorkshopPage.INVENTORY)
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
            .padding(vertical = 5.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("$title: $count", fontWeight = FontWeight.Bold)
            Text("فتح")
        }
    }
}

@Composable
private fun CustomersPage(
    items: List<Customer>,
    add: () -> Unit,
    edit: (Customer) -> Unit,
    delete: (Customer) -> Unit
) {
    PageTitle("العملاء والمركبات", "تسجيل عميل", add)

    if (items.isEmpty()) EmptyMessage("لا يوجد عملاء مسجلون بعد.")

    items.forEach { customer ->
        InfoCard(
            title = "#${customer.id} — ${customer.name}",
            details = """
                الهاتف: ${customer.phone}
                السيارة: ${customer.car}
                اللوحة: ${customer.plate}
                ملاحظات: ${customer.notes}
                التسجيل: ${customer.createdAt}
            """.trimIndent(),
            onEdit = { edit(customer) },
            onDelete = { delete(customer) }
        )
    }
}

@Composable
private fun DiagnosticsPage(
    items: List<Diagnostic>,
    add: () -> Unit,
    edit: (Diagnostic) -> Unit,
    delete: (Diagnostic) -> Unit
) {
    PageTitle("تقارير فحص الأعطال", "إضافة تقرير", add)

    if (items.isEmpty()) EmptyMessage("لا توجد تقارير فحص مسجلة بعد.")

    items.forEach { item ->
        InfoCard(
            title = "تقرير #${item.id} — ${item.customerName}",
            details = """
                النظام: ${item.system}
                أكواد DTC: ${item.codes}
                ملاحظات الفني: ${item.notes}
                التاريخ: ${item.date}
            """.trimIndent(),
            onEdit = { edit(item) },
            onDelete = { delete(item) }
        )
    }
}

@Composable
private fun InvoicesPage(
    items: List<Invoice>,
    add: () -> Unit,
    edit: (Invoice) -> Unit,
    delete: (Invoice) -> Unit
) {
    PageTitle("الفواتير والإيرادات", "إصدار فاتورة", add)

    InfoCard(
        title = "إجمالي الإيرادات المسجلة",
        details = "${money(items.sumOf { it.total })} ريال"
    )

    if (items.isEmpty()) EmptyMessage("لا توجد فواتير مسجلة بعد.")

    items.forEach { item ->
        InfoCard(
            title = "فاتورة #${item.id} — ${item.customerName} (${item.car})",
            details = """
                الأعمال: ${item.details}
                قطع الغيار: ${money(item.parts)} ريال
                أجور اليد: ${money(item.labor)} ريال
                الإجمالي: ${money(item.total)} ريال
                التاريخ: ${item.date}
            """.trimIndent(),
            onEdit = { edit(item) },
            onDelete = { delete(item) }
        )
    }
}

@Composable
private fun InventoryPage(
    items: List<Part>,
    add: () -> Unit,
    edit: (Part) -> Unit,
    delete: (Part) -> Unit
) {
    PageTitle("مخزون قطع الغيار", "إضافة قطعة", add)

    if (items.isEmpty()) EmptyMessage("لا توجد قطع غيار مسجلة بعد.")

    items.forEach { item ->
        InfoCard(
            title = "${item.name} — ${item.number}",
            details = """
                التصنيف: ${item.category}
                الكمية: ${item.quantity}
                سعر الوحدة: ${money(item.price)} ريال
            """.trimIndent(),
            onEdit = { edit(item) },
            onDelete = { delete(item) }
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
            title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Button(onClick = onAction) {
            Text(action)
        }
    }
    Spacer(Modifier.height(8.dp))
}

@Composable
private fun InfoCard(
    title: String,
    details: String,
    onEdit: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(title, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(details)

            if (onEdit != null || onDelete != null) {
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    if (onEdit != null) {
                        TextButton(onClick = onEdit) {
                            Text("تعديل")
                        }
                    }
                    if (onDelete != null) {
                        TextButton(
                            onClick = onDelete,
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text("حذف")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyMessage(message: String) {
    Text(
        message,
        modifier = Modifier.padding(vertical = 14.dp),
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

private fun money(value: Double): String =
    String.format(Locale.getDefault(), "%.2f", value)
