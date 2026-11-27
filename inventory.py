import sqlite3
from config import get_database_path, load_config

config = load_config()


def _ensure_inventory_table(cursor):
    cursor.execute(
        """
        CREATE TABLE IF NOT EXISTS inventory (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            part_number TEXT UNIQUE,
            part_name TEXT NOT NULL,
            category TEXT,
            quantity INTEGER DEFAULT 0,
            unit_price REAL DEFAULT 0.0,
            last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        )
        """
    )


def add_inventory_item(part_number, part_name, category, quantity, unit_price):
    """إضافة قطعة غيار أو زيادة كميتها في المخزون"""
    with sqlite3.connect(get_database_path()) as conn:
        cursor = conn.cursor()
        _ensure_inventory_table(cursor)
        cursor.execute(
            """
            INSERT INTO inventory (part_number, part_name, category, quantity, unit_price)
            VALUES (?, ?, ?, ?, ?)
            ON CONFLICT(part_number) DO UPDATE SET
                part_name = excluded.part_name,
                category = excluded.category,
                quantity = quantity + excluded.quantity,
                unit_price = excluded.unit_price,
                last_updated = CURRENT_TIMESTAMP
            """,
            (part_number, part_name, category, quantity, unit_price),
        )
    print(f"تم تسجيل/تحديث قطعة الغيار ({part_name}) بنجاح.")


def show_inventory():
    """استعراض قطع الغيار المتوفرة في مخزون الورشة"""
    with sqlite3.connect(get_database_path()) as conn:
        cursor = conn.cursor()
        _ensure_inventory_table(cursor)
        items = cursor.execute(
            "SELECT part_number, part_name, category, quantity, unit_price FROM inventory ORDER BY part_name"
        ).fetchall()
    print(f"\n=== مخزون {config['workshop_name']} ===")
    if not items:
        print("لا توجد قطع غيار مسجلة حالياً.")
        return
    for item in items:
        print(f"رقم القطعة: {item[0]} | الاسم: {item[1]}")
        print(f"التصنيف: {item[2]} | الكمية: {item[3]} | سعر الوحدة: {item[4]} ريال")
        print("-" * 40)


if __name__ == "__main__":
    show_inventory()
