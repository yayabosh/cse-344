-- Q2.1
CREATE TABLE Edges (Source INT, Destination INT);
-- Q2.2
INSERT INTO Edges (Source, Destination)
VALUES
  (10, 5),
  (6, 25),
  (1, 3),
  (4, 4);
-- Q2.3
SELECT * FROM Edges;
-- Q2.4
SELECT Source FROM Edges;
-- Q2.5
SELECT *
FROM Edges
WHERE Source > Destination;
-- Q2.6
INSERT INTO Edges VALUES ('-1', '2000');

-- Q3
CREATE TABLE MyRestaurants
  (Name VARCHAR,
   FoodType VARCHAR,
   Distance INT,
   LastVisitDate VARCHAR,
   IsLiked BOOLEAN);
-- Q4
INSERT INTO MyRestaurants (Name, FoodType, Distance, LastVisitDate, IsLiked)
VALUES
  ('Local Point', 'Burgers', 5, '2021-12-15', 0),
  ('McDonalds', 'Burgers', 10, '2022-01-02', 1),
  ('Korean Tofu House', 'Korean', 12, '2021-12-10', 1),
  ('Chipotle', 'Mexican', 12, '2021-12-05', 1),
  ('Center Table', 'Pizza', 15, '2021-12-07', NULL);
-- Q5.1
.headers on
1. Comma-separated
Command: .mode csv
Query: SELECT * FROM MyRestaurants;

2. List format, "|" delimiter
Command: .mode list
Query: SELECT * FROM MyRestaurants;

3. Column format with column width >= 15
Command: 
.mode column
.width 15 15 15 15 15
Query: SELECT * FROM MyRestaurants;
-- Q5.2
.headers off
1. Comma-separated
Command: .mode csv
Query: SELECT * FROM MyRestaurants;

2. List format, "|" delimiter
Command: .mode list
Query: SELECT * FROM MyRestaurants;

3. Column format with column width >= 15
Command:
.mode column
.width 15 15 15 15 15
Query: SELECT * FROM MyRestaurants;
-- Q6
SELECT *
FROM MyRestaurants
WHERE IsLiked = 1 AND date(LastVisitDate) >= date('now', '-3 months');
-- Q7
SELECT *
FROM MyRestaurants
WHERE Distance <= 10;
-- Q8
SELECT Name, Distance
FROM MyRestaurants
WHERE Distance <= 20
ORDER BY Name ASC;
