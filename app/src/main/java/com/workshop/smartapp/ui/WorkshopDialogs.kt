
package com.workshop.smartapp.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.workshop.smartapp.model.Customer
import com.workshop.smartapp.model.WorkshopForm
import com.workshop.smartapp.model.WorkshopFormSubmission

@Composable
fun WorkshopFormDialog(
    type: WorkshopForm,
    customers: List<Customer>,
    onDismiss: () -> Unit,
    onSave: (WorkshopFormSubmission) -> String?,
    initialValues: List<String> = emptyList(),
    initialCustomerId: Long? = null,
    showCustomerPicker: Boolean = type.requiresCustomer,
    dialogTitle: String = type.title,
    saveLabel: String = "حفظ"
) {
    val values = remember(type, initialValues) {
        mutableStateListOf<String>().apply {
            type.fields.forEachIndexed { index, _ ->
                add(initialValues.getOrNull(index).orEmpty())
            }
        }
    }

    var selectedCustomerId by remember(type, initialCustomerId) {
        mutableStateOf(initialCustomerId)
    }

    var customerPickerOpen by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val selectedCustomer = customers.firstOrNull {
        it.id == selectedCustomerId
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(dialogTitle) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 520.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                if (showCustomerPicker) {
                    Text(
                        text = "العميل المرتبط بالسجل",
                        style = MaterialTheme.typography.labelLarge
                    )

                    Spacer(Modifier.height(6.dp))

                    OutlinedButton(
                        onClick = { customerPickerOpen = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            selectedCustomer?.let {
                                "${it.name} — ${it.phone}"
                            } ?: "اضغط هنا للبحث واختيار العميل"
                        )
                    }

                    if (selectedCustomer == null) {
                        Text(
                            text = "لم يتم اختيار عميل بعد",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Spacer(Modifier.height(12.dp))
                }

                type.fields.forEachIndexed { index, field ->
                    OutlinedTextField(
                        value = values[index],
                        onValueChange = {
                            values[index] = it
                            error = null
                        },
                        label = { Text(field.first) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = if (field.second) {
                                KeyboardType.Decimal
                            } else {
                                KeyboardType.Text
                            }
                        ),
                        singleLine = !field.first.contains("ملاحظات") &&
                            !field.first.contains("تفاصيل") &&
                            !field.first.contains("أكواد")
                    )
                }

                error?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !showCustomerPicker || selectedCustomer != null,
                onClick = {
                    error = onSave(
                        WorkshopFormSubmission(
                            fields = values.toList(),
                            customerId = selectedCustomerId
                        )
                    )
                }
            ) {
                Text(saveLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )

    if (customerPickerOpen) {
        CustomerPickerDialog(
            customers = customers,
            onDismiss = { customerPickerOpen = false },
            onSelect = {
                selectedCustomerId = it.id
                customerPickerOpen = false
                error = null
            }
        )
    }
}

@Composable
private fun CustomerPickerDialog(
    customers: List<Customer>,
    onDismiss: () -> Unit,
    onSelect: (Customer) -> Unit
) {
    var query by remember { mutableStateOf("") }

    val filteredCustomers = remember(customers, query) {
        customers.filter {
            it.name.contains(query, ignoreCase = true) ||
                it.phone.contains(query, ignoreCase = true) ||
                it.id.toString() == query.trim()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("اختيار العميل") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 500.dp)
            ) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("ابحث بالاسم أو رقم الهاتف") },
                    singleLine = true
                )

                Spacer(Modifier.height(10.dp))

                if (filteredCustomers.isEmpty()) {
                    Text(
                        text = "لا توجد نتائج مطابقة",
                        modifier = Modifier.padding(12.dp)
                    )
                } else {
                    LazyColumn {
                        items(
                            items = filteredCustomers,
                            key = { it.id }
                        ) { customer ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable { onSelect(customer) }
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp)
                                ) {
                                    Text(
                                        customer.name,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text("الهاتف: ${customer.phone}")

                                    if (customer.car.isNotBlank()) {
                                        Text("السيارة: ${customer.car}")
                                    }

                                    if (customer.plate.isNotBlank()) {
                                        Text("اللوحة: ${customer.plate}")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("إغلاق")
            }
        }
    )
}
