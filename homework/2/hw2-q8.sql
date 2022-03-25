-- 22 rows
SELECT c.name, SUM(f.departure_delay) AS delay
FROM Flights f, Carriers c
WHERE f.carrier_id = c.cid
GROUP BY c.cid, c.name;