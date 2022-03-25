-- 1 row
SELECT w.day_of_week AS day_of_week, AVG(f.arrival_delay) AS delay
FROM Flights f, Weekdays w
WHERE f.day_of_week_id = w.did
GROUP BY w.did, w.day_of_week
ORDER BY delay DESC
LIMIT 1;