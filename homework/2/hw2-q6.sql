-- 3 rows
SELECT c.name AS carrier, MAX(f.price) AS max_price
FROM Flights f, Carriers c
WHERE ((f.origin_city = 'Seattle WA' AND f.dest_city = 'New York NY')
       OR (f.origin_city = 'New York NY' AND f.dest_city = 'Seattle WA')) 
      AND f.carrier_id = c.cid
GROUP BY c.cid, c.name;