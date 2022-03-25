-- 3 rows
SELECT DISTINCT x.flight_num
FROM Flights x, Weekdays y, Carriers z
WHERE x.origin_city = 'Seattle WA' AND x.dest_city = 'Boston MA' 
      AND x.day_of_week_id = y.did AND y.day_of_week = 'Monday' 
      AND x.carrier_id = z.cid     AND z.name = 'Alaska Airlines Inc.';