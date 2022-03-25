-- Create Carriers
CREATE TABLE Carriers (cid varchar(7) PRIMARY KEY, name varchar(83));

-- Create Months
CREATE TABLE Months (mid int PRIMARY KEY, month varchar(9));

-- Create Weekdays
CREATE TABLE Weekdays (did int PRIMARY KEY, day_of_week varchar(9));

-- Create Flights
CREATE TABLE 
Flights (fid int PRIMARY KEY,
         month_id int,        -- 1-indexed (ie, 1-12)
         day_of_month int,    -- 1-indexed (ie, 1-31)
         day_of_week_id int,  -- 1-indexed (ie, 1-7). 1 = Monday, 2 = Tuesday, etc
         carrier_id varchar(7),
         flight_num int,
         origin_city varchar(34),
         origin_state varchar(47),
         dest_city varchar(34),
         dest_state varchar(46),
         departure_delay int, -- in mins
         taxi_out int,        -- in mins
         arrival_delay int,   -- in mins
         canceled int,        -- boolean; 1 means canceled
         actual_time int,     -- flight duration, in mins
         distance int,        -- in miles
         capacity int,
         price int,            -- in USD (ie, dollars)
         FOREIGN KEY (month_id) REFERENCES Months(mid),
         FOREIGN KEY (day_of_week_id) REFERENCES Weekdays(did),
         FOREIGN KEY (carrier_id) REFERENCES Carriers(cid)
        );