import sqlite3
import json

# تحميل الإعدادات الأساسية للورشة من ملف config.json
with open('config.json', 'r', encoding='utf-8') as f:
    config = json.load(f)

db_name = config['database']['name']

def init_db():
    """إنشاء قاعدة البيانات وجداول النظام الأساسية لورشة خبراء هونداي وفورد"""
    conn = sqlite3.connect(db_name)
    cursor = conn.cursor()
    
    # جدول العملاء والمركبات
    cursor.execute('''
        CREATE TABLE IF NOT EXISTS customers (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            name TEXT NOT NULL,
            phone TEXT NOT NULL,
            car_model TEXT,
            plate_number TEXT,
            notes TEXT,
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        )
    ''')
    
    # جدول الفواتير وأعمال الصيانة
    cursor.execute('''
        CREATE TABLE IF NOT EXISTS invoices (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            customer_id INTEGER,
            service_details TEXT,
            parts_cost REAL,
            labor_cost REAL,
            total_amount REAL,
            invoice_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            FOREIGN KEY (customer_id) REFERENCES customers (id)
        )
    ''')
    
    conn.commit()
    conn.close()
    print(f"تم تهيئة قاعدة البيانات '{db_name}' بنجاح لصالح {config['workshop_name']}.")

if __name__ == '__main__':
    init_db()
