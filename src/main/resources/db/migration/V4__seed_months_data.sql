-- ═══════════════════════════════════════════════════════════════════════
-- V4: Seed 12 months with Palestinian weather descriptions
-- بذر بيانات الأشهر الـ 12 مع وصف الطقس في فلسطين
-- ═══════════════════════════════════════════════════════════════════════

-- تأكد الجدول فاضي قبل ما نضيف
INSERT INTO months (month_number, name_ar, name_en, season, weather_description_ar, weather_description_en, image_url)
SELECT 1, 'كانون الثاني', 'January', 'WINTER',
       'أبرد أشهر السنة في فلسطين. أمطار غزيرة وثلوج على المرتفعات الجبلية (القدس، الخليل). درجات الحرارة تتراوح بين 5-12°م. موسم ممتاز لزراعة الخضروات الشتوية والأعشاب المقاومة للبرد.',
       'Coldest month in Palestine. Heavy rain and possible snow in mountain areas (Jerusalem, Hebron). Temperatures range 5-12°C. Excellent season for winter vegetables and cold-resistant herbs.',
       NULL
WHERE NOT EXISTS (SELECT 1 FROM months WHERE month_number = 1);

INSERT INTO months (month_number, name_ar, name_en, season, weather_description_ar, weather_description_en, image_url)
SELECT 2, 'شباط', 'February', 'WINTER',
       'استمرار البرد والأمطار مع بداية ارتفاع تدريجي بدرجات الحرارة. موسم أزهار اللوز. مناسب لتحضير التربة وبدء زراعة البذور داخل البيت.',
       'Continued cold and rain with gradually rising temperatures. Almond blossom season. Good time to prepare soil and start indoor seed planting.',
       NULL
WHERE NOT EXISTS (SELECT 1 FROM months WHERE month_number = 2);

INSERT INTO months (month_number, name_ar, name_en, season, weather_description_ar, weather_description_en, image_url)
SELECT 3, 'آذار', 'March', 'SPRING',
       'بداية الربيع! الطقس يعتدل وتنخفض الأمطار. درجات الحرارة 12-20°م. أفضل وقت لزراعة معظم الأعشاب والخضروات. تفتح الأزهار البرية في كل فلسطين.',
       'Spring begins! Weather turns mild, rainfall decreases. Temperatures 12-20°C. Best time to plant most herbs and vegetables. Wildflowers bloom across Palestine.',
       NULL
WHERE NOT EXISTS (SELECT 1 FROM months WHERE month_number = 3);

INSERT INTO months (month_number, name_ar, name_en, season, weather_description_ar, weather_description_en, image_url)
SELECT 4, 'نيسان', 'April', 'SPRING',
       'طقس ربيعي مثالي للزراعة. مشمس ودافئ، 15-25°م. آخر الأمطار الربيعية. أفضل شهر لنقل الشتلات للخارج وزراعة الطماطم والفلفل والريحان.',
       'Ideal spring weather for planting. Sunny and warm, 15-25°C. Last spring rains. Best month for transplanting seedlings outdoors and planting tomatoes, peppers, and basil.',
       NULL
WHERE NOT EXISTS (SELECT 1 FROM months WHERE month_number = 4);

INSERT INTO months (month_number, name_ar, name_en, season, weather_description_ar, weather_description_en, image_url)
SELECT 5, 'أيار', 'May', 'SPRING',
       'نهاية الربيع، الطقس يصبح حاراً في المناطق المنخفضة (أريحا، الأغوار 35°م+). المرتفعات لا تزال معتدلة 20-28°م. موسم حصاد بعض الخضروات الربيعية.',
       'Late spring, weather turns hot in low areas (Jericho, Jordan Valley 35°C+). Highlands still moderate 20-28°C. Harvest season for some spring vegetables.',
       NULL
WHERE NOT EXISTS (SELECT 1 FROM months WHERE month_number = 5);

INSERT INTO months (month_number, name_ar, name_en, season, weather_description_ar, weather_description_en, image_url)
SELECT 6, 'حزيران', 'June', 'SUMMER',
       'بداية الصيف الحار والجاف. درجات الحرارة 25-33°م. لا أمطار تقريباً. الري المنتظم ضروري جداً. مناسب لزراعة الخيار والباميا والبطيخ.',
       'Hot dry summer begins. Temperatures 25-33°C. Almost no rain. Regular watering is essential. Good for planting cucumbers, okra, and watermelon.',
       NULL
WHERE NOT EXISTS (SELECT 1 FROM months WHERE month_number = 6);

INSERT INTO months (month_number, name_ar, name_en, season, weather_description_ar, weather_description_en, image_url)
SELECT 7, 'تموز', 'July', 'SUMMER',
       'ذروة الحر في فلسطين. درجات الحرارة 28-38°م. حرارة شديدة في الأغوار وغزة. يجب الري مرتين يومياً للنباتات الخارجية. موسم حصاد الطماطم والخيار.',
       'Peak heat in Palestine. Temperatures 28-38°C. Extreme heat in Jordan Valley and Gaza. Water plants twice daily outdoors. Tomato and cucumber harvest season.',
       NULL
WHERE NOT EXISTS (SELECT 1 FROM months WHERE month_number = 7);

INSERT INTO months (month_number, name_ar, name_en, season, weather_description_ar, weather_description_en, image_url)
SELECT 8, 'آب', 'August', 'SUMMER',
       'استمرار الحر الشديد. أحر شهر في معظم المناطق. الرطوبة عالية في المناطق الساحلية. مناسب لتحضير بذور الموسم الخريفي. حصاد التين والعنب.',
       'Continued extreme heat. Hottest month in most areas. High humidity in coastal areas. Good time to prepare autumn season seeds. Fig and grape harvest.',
       NULL
WHERE NOT EXISTS (SELECT 1 FROM months WHERE month_number = 8);

INSERT INTO months (month_number, name_ar, name_en, season, weather_description_ar, weather_description_en, image_url)
SELECT 9, 'أيلول', 'September', 'AUTUMN',
       'بداية الخريف مع انخفاض تدريجي بالحرارة. 22-32°م. أول أمطار خريفية في نهاية الشهر. مناسب جداً لزراعة الخس والسبانخ والبقدونس.',
       'Autumn begins with gradual temperature drop. 22-32°C. First autumn rains at month end. Excellent for planting lettuce, spinach, and parsley.',
       NULL
WHERE NOT EXISTS (SELECT 1 FROM months WHERE month_number = 9);

INSERT INTO months (month_number, name_ar, name_en, season, weather_description_ar, weather_description_en, image_url)
SELECT 10, 'تشرين الأول', 'October', 'AUTUMN',
       'طقس خريفي معتدل ومريح. 18-27°م. عودة الأمطار بشكل أوضح. موسم زراعة الثوم والبصل والفول. بداية موسم الزيتون.',
       'Pleasant mild autumn weather. 18-27°C. Rain returns more noticeably. Season for planting garlic, onions, and fava beans. Olive harvest begins.',
       NULL
WHERE NOT EXISTS (SELECT 1 FROM months WHERE month_number = 10);

INSERT INTO months (month_number, name_ar, name_en, season, weather_description_ar, weather_description_en, image_url)
SELECT 11, 'تشرين الثاني', 'November', 'AUTUMN',
       'الخريف يتحول للشتاء. أمطار متزايدة وبرودة ملحوظة. 12-20°م. موسم قطف الزيتون. مناسب لزراعة البازيلاء والسبانخ الشتوية وإكليل الجبل.',
       'Autumn transitioning to winter. Increasing rain and notable cold. 12-20°C. Olive pressing season. Good for planting peas, winter spinach, and rosemary.',
       NULL
WHERE NOT EXISTS (SELECT 1 FROM months WHERE month_number = 11);

INSERT INTO months (month_number, name_ar, name_en, season, weather_description_ar, weather_description_en, image_url)
SELECT 12, 'كانون الأول', 'December', 'WINTER',
       'شتاء بارد وماطر. درجات الحرارة 7-14°م. ثلوج محتملة في المرتفعات. مناسب لزراعة الثوم والبصل والأعشاب الشتوية داخل البيت. فترة راحة لمعظم النباتات.',
       'Cold rainy winter. Temperatures 7-14°C. Possible snow in highlands. Good for planting garlic, onions, and indoor winter herbs. Rest period for most plants.',
       NULL
WHERE NOT EXISTS (SELECT 1 FROM months WHERE month_number = 12);
