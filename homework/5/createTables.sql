CREATE TABLE Users (
    username VARCHAR(20) PRIMARY KEY,
    salt VARBINARY(20),
    hash VARBINARY(20),
    balance INT
);

-- One User can have many Reservations
CREATE TABLE Reservations (
    rid INT PRIMARY KEY,
    paid INT,              -- boolean, 1 means paid
    canceled INT,          -- boolean, 1 means canceled
    fid1 INT NOT NULL REFERENCES Flights,
    fid2 INT REFERENCES Flights,
    username VARCHAR(20) REFERENCES Users
);
