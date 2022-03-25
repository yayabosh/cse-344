-- 12 rows
SELECT DISTINCT c.name
FROM Flights f, Carriers c
WHERE c.cid = f.carrier_id
GROUP BY c.cid, c.name, f.month_id, f.day_of_month
HAVING COUNT(*) > 1000;