import json
import sqlite3

with open('config.json', 'r', encoding='utf-8') as f:
    config = json.load(f)

def add_diagnostic_report(customer_id, system_type, dtc_codes, technician_notes):
    """تسجيل تقرير فحص بالكمبيوتر وأكواد الأعطال للسيارة"""
    conn = sqlite3.connect(config['database']['name'])
    cursor = conn.cursor()
    
    # التأكد من وجود جدول الفحوصات أو إنشائه تلقائياً
    cursor.execute('''
        CREATE TABLE IF NOT EXISTS diagnostics (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            customer_id INTEGER,
            system_type TEXT,
            dtc_codes TEXT,
            technician_notes TEXT,
            check_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            FOREIGN KEY (customer_id) REFERENCES customers (id)
        )
    ''')
    
    cursor.execute('''
        INSERT INTO diagnostics (customer_id, system_type, dtc_codes, technician_notes)
        VALUES (?, ?, ?, ?)
    ''', (customer_id, system_type, dtc_codes, technician_notes))
    
    conn.commit()
    conn.close()
    print(f"تم حفظ تقرير فحص النظام ({system_type}) بنجاح.")

def show_customer_diagnostics(customer_id):
    """استعراض تقارير الفحص السابقة لعميل معين"""
    conn = sqlite3.connect(config['database']['name'])
    cursor = conn.cursor()
    cursor.execute('''
        SELECT diagnostics.id, diagnostics.system_type, diagnostics.dtc_codes, 
               diagnostics.technician_notes, diagnostics.check_date 
        FROM diagnostics 
        WHERE diagnostics.customer_id = ?
    ''', (customer_id,))
    reports = cursor.fetchall()
    conn.close()
    
    print(f"\n=== سجل فحوصات الأعطال للعميل رقم #{customer_id} ===")
    if not reports:
        print("⚠️ لا توجد تقارير فحص مسجلة لهذا العميل.")
        return
        
    for rep in reports:
        print(f"🔍 تقرير فحص #{rep[0]} | النظام: {rep[1]}")
        print(f"⚡ أكواد الأعطال (DTC): {rep[2]}")
        print(f"🛠️ ملاحظات الفني: {rep[3]}")
        print(f"⏰ وقت الفحص: {rep[4]}")
        print("-" * 40)

if __name__ == '__main__':
    print("ملف تشخيص الأعطال جاهز للاستخدام ضمن النظام.")
