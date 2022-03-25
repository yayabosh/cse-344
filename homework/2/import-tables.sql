-- Set input format to CSV
.mode csv

-- Import each CSV file into its corresponding table
.import carriers.csv Carriers
.import months.csv Months
.import weekdays.csv Weekdays
.import flights-small.csv Flights