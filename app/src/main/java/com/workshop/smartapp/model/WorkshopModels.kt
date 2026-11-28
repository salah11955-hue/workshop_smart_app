
package com.workshop.smartapp.model

data class Customer(
    val id: Long,
    val name: String,
    val phone: String,
    val car: String,
    val plate: String,
    val notes: String,
    val createdAt: String
)

data class Diagnostic(
    val id: Long,
    val customerName: String,
    val system: String,
    val codes: String,
    val notes: String,
    val date: String
)

data class Invoice(
    val id: Long,
    val customerName: String,
    val car: String,
    val details: String,
    val parts: Double,
    val labor: Double,
    val total: Double,
    val date: String
)

data class Part(
    val number: String,
    val name: String,
    val category: String,
    val quantity: Int,
    val price: Double
)

data class Vehicle(
    val id: Long,
    val customerId: Long,
    val makeModel: String,
    val plate: String,
    val vin: String = "",
    val year: String = "",
    val color: String = "",
    val createdAt: String = ""
)

data class WorkshopVisit(
    val id: Long,
    val customerId: Long,
    val vehicleId: Long,
    val odometer: Long,
    val complaint: String,
    val notes: String,
    val status: String,
    val date: String
)

data class WorkshopAuditEntry(
    val id: Long,
    val action: String,
    val entityType: String,
    val entityId: String,
    val description: String,
    val date: String
)

enum class WorkshopPage(val title: String) {
    HOME("الرئيسية"),
    CUSTOMERS("العملاء والمركبات"),
    DIAGNOSTICS("فحص الأعطال"),
    INVOICES("الفواتير والتقارير"),
    INVENTORY("المخزون")
}

enum class WorkshopForm(
    val title: String,
    val fields: List<Pair<String, Boolean>>,
    val requiresCustomer: Boolean = false
) {
    CUSTOMER(
        "تسجيل عميل ومركبة",
        listOf(
            "اسم العميل" to false,
            "رقم الهاتف" to false,
            "موديل السيارة" to false,
            "رقم اللوحة" to false,
            "ملاحظات" to false
        )
    ),

    DIAGNOSTIC(
        "تسجيل تقرير فحص",
        listOf(
            "النظام المعني" to false,
            "أكواد الأعطال DTC" to false,
            "ملاحظات الفني" to false
        ),
        requiresCustomer = true
    ),

    INVOICE(
        "إصدار فاتورة صيانة",
        listOf(
            "تفاصيل الصيانة" to false,
            "تكلفة قطع الغيار بالريال" to true,
            "أجور اليد العاملة بالريال" to true
        ),
        requiresCustomer = true
    ),

    PART(
        "إضافة أو تحديث قطعة غيار",
        listOf(
            "رقم القطعة" to false,
            "اسم القطعة" to false,
            "التصنيف" to false,
            "الكمية المضافة" to true,
            "سعر الوحدة بالريال" to true
        )
    )
}

data class WorkshopFormSubmission(
    val fields: List<String>,
    val customerId: Long? = null
)
