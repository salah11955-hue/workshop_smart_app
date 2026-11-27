import sqlite3
from config import get_database_path, load_config

config = load_config()


def show_all_customers():
    """عرض قائمة بجميع العملاء والسيارات المسجلة في الورشة"""
    with sqlite3.connect(get_database_path()) as conn:
        customers = conn.execute(
            "SELECT id, name, phone, car_model, plate_number, notes, created_at FROM customers"
        ).fetchall()

    print(f"\n=== سجل عملاء {config['workshop_name']} ===")
    if not customers:
        print("لا يوجد عملاء مسجلون حتى الآن.")
        return
    for customer in customers:
        print(f"المعرف: {customer[0]} | الاسم: {customer[1]} | الهاتف: {customer[2]}")
        print(f"السيارة: {customer[3]} | اللوحة: {customer[4]}")
        print(f"ملاحظات: {customer[5]} | تاريخ التسجيل: {customer[6]}")
        print("-" * 40)


def show_all_invoices():
    """عرض كافة فواتير الصيانة وإجمالي الإيرادات"""
    with sqlite3.connect(get_database_path()) as conn:
        invoices = conn.execute(
            """
            SELECT invoices.id, customers.name, customers.car_model, invoices.service_details,
                   invoices.parts_cost, invoices.labor_cost, invoices.total_amount, invoices.invoice_date
            FROM invoices JOIN customers ON invoices.customer_id = customers.id
            ORDER BY invoices.id DESC
            """
        ).fetchall()

    print(f"\n=== سجل فواتير صيانة {config['workshop_name']} ===")
    if not invoices:
        print("لا توجد فواتير صادرة حتى الآن.")
        return
    total_revenue = 0
    for invoice in invoices:
        total_revenue += invoice[6] or 0
        print(f"الفاتورة #{invoice[0]} | العميل: {invoice[1]} ({invoice[2]})")
        print(f"تفاصيل العمل: {invoice[3]}")
        print(f"قطع الغيار: {invoice[4]} ريال | أجور اليد: {invoice[5]} ريال")
        print(f"الإجمالي: {invoice[6]} ريال | التاريخ: {invoice[7]}")
        print("-" * 40)
    print(f"إجمالي الإيرادات المسجلة: {total_revenue} ريال")


if __name__ == "__main__":
    show_all_customers()
    show_all_invoices()
