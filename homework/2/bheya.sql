-- top 10 airlines by number of flights
-- SELECT c.name, COUNT(*)
-- FROM Flights f, Carriers c
-- WHERE f.carrier_id = c.cid
-- GROUP BY f.carrier_id
-- ORDER BY COUNT(*) DESC
-- LIMIT 10;

-- carriers that are the earliest (smallest departure delay)
-- SELECT COUNT(*), c.name, AVG(f.departure_delay) AS avg
-- FROM Flights f, Carriers c
-- WHERE f.carrier_id = c.cid
-- GROUP BY f.carrier_id
-- ORDER BY avg DESC
-- LIMIT 20;

-- biggest carriers by passengers
-- SELECT c.name, SUM(f.capacity) AS passengers
-- FROM Flights f, Carriers c
-- WHERE f.carrier_id = c.cid
-- GROUP BY f.carrier_id
-- ORDER BY passengers DESC
-- LIMIT 10;

-- most outgoing flights cities
-- SELECT origin_city, COUNT(*)
-- FROM Flights
-- GROUP BY origin_city
-- ORDER BY COUNT(*) DESC
-- LIMIT 20;

-- cities w/ most incoming flights
SELECT dest_city, COUNT(*)
FROM Flights
GROUP BY dest_city
ORDER BY COUNT(*) DESC
LIMIT 20;