-- QUESTION A
PRAGMA FOREIGN_KEYS = ON;

CREATE TABLE InsuranceCo(name TEXT PRIMARY KEY, phone INT);

CREATE TABLE Person(ssn INT PRIMARY KEY, name TEXT);

-- One InsuranceCo can insure many Vehicles; one Vehicle is insured by one InsuranceCo
-- One Person can own many Vehicles; one Vehicle is owned by one Person
CREATE TABLE Vehicle(licensePlate TEXT PRIMARY KEY,
                     year INT,
                     maxLiability REAL,
                     insurance TEXT REFERENCES InsuranceCo,
                     ownerSSN INT REFERENCES Person);

-- A Driver is-a Person
CREATE TABLE Driver(ssn INT PRIMARY KEY REFERENCES Person, yearLicensed INT);

-- A ProfessionalDriver is-a Driver
CREATE TABLE ProfessionalDriver(ssn INT PRIMARY KEY REFERENCES Driver(ssn), medicalHistory TEXT);

-- A Truck is-a Vehicle
-- One ProfessionalDriver can operate many Trucks; one Truck can be operated by one ProfessionalDriver
CREATE TABLE Truck(licensePlate TEXT PRIMARY KEY REFERENCES Vehicle,
                   capacity INT,
                   operatorSSN INT REFERENCES ProfessionalDriver(ssn));

-- A NonProfessionalDriver is-a Driver (with no distinct attributes)
CREATE TABLE NonProfessionalDriver(ssn INT PRIMARY KEY REFERENCES Driver(ssn));

-- A Car is-a Vehicle
CREATE TABLE Car(licensePlate TEXT PRIMARY KEY REFERENCES Vehicle, make TEXT);

-- One NonProfessionalDriver can drive many Cars; one Car can be driven by Many NonProfessionalDrivers
CREATE TABLE Drives(driverSSN INT REFERENCES NonProfessionalDriver(ssn),
                    licensePlate TEXT REFERENCES Car(licensePlate),
                    PRIMARY KEY (driverSSN, licensePlate));


-- QUESTION B
-- The Vehicle table represents the insures relationship in the E/R diagram.
-- This is my representation because many Vehicles can be insured by one
-- InsuranceCo, but only one InsuranceCo can insure a given Vehicle. Therefore,
-- there is a many-to-one relation from Vehicle to InsuranceCo, so each Vehicle
-- must store a foreign key that represents its insurance company. Additionally,
-- each Vehicle has its own maxLiability, so it must store this as an attribute
-- as well. It would not make sense for InsuranceCo to store a maxLiability,
-- since there is no fixed maxLiability for an InsuranceCo; an InsuranceCo has
-- many maxLiabilities for each Vehicle it insures.


-- QUESTION C
-- The operates relation is many-to-one from Truck to ProfessionalDriver, meaning
-- one ProfessionalDriver can drive many Trucks, but one Truck can only be driven
-- by one ProfessionalDriver. Therefore, we can store the ProfessionalDriver's
-- SSN in the Truck table as a foreign key representing each Truck's
-- ProfessionalDriver.

-- On the other hand, the drives relation is many-to-many from Car to
-- NonProfessionalDriver, because one NonProfessionalDriver can drive many Cars
-- and one Car can be driven by many NonProfessionalDrivers. Therefore, I created
-- another table to represent this relationship which stores a foreign key to the
-- NonProfessionalDriver and a foreign key to the Car they drive, and a composite
-- key which is this tuple together to insure there are no duplicates added.
