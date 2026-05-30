INSERT INTO users (username, password, role, enabled) VALUES
('driver1', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', 'DRIVER', true),
('driver2', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', 'DRIVER', true),
('admin1',  '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', 'ADMIN',  true)
ON CONFLICT (username) DO NOTHING;

INSERT INTO charging_stations (name, address, latitude, longitude, description) VALUES
('Aristotelous Square Hub',
 'Aristotelous Square, Thessaloniki 546 24',
 40.632944, 22.941472,
 'Central fast-charging hub on the iconic Aristotelous Square, steps from the seafront.'),

('Port of Thessaloniki Charger',
 'Port Authority, Salaminos 7, Thessaloniki 546 26',
 40.635139, 22.933778,
 'Located at the main port entrance, ideal for ferry travellers and port workers.'),

('Ano Poli EV Point',
 'Eptapyrgiou, Thessaloniki 546 34',
 40.643889, 22.961917,
 'Scenic upper city location near the Byzantine walls and Heptapyrgion fortress.'),

('Thessaloniki Airport Charger',
 'Makedonia International Airport, Thessaloniki 570 01',
 40.524722, 22.976972,
 'Dedicated charging bay at the airport departures level.'),

('Kalamaria Marina Charger',
 'Nea Paralia, Kalamaria, Thessaloniki 551 32',
 40.579750, 22.938333,
 'Seafront location in Kalamaria, close to the marina and restaurants.')
ON CONFLICT (name) DO UPDATE
  SET latitude    = EXCLUDED.latitude,
      longitude   = EXCLUDED.longitude,
      description = EXCLUDED.description;

INSERT INTO connectors (station_id, connector_type, power_kw)
SELECT s.id, 'Type 2', 22
FROM   charging_stations s WHERE s.name = 'Aristotelous Square Hub'
ON CONFLICT DO NOTHING;

INSERT INTO connectors (station_id, connector_type, power_kw)
SELECT s.id, 'CCS', 50
FROM   charging_stations s WHERE s.name = 'Aristotelous Square Hub'
ON CONFLICT DO NOTHING;

INSERT INTO connectors (station_id, connector_type, power_kw)
SELECT s.id, 'Type 2', 22
FROM   charging_stations s WHERE s.name = 'Port of Thessaloniki Charger'
ON CONFLICT DO NOTHING;

INSERT INTO connectors (station_id, connector_type, power_kw)
SELECT s.id, 'CHAdeMO', 50
FROM   charging_stations s WHERE s.name = 'Port of Thessaloniki Charger'
ON CONFLICT DO NOTHING;

INSERT INTO connectors (station_id, connector_type, power_kw)
SELECT s.id, 'Type 2', 11
FROM   charging_stations s WHERE s.name = 'Ano Poli EV Point'
ON CONFLICT DO NOTHING;

INSERT INTO connectors (station_id, connector_type, power_kw)
SELECT s.id, 'CCS', 100
FROM   charging_stations s WHERE s.name = 'Thessaloniki Airport Charger'
ON CONFLICT DO NOTHING;

INSERT INTO connectors (station_id, connector_type, power_kw)
SELECT s.id, 'Type 2', 22
FROM   charging_stations s WHERE s.name = 'Thessaloniki Airport Charger'
ON CONFLICT DO NOTHING;

INSERT INTO connectors (station_id, connector_type, power_kw)
SELECT s.id, 'Type 2', 22
FROM   charging_stations s WHERE s.name = 'Kalamaria Marina Charger'
ON CONFLICT DO NOTHING;

INSERT INTO bookings (user_id, connector_id, booking_date, start_time, end_time, status, created_at)
SELECT u.id, c.id, CURRENT_DATE - 10, '09:00', '11:00', 'COMPLETED', NOW() - INTERVAL '10 days'
FROM   users u, connectors c
JOIN   charging_stations s ON s.id = c.station_id
WHERE  u.username = 'driver1' AND s.name = 'Aristotelous Square Hub' AND c.connector_type = 'CCS'
LIMIT  1 ON CONFLICT DO NOTHING;

INSERT INTO bookings (user_id, connector_id, booking_date, start_time, end_time, status, created_at)
SELECT u.id, c.id, CURRENT_DATE - 7, '14:00', '15:30', 'COMPLETED', NOW() - INTERVAL '7 days'
FROM   users u, connectors c
JOIN   charging_stations s ON s.id = c.station_id
WHERE  u.username = 'driver1' AND s.name = 'Thessaloniki Airport Charger' AND c.connector_type = 'CCS'
LIMIT  1 ON CONFLICT DO NOTHING;

INSERT INTO bookings (user_id, connector_id, booking_date, start_time, end_time, status, created_at)
SELECT u.id, c.id, CURRENT_DATE - 4, '08:00', '09:00', 'COMPLETED', NOW() - INTERVAL '4 days'
FROM   users u, connectors c
JOIN   charging_stations s ON s.id = c.station_id
WHERE  u.username = 'driver1' AND s.name = 'Port of Thessaloniki Charger' AND c.connector_type = 'Type 2'
LIMIT  1 ON CONFLICT DO NOTHING;

INSERT INTO bookings (user_id, connector_id, booking_date, start_time, end_time, status, created_at)
SELECT u.id, c.id, CURRENT_DATE - 14, '11:00', '13:00', 'COMPLETED', NOW() - INTERVAL '14 days'
FROM   users u, connectors c
JOIN   charging_stations s ON s.id = c.station_id
WHERE  u.username = 'driver2' AND s.name = 'Kalamaria Marina Charger' AND c.connector_type = 'Type 2'
LIMIT  1 ON CONFLICT DO NOTHING;

INSERT INTO bookings (user_id, connector_id, booking_date, start_time, end_time, status, created_at)
SELECT u.id, c.id, CURRENT_DATE - 5, '16:00', '17:30', 'COMPLETED', NOW() - INTERVAL '5 days'
FROM   users u, connectors c
JOIN   charging_stations s ON s.id = c.station_id
WHERE  u.username = 'driver2' AND s.name = 'Ano Poli EV Point' AND c.connector_type = 'Type 2'
LIMIT  1 ON CONFLICT DO NOTHING;

INSERT INTO bookings (user_id, connector_id, booking_date, start_time, end_time, status, created_at)
SELECT u.id, c.id, CURRENT_DATE + 1, '10:00', '12:00', 'CONFIRMED', NOW()
FROM   users u, connectors c
JOIN   charging_stations s ON s.id = c.station_id
WHERE  u.username = 'driver1'
AND    s.name = 'Aristotelous Square Hub'
AND    c.connector_type = 'Type 2'
LIMIT  1
ON CONFLICT DO NOTHING;

INSERT INTO bookings (user_id, connector_id, booking_date, start_time, end_time, status, created_at)
SELECT u.id, c.id, CURRENT_DATE + 2, '14:00', '16:00', 'CONFIRMED', NOW()
FROM   users u, connectors c
JOIN   charging_stations s ON s.id = c.station_id
WHERE  u.username = 'driver1'
AND    s.name = 'Kalamaria Marina Charger'
AND    c.connector_type = 'Type 2'
LIMIT  1
ON CONFLICT DO NOTHING;
