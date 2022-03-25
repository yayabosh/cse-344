-- 109 rows
-- 4 seconds
-- Aberdeen SD
-- Abilene TX
-- Alpena MI
-- Ashland WV
-- Augusta GA
-- Barrow AK
-- Beaumont/Port Arthur TX
-- Bemidji MN
-- Bethel AK
-- Binghamton NY
-- Brainerd MN
-- Bristol/Johnson City/Kingsport TN
-- Butte MT
-- Carlsbad CA
-- Casper WY
-- Cedar City UT
-- Chico CA
-- College Station/Bryan TX
-- Columbia MO
-- Columbus GA
SELECT DISTINCT origin_city AS city
FROM Flights
WHERE canceled != 1
GROUP BY origin_city
HAVING MAX(actual_time) < 180
ORDER BY city;