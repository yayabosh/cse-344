-- 334 rows
-- 7 seconds
SELECT DISTINCT f.origin_city, f.dest_city, f.actual_time AS time
FROM Flights f, (SELECT origin_city, MAX(actual_time) AS max_time
                 FROM Flights
                 GROUP BY origin_city) ff
WHERE f.origin_city = ff.origin_city AND f.actual_time = ff.max_time
ORDER BY origin_city, dest_city;