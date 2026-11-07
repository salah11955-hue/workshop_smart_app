import json
import sqlite3
from database import init_db

# تحميل بيانات الورشة الأساسية
with open('config.json', 'r', encoding='utf-8') as f:
    config = json.load(f)

def add_customer(name, phone, car_model, plate_number, notes=""):
    """إضافة عميل وسيارة جديدة إلى قاعدة بيانات الورشة"""
    conn = sqlite3.connect(config['database']['name'])
    cursor = conn.cursor()
    cursor.execute('''
        INSERT INTO customers (name, phone, car_model, plate_number, notes)
        VALUES (?, ?, ?, ?, ?)
    ''', (name, phone, car_model, plate_number, notes))
    conn.commit()
    customer_id = cursor.lastrowid
    conn.close()
    print(f"تم تسجيل العميل {name} لسيارة {car_model} بنجاح (معرف العميل: {customer_id}).")
    return customer_id

def create_invoice(customer_id, service_details, parts_cost, labor_cost):
    """حساب وإنشاء فاتورة صيانة للعميل بالريال"""
    total_amount = parts_cost + labor_cost
    conn = sqlite3.connect(config['database']['name'])
    cursor = conn.cursor()
    cursor.execute('''
        INSERT INTO invoices (customer_id, service_details, parts_cost, labor_cost, total_amount)
        VALUES (?, ?, ?, ?, ?)
    ''', (customer_id, service_details, parts_cost, labor_cost, total_amount))
    conn.commit()
    invoice_id = cursor.lastrowid
    conn.close()
    print(f"تم إصدّار الفاتورة رقم #{invoice_id} بالمبلغ الإجمالي: {total_amount}")
    return invoice_id

if __name__ == '__main__':
    # تهيئة قاعدة البيانات والتأكد من جاهزية النظام
    init_db()
    print(f"نظام {config['workshop_name']} جاهز للعمل. للتواصل: {config['primary_contact']}")
