import json
from app import add_customer, create_invoice
from database import init_db
from reports import show_all_customers, show_all_invoices

with open('config.json', 'r', encoding='utf-8') as f:
    config = json.load(f)

def main_menu():
    init_db()
    while True:
        print(f"\n=== نظام إدارة {config['workshop_name']} ===")
        print(f"📍 العنوان: {config['location']} | ☎️ للتواصل: {config['primary_contact']}")
        print("----------------------------------------")
        print("1. تسجيل عميل ومركبة جديدة")
        print("2. إصدار فاتورة صيانة جديدة")
        print("3. استعراض سجل العملاء والمركبات")
        print("4. استعراض فواتير الصيانة وإجمالي الإيرادات")
        print("5. خروج من النظام")
        
        choice = input("\nاختر رقم العملية المطلوبة (1-5): ").strip()
        
        if choice == '1':
            print("\n--- تسجيل بيانات العميل والسيارة ---")
            name = input("اسم العميل: ").strip()
            phone = input("رقم هاتف العميل: ").strip()
            car_model = input("موديل السيارة (مثلاً: هونداي النترا / فورد إكسبلورر): ").strip()
            plate_number = input("رقم اللوحة: ").strip()
            notes = input("ملاحظات الأعطال أو الفحص الأولي: ").strip()
            
            add_customer(name, phone, car_model, plate_number, notes)
            
        elif choice == '2':
            print("\n--- إصدار فاتورة صيانة ---")
            try:
                customer_id = int(input("أدخل معرف العميل (Customer ID): ").strip())
                service_details = input("تفاصيل أعمال الصيانة والإصلاح: ").strip()
                parts_cost = float(input("تكلفة قطع الغيار (بالريال): ").strip())
                labor_cost = float(input("أجور اليد العاملة (بالريال): ").strip())
                
                create_invoice(customer_id, service_details, parts_cost, labor_cost)
            except ValueError:
                print("⚠️ خطأ في الإدخال: يرجى التأكد من كتابة أرقام صحيحة لتكاليف القطع وأجور العمل.")
                
        elif choice == '3':
            show_all_customers()
            
        elif choice == '4':
            show_all_invoices()
            
        elif choice == '5':
            print(f"\nمع السلامة يا مهندس صلاح. نتمنى لك التوفيق في إدارة {config['workshop_name']}.")
            break
        else:
            print("⚠️ خيار غير صحيح، يرجى اختيار رقم بين 1 و 5.")

if __name__ == '__main__':
    main_menu()
