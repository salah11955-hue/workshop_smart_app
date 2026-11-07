from app import add_customer, create_invoice
from database import init_db

if __name__ == '__main__':
    print("=== بدء نظام إدارة ورشة خبراء هونداي وفورد ===")
    
    # تهيئة قاعدة البيانات والجداول
    init_db()
    
    # تجربة تسجيل عميل جديد (مثلاً لسيارة هونداي أو فورد)
    customer_id = add_customer(
        name="محمد علي",
        phone="770000000",
        car_model="هونداي النترا 2015",
        plate_number="5678 / أ",
        notes="فحص منظومة المحرك وتغيير زيت القير"
    )
    
    # تجربة إصدار فاتورة صيانة وقطع غيار بالريال
    invoice_id = create_invoice(
        customer_id=customer_id,
        service_details="تغيير طقم الفلاتر وزيت المحرك مع الفحص بالكمبيوتر",
        parts_cost=25000,
        labor_cost=10000
    )
    
    print("=== تمت عملية الاختبار بنجاح وحفظ البيانات في قاعدة البيانات المحلية ===")
