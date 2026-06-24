-- V2 — Curated exercise -> muscle mapping for the 40 exercises in the real export (CLAUDE.md §0, §2).
--
-- Muscle vocabulary (primary_muscle) and how Phase 1 split-classification will bucket it:
--   PUSH : CHEST, SHOULDERS, TRICEPS
--   PULL : BACK, REAR_DELTS, TRAPS, BICEPS, FOREARMS
--   LOWER: QUADS, HAMSTRINGS, GLUTES, ABDUCTORS, ADDUCTORS, CALVES
--   CORE : ABS
-- Bodyweight movements get equipment=BODYWEIGHT; per-set load_basis is still decided at ingest
-- (e.g. "Triceps Dip (Weighted)" is mixed — 205/247 sets carry no added weight).

INSERT INTO exercise (hevy_name, canonical_name, primary_muscle, secondary_muscles, equipment, movement_type, is_unilateral) VALUES
  ('Preacher Curl (Machine)',                     'Preacher Curl',               'BICEPS',     '[]',                          'MACHINE',    'ISOLATION', FALSE),
  ('Pull Up',                                     'Pull Up',                     'BACK',       '["LATS","BICEPS"]',           'BODYWEIGHT', 'COMPOUND',  FALSE),
  ('Squat (Barbell)',                             'Squat',                       'QUADS',      '["GLUTES","HAMSTRINGS"]',     'BARBELL',    'COMPOUND',  FALSE),
  ('Romanian Deadlift (Barbell)',                 'Romanian Deadlift',           'HAMSTRINGS', '["GLUTES","BACK"]',           'BARBELL',    'COMPOUND',  FALSE),
  ('Triceps Extension (Cable)',                   'Triceps Extension',           'TRICEPS',    '[]',                          'CABLE',      'ISOLATION', FALSE),
  ('Lateral Raise (Cable)',                       'Lateral Raise',               'SHOULDERS',  '[]',                          'CABLE',      'ISOLATION', TRUE),
  ('Face Pull',                                   'Face Pull',                   'REAR_DELTS', '["TRAPS","BACK"]',            'CABLE',      'ISOLATION', FALSE),
  ('Leg Extension (Machine)',                     'Leg Extension',               'QUADS',      '[]',                          'MACHINE',    'ISOLATION', FALSE),
  ('Seated Leg Curl (Machine)',                   'Seated Leg Curl',             'HAMSTRINGS', '[]',                          'MACHINE',    'ISOLATION', FALSE),
  ('Chest Fly (Machine)',                         'Chest Fly',                   'CHEST',      '[]',                          'MACHINE',    'ISOLATION', FALSE),
  ('Triceps Dip (Weighted)',                      'Triceps Dip',                 'TRICEPS',    '["CHEST","SHOULDERS"]',       'BODYWEIGHT', 'COMPOUND',  FALSE),
  ('Overhead Press (Dumbbell)',                   'Overhead Press',              'SHOULDERS',  '["TRICEPS"]',                 'DUMBBELL',   'COMPOUND',  FALSE),
  ('Incline Bench Press (Dumbbell)',              'Incline Bench Press',         'CHEST',      '["SHOULDERS","TRICEPS"]',     'DUMBBELL',   'COMPOUND',  FALSE),
  ('Hip Abduction (Machine)',                     'Hip Abduction',               'ABDUCTORS',  '["GLUTES"]',                  'MACHINE',    'ISOLATION', FALSE),
  ('Seated Cable Row - Bar Wide Grip',           'Seated Cable Row (Wide)',     'BACK',       '["BICEPS","REAR_DELTS"]',     'CABLE',      'COMPOUND',  FALSE),
  ('Bench Press (Dumbbell)',                      'Bench Press',                 'CHEST',      '["TRICEPS","SHOULDERS"]',     'DUMBBELL',   'COMPOUND',  FALSE),
  ('Cable Crunch',                                'Cable Crunch',                'ABS',        '[]',                          'CABLE',      'ISOLATION', FALSE),
  ('Chin Up',                                     'Chin Up',                     'BACK',       '["LATS","BICEPS"]',           'BODYWEIGHT', 'COMPOUND',  FALSE),
  ('Shrug (Dumbbell)',                            'Shrug',                       'TRAPS',      '[]',                          'DUMBBELL',   'ISOLATION', FALSE),
  ('Reverse Grip Lat Pulldown (Cable)',          'Reverse Grip Lat Pulldown',   'BACK',       '["LATS","BICEPS"]',           'CABLE',      'COMPOUND',  FALSE),
  ('Seated Palms Up Wrist Curl',                  'Seated Wrist Curl',           'FOREARMS',   '[]',                          'BARBELL',    'ISOLATION', FALSE),
  ('Lat Pulldown (Cable)',                        'Lat Pulldown',                'BACK',       '["LATS","BICEPS"]',           'CABLE',      'COMPOUND',  FALSE),
  ('Scapular Pull Ups',                           'Scapular Pull Up',            'BACK',       '["TRAPS","LATS"]',            'BODYWEIGHT', 'ISOLATION', FALSE),
  ('Behind the Back Bicep Wrist Curl (Barbell)', 'Behind-the-Back Wrist Curl',  'FOREARMS',   '[]',                          'BARBELL',    'ISOLATION', FALSE),
  ('Triceps Dip',                                 'Triceps Dip',                 'TRICEPS',    '["CHEST","SHOULDERS"]',       'BODYWEIGHT', 'COMPOUND',  FALSE),
  ('Seated Cable Row - Bar Grip',                'Seated Cable Row',            'BACK',       '["BICEPS","REAR_DELTS"]',     'CABLE',      'COMPOUND',  FALSE),
  ('Seated Shoulder Press (Machine)',             'Seated Shoulder Press',       'SHOULDERS',  '["TRICEPS"]',                 'MACHINE',    'COMPOUND',  FALSE),
  ('Hammer Curl (Dumbbell)',                      'Hammer Curl',                 'BICEPS',     '["FOREARMS"]',                'DUMBBELL',   'ISOLATION', FALSE),
  ('Hanging Leg Raise',                           'Hanging Leg Raise',           'ABS',        '[]',                          'BODYWEIGHT', 'ISOLATION', FALSE),
  ('Bent Over Row (Dumbbell)',                    'Bent Over Row',               'BACK',       '["BICEPS","REAR_DELTS"]',     'DUMBBELL',   'COMPOUND',  TRUE),
  ('Standing Calf Raise (Dumbbell)',              'Standing Calf Raise',         'CALVES',     '[]',                          'DUMBBELL',   'ISOLATION', FALSE),
  ('Lateral Raise (Dumbbell)',                    'Lateral Raise',               'SHOULDERS',  '[]',                          'DUMBBELL',   'ISOLATION', FALSE),
  ('Bicep Curl (Dumbbell)',                       'Bicep Curl',                  'BICEPS',     '[]',                          'DUMBBELL',   'ISOLATION', FALSE),
  ('Bench Press (Barbell)',                       'Bench Press',                 'CHEST',      '["TRICEPS","SHOULDERS"]',     'BARBELL',    'COMPOUND',  FALSE),
  ('Hip Adduction (Machine)',                     'Hip Adduction',               'ADDUCTORS',  '[]',                          'MACHINE',    'ISOLATION', FALSE),
  ('Overhead Press (Barbell)',                    'Overhead Press',              'SHOULDERS',  '["TRICEPS"]',                 'BARBELL',    'COMPOUND',  FALSE),
  ('Leg Raise Parallel Bars',                     'Leg Raise (Parallel Bars)',   'ABS',        '[]',                          'BODYWEIGHT', 'ISOLATION', FALSE),
  ('Standing Calf Raise (Barbell)',               'Standing Calf Raise',         'CALVES',     '[]',                          'BARBELL',    'ISOLATION', FALSE),
  ('Reverse Curl (Dumbbell)',                     'Reverse Curl',                'FOREARMS',   '["BICEPS"]',                  'DUMBBELL',   'ISOLATION', FALSE),
  ('Triceps Pushdown',                            'Triceps Pushdown',            'TRICEPS',    '[]',                          'CABLE',      'ISOLATION', FALSE);
