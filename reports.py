import json
import sqlite3

with open('config.json', 'r', encoding='utf-8') as f:
    config = json.load(f)

def show_all_customers():
    """عرض قائمة بجميع العملاء والسيارات المسجلة في الورشة"""
    conn = sqlite3.connect(config['database']['name'])
    cursor = conn.cursor()
    cursor.execute('SELECT id, name, phone, car_model, plate_number, notes, created_at FROM customers')
    customers = cursor.fetchall()
    conn.close()
    
    print(f"\n=== سجل عملاء {config['workshop_name']} ===")
    if not customers:
        print("⚠️ لا يوجد عملاء مسجلين حتى الآن.")
        return
        
    for cust in customers:
        print(f"🆔 المعرف: {cust[0]} | 👤 الاسم: {cust[1]} | ☎️ الهاتف: {cust[2]}")
        print(f"🚗 السيارة: {cust[3]} | 🔢 اللوحة: {cust[4]}")
        print(f"📝 ملاحظات: {cust[5]} | 📅 تاريخ التسجيل: {cust[6]}")
        print("-" * 40)

def show_all_invoices():
    """عرض كافة فواتير الصيانة وإجمالي الإيرادات"""
    conn = sqlite3.connect(config['database']['name'])
    cursor = conn.cursor()
    cursor.execute('''
        SELECT invoices.id, customers.name, customers.car_model, invoices.service_details, 
               invoices.parts_cost, invoices.labor_cost, invoices.total_amount, invoices.invoice_date 
        FROM invoices 
        JOIN customers ON invoices.customer_id = customers.id
    ''')
    invoices = cursor.fetchall()
    conn.close()
    
    print(f"\n=== سجل فواتير صيانة {config['workshop_name']} ===")
    if not invoices:
        print("⚠️ لا توجد فواتير مُصدرة حتى الآن.")
        return
        
    total_revenue = 0
    for inv in invoices:
        total_revenue += inv[6]
        print(f"🧾 الفاتورة رقم: #{inv[0]} | 👤 العميل: {inv[1]} ({inv[2]})")
        print(f"🔧 تفاصيل العمل: {inv[3]}")
        print(f"🔩 قطع الغيار: {inv[4]} ريال | 🛠️ أجور اليد: {inv[5]} ريال")
        print(f"💰 الإجمالي: {inv[6]} ريال | ⏰ التاريخ: {inv[7]}")
        print("-" * 40)
    print(f"📈 إجمالي إيرادات الورشة المسجلة: {total_revenue} ريال")

if __name__ == '__main__':
    print("جاري استعراض تقارير النظام...")
    show_all_customers()
    show_all_invoices()
