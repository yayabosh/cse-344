-- 3 rows
-- 20 seconds
-- Devils Lake ND
-- Hattiesburg/Laurel MS
-- St. Augustine FL
SELECT DISTINCT dest_city AS city
FROM Flights
WHERE dest_city NOT IN (
	  	SELECT DISTINCT f3.dest_city
	  	FROM Flights f2, Flights f3
	  	WHERE f2.origin_city = 'Seattle WA' AND f2.dest_city = f3.origin_city
	  ) AND dest_city NOT IN (
	  	SELECT DISTINCT dest_city
	  	FROM Flights
		WHERE origin_city = 'Seattle WA'
	  )
ORDER BY city;