import json
from app import add_customer, create_invoice
from database import init_db
from reports import show_all_customers, show_all_invoices
from diagnostics import add_diagnostic_report, show_customer_diagnostics
from inventory import add_inventory_item, show_inventory

with open('config.json', 'r', encoding='utf-8') as f:
    config = json.load(f)

def main_menu():
    init_db()
    while True:
        print(f"\n=== نظام إدارة {config['workshop_name']} ===")
        print(f"📍 العنوان: {config['location']} | ☎️ للتواصل: {config['primary_contact']}")
        print("----------------------------------------")
        print("1. تسجيل عميل ومركبة جديدة")
        print("2. تسجيل تقرير فحص كمبيوتر وأكواد أعطال (DTC)")
        print("3. استعراض فحوصات الأعطال لعميل")
        print("4. إصدار فاتورة صيانة جديدة")
        print("5. استعراض سجل العملاء والمركبات")
        print("6. استعراض فواتير الصيانة وإجمالي الإيرادات")
        print("7. إضافة أو تحديث قطعة غيار في المخزون")
        print("8. استعراض مخزون قطع الغيار")
        print("9. خروج من النظام")
        
        choice = input("\nاختر رقم العملية المطلوبة (1-9): ").strip()
        
        if choice == '1':
            print("\n--- تسجيل بيانات العميل والسيارة ---")
            name = input("اسم العميل: ").strip()
            phone = input("رقم هاتف العميل: ").strip()
            car_model = input("موديل السيارة (مثلاً: هونداي النترا / فورد إكسبلورر): ").strip()
            plate_number = input("رقم اللوحة: ").strip()
            notes = input("ملاحظات الأعطال أو الفحص الأولي: ").strip()
            add_customer(name, phone, car_model, plate_number, notes)
            
        elif choice == '2':
            print("\n--- تسجيل تقرير فحص بالكمبيوتر ---")
            try:
                customer_id = int(input("أدخل معرف العميل (Customer ID): ").strip())
                system_type = input("النظام المعني (مثلاً: المحرك / قير / إنفرتر هجين / منظومة التوجيه): ").strip()
                dtc_codes = input("أكواد الأعطال DTC (مثلاً: P0300, P0A80): ").strip()
                technician_notes = input("ملاحظات الفحص التشخيصي والتوصيات: ").strip()
                add_diagnostic_report(customer_id, system_type, dtc_codes, technician_notes)
            except ValueError:
                print("⚠️ خطأ في الإدخال: يرجى التأكد من كتابة رقم صحيح لمعرف العميل.")
                
        elif choice == '3':
            try:
                customer_id = int(input("أدخل معرف العميل لاستعراض فحوصاته: ").strip())
                show_customer_diagnostics(customer_id)
            except ValueError:
                print("⚠️ خطأ في الإدخال: يرجى التأكد من كتابة رقم صحيح لمعرف العميل.")
                
        elif choice == '4':
            print("\n--- إصدار فاتورة صيانة ---")
            try:
                customer_id = int(input("أدخل معرف العميل (Customer ID): ").strip())
                service_details = input("تفاصيل أعمال الصيانة والإصلاح: ").strip()
                parts_cost = float(input("تكلفة قطع الغيار (بالريال): ").strip())
                labor_cost = float(input("أجور اليد العاملة (بالريال): ").strip())
                create_invoice(customer_id, service_details, parts_cost, labor_cost)
            except ValueError:
                print("⚠️ خطأ في الإدخال: يرجى التأكد من كتابة أرقام صحيحة لتكاليف القطع وأجور العمل.")
                
        elif choice == '5':
            show_all_customers()
            
        elif choice == '6':
            show_all_invoices()
            
        elif choice == '7':
            print("\n--- إضافة / تحديث قطعة غيار في المخزون ---")
            try:
                part_number = input("رقم القطعة (Part Number): ").strip()
                part_name = input("اسم القطعة: ").strip()
                category = input("التصنيف (مثلاً: فلاتر، حساسات، قطع هجين): ").strip()
                quantity = int(input("الكمية المضافة: ").strip())
                unit_price = float(input("سعر الوحدة (بالريال): ").strip())
                add_inventory_item(part_number, part_name, category, quantity, unit_price)
            except ValueError:
                print("⚠️ خطأ في الإدخال: يرجى التأكد من كتابة أرقام صحيحة للكمية والسعر.")
                
        elif choice == '8':
            show_inventory()
            
        elif choice == '9':
            print(f"\nمع السلامة يا مهندس صلاح. نتمنى لك التوفيق في إدارة {config['workshop_name']}.")
            break
        else:
            print("⚠️ خيار غير صحيح، يرجى اختيار رقم بين 1 و 9.")

if __name__ == '__main__':
    main_menu()
