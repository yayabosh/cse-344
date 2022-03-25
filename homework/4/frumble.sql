-- QUESTION 1
CREATE TABLE Sales(name TEXT,
                   discount INT,  -- percentage
                   month TEXT,    -- apr, aug, dec, etc.
                   price INT);


-- QUESTION 2
-- If the output is empty, the functional dependency must hold
-- because each query outputs rows that do NOT hold under the 
-- functional dependency; if no rows are printed, the FD is true
-- for the relation.

-- name -> price
SELECT name, COUNT(DISTINCT price) AS count
FROM Sales
GROUP BY name
HAVING count != 1;

-- month -> discount
SELECT month, COUNT(DISTINCT discount) AS count
FROM Sales
GROUP BY month
HAVING count != 1;

-- does not exist: price -> discount
SELECT price, COUNT(DISTINCT discount) AS count
FROM Sales
GROUP BY price
HAVING count != 1;

-- does not exist: discount, price -> month
SELECT discount, price, COUNT(DISTINCT month) AS count
FROM Sales
GROUP BY discount, price
HAVING count != 1;


-- QUESTION 3
-- Let Sales = R(name, discount, month, price)
-- FDs: name -> price, month -> discount

-- Decompose R with name -> price:
-- R1(name, price)           - in BCNF
-- R2(discount, month, name) - not in BCNF

-- Decompose R2 with month -> discount:
-- R3(month, discount)  - in BCNF
-- R4(name, month)      - in BCNF

-- Final relations: R1(name (key), price), R3(month (key), discount), R4(name, month)

PRAGMA FOREIGN_KEYS = ON;

CREATE TABLE Price(name TEXT PRIMARY KEY, price INT);

CREATE TABLE Discount(month TEXT PRIMARY KEY, discount INT);

CREATE TABLE NameAndMonth(name TEXT REFERENCES Price,
                          month TEXT REFERENCES Discount,
                          PRIMARY KEY (name, month));


-- QUESTION 4
-- 36 rows
INSERT INTO Price (name, price)
SELECT DISTINCT name, price
FROM Sales;

-- 12 rows
INSERT INTO Discount (month, discount)
SELECT DISTINCT month, discount
FROM Sales;

-- 426 rows
INSERT INTO NameAndMonth (name, month)
SELECT DISTINCT name, month
FROM Sales;
