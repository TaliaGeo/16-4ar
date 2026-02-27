-- ═══════════════════════════════════════════════════════════════════════
-- V5: Seed default task types for plant care
-- بذر أنواع المهام الافتراضية لرعاية النباتات
-- ═══════════════════════════════════════════════════════════════════════

INSERT INTO task_types (name_ar, name_en, icon, description_ar, description_en)
SELECT 'ري', 'Watering', '💧',
       'سقاية النبات بالماء حسب احتياجاته. الري المنتظم ضروري لنمو صحي.',
       'Water the plant according to its needs. Regular watering is essential for healthy growth.'
WHERE NOT EXISTS (SELECT 1 FROM task_types WHERE name_en = 'Watering');

INSERT INTO task_types (name_ar, name_en, icon, description_ar, description_en)
SELECT 'تسميد', 'Fertilizing', '🧪',
       'إضافة السماد لتغذية النبات وتعزيز نموه. يختلف حسب نوع النبات والموسم.',
       'Add fertilizer to nourish the plant and boost growth. Varies by plant type and season.'
WHERE NOT EXISTS (SELECT 1 FROM task_types WHERE name_en = 'Fertilizing');

INSERT INTO task_types (name_ar, name_en, icon, description_ar, description_en)
SELECT 'تقليم', 'Pruning', '✂️',
       'قص الأجزاء الميتة أو الزائدة لتشجيع نمو جديد وصحي وتحسين شكل النبات.',
       'Trim dead or excess parts to encourage new healthy growth and improve plant shape.'
WHERE NOT EXISTS (SELECT 1 FROM task_types WHERE name_en = 'Pruning');

INSERT INTO task_types (name_ar, name_en, icon, description_ar, description_en)
SELECT 'حصاد', 'Harvesting', '🌾',
       'جمع الثمار أو الأوراق الناضجة. الحصاد في الوقت المناسب يحسن الطعم والجودة.',
       'Collect ripe fruits or mature leaves. Timely harvesting improves taste and quality.'
WHERE NOT EXISTS (SELECT 1 FROM task_types WHERE name_en = 'Harvesting');

INSERT INTO task_types (name_ar, name_en, icon, description_ar, description_en)
SELECT 'مكافحة آفات', 'Pest Control', '🐛',
       'فحص النبات ومعالجته من الحشرات والأمراض. الوقاية خير من العلاج.',
       'Inspect and treat the plant for insects and diseases. Prevention is better than cure.'
WHERE NOT EXISTS (SELECT 1 FROM task_types WHERE name_en = 'Pest Control');

INSERT INTO task_types (name_ar, name_en, icon, description_ar, description_en)
SELECT 'تغيير تربة', 'Repotting', '🪴',
       'نقل النبات إلى أصيص أكبر أو تجديد التربة. ضروري عندما تمتلئ الجذور.',
       'Move the plant to a larger pot or refresh soil. Necessary when roots are crowded.'
WHERE NOT EXISTS (SELECT 1 FROM task_types WHERE name_en = 'Repotting');

INSERT INTO task_types (name_ar, name_en, icon, description_ar, description_en)
SELECT 'تعريض لشمس', 'Sun Exposure', '☀️',
       'ضمان حصول النبات على كمية كافية من ضوء الشمس حسب احتياجاته.',
       'Ensure the plant gets adequate sunlight according to its needs.'
WHERE NOT EXISTS (SELECT 1 FROM task_types WHERE name_en = 'Sun Exposure');
