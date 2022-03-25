-- 4 rows
-- 1 second
-- Alaska Airlines Inc.
-- SkyWest Airlines Inc.
-- United Air Lines Inc.
-- Virgin America
SELECT DISTINCT c.name AS carrier
FROM Carriers c, Flights f
WHERE c.cid = f.carrier_id AND f.origin_city = 'Seattle WA' AND f.dest_city = 'San Francisco CA'
ORDER BY carrier;