package com.workshop.smartapp.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
    onSave: (WorkshopFormSubmission) -> String?
) {
    val values = remember(type) {
        mutableStateListOf<String>().apply {
            addAll(type.fields.map { "" })
        }
    }

    var selectedCustomerId by remember(type, customers) {
        mutableStateOf(customers.firstOrNull()?.id)
    }
    var customerMenuExpanded by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val selectedCustomer = customers.firstOrNull { it.id == selectedCustomerId }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = type.title) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(440.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                if (type.requiresCustomer) {
                    if (customers.isEmpty()) {
                        Text(
                            text = "سجّل عميلاً أولاً قبل إضافة هذا السجل.",
                            color = MaterialTheme.colorScheme.error
                        )
                    } else {
                        OutlinedTextField(
                            value = selectedCustomer?.let {
                                "#${it.id} — ${it.name} — ${it.phone}"
                            } ?: "",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(text = "اختر العميل") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { customerMenuExpanded = true }
                                .padding(vertical = 4.dp)
                        )

                        DropdownMenu(
                            expanded = customerMenuExpanded,
                            onDismissRequest = { customerMenuExpanded = false },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            customers.forEach { customer ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            "#${customer.id} — ${customer.name} — ${customer.phone}"
                                        )
                                    },
                                    onClick = {
                                        selectedCustomerId = customer.id
                                        customerMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                type.fields.forEachIndexed { index, field ->
                    OutlinedTextField(
                        value = values[index],
                        onValueChange = { values[index] = it },
                        label = { Text(text = field.first) },
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
                            !field.first.contains("تفاصيل")
                    )
                }

                error?.let { message ->
                    Text(
                        text = message,
                        modifier = Modifier.padding(top = 8.dp),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !type.requiresCustomer || customers.isNotEmpty(),
                onClick = {
                    error = onSave(
                        WorkshopFormSubmission(
                            fields = values.toList(),
                            customerId = selectedCustomerId
                        )
                    )
                }
            ) {
                Text(text = "حفظ")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "إلغاء")
            }
        }
    )
}
