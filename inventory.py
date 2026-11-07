import json
import sqlite3

with open('config.json', 'r', encoding='utf-8') as f:
    config = json.load(f)

def add_inventory_item(part_number, part_name, category, quantity, unit_price):
    """إضافة قطعة غيار جديدة أو تحديث الكمية في المخزون"""
    conn = sqlite3.connect(config['database']['name'])
    cursor = conn.cursor()
    
    cursor.execute('''
        CREATE TABLE IF NOT EXISTS inventory (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            part_number TEXT UNIQUE,
            part_name TEXT NOT NULL,
            category TEXT,
            quantity INTEGER DEFAULT 0,
            unit_price REAL DEFAULT 0.0,
            last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        )
    ''')
    
    cursor.execute('''
        INSERT INTO inventory (part_number, part_name, category, quantity, unit_price)
        VALUES (?, ?, ?, ?, ?)
        ON CONFLICT(part_number) DO UPDATE SET
            quantity = quantity + excluded.quantity,
            unit_price = excluded.unit_price,
            last_updated = CURRENT_TIMESTAMP
    ''', (part_number, part_name, category, quantity, unit_price))
    
    conn.commit()
    conn.close()
    print(f"تم تسجيل/تحديث قطعة الغيار ({part_name}) برقم القطعة {part_number} بنجاح.")

def show_inventory():
    """استعراض كافة قطع الغيار المتوفرة في مخزون الورشة"""
    conn = sqlite3.connect(config['database']['name'])
    cursor = conn.cursor()
    cursor.execute('''
        CREATE TABLE IF NOT EXISTS inventory (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            part_number TEXT UNIQUE,
            part_name TEXT NOT NULL,
            category TEXT,
            quantity INTEGER DEFAULT 0,
            unit_price REAL DEFAULT 0.0,
            last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        )
    ''')
    cursor.execute('SELECT part_number, part_name, category, quantity, unit_price FROM inventory')
    items = cursor.fetchall()
    conn.close()
    
    print(f"\n=== سجل مخزون قطع الغيار في {config['workshop_name']} ===")
    if not items:
        print("⚠️ لا توجد قطّع غيار مسجلة في المخزون حالياً.")
        return
        
    for item in items:
        print(f"🔢 رقم القطعة: {item[0]} | 🔩 اسم القطعة: {item[1]}")
        print(f"🏷️ التصنيف: {item[2]} | 📦 الكمية المتوفرة: {item[3]}")
        print(f"💵 سعر الوحدة: {item[4]} ريال")
        print("-" * 40)

if __name__ == '__main__':
    print("ملف إدارة المخزون جاهز للاستخدام.")
