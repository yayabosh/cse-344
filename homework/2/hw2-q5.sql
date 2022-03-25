-- 6 rows
SELECT c.name, 100.0 * SUM(f.canceled) / COUNT(f.canceled) AS percentage
FROM Flights f, Carriers c
WHERE f.origin_city = 'Seattle WA' AND c.cid = f.carrier_id
GROUP BY c.cid, c.name
HAVING percentage > 0.5
ORDER BY percentage ASC;