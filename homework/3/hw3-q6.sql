-- 4 rows
-- 4 seconds
-- Alaska Airlines Inc.
-- SkyWest Airlines Inc.
-- United Air Lines Inc.
-- Virgin America
SELECT DISTINCT c.name AS carrier
FROM Carriers c, (SELECT *
                  FROM Flights
                  WHERE origin_city = 'Seattle WA' AND dest_city = 'San Francisco CA') f
WHERE c.cid = f.carrier_id
ORDER BY carrier;