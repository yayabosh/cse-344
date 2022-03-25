package edu.uw.cs;

import org.apache.log4j.*;
import org.apache.parquet.filter2.predicate.Operators.Column;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.RowFactory;
import scala.Tuple2;

/**
 * SparkApp example code and problem specifications.
 */
public class SparkApp {

    // All tuple indexes that you might need to access
    public static final int MONTH = 2;
    public static final int ORIGIN_CITY_NAME = 15;
    public static final int DEST_CITY_NAME = 24;
    public static final int DEP_DELAY = 32;
    public static final int CANCELLED = 47;


  /* We list all the fields in the input data file for your reference root
 |-- year: integer (nullable = true)   // index 0
 |-- quarter: integer (nullable = true)
 |-- month: integer (nullable = true)
 |-- dayofmonth: integer (nullable = true)
 |-- dayofweek: integer (nullable = true)
 |-- flightdate: string (nullable = true)
 |-- uniquecarrier: string (nullable = true)
 |-- airlineid: integer (nullable = true)
 |-- carrier: string (nullable = true)
 |-- tailnum: string (nullable = true)
 |-- flightnum: integer (nullable = true)
 |-- originairportid: integer (nullable = true)
 |-- originairportseqid: integer (nullable = true)
 |-- origincitymarketid: integer (nullable = true)
 |-- origin: string (nullable = true)   // airport short name
 |-- origincityname: string (nullable = true) // e.g., Seattle, WA
 |-- originstate: string (nullable = true)
 |-- originstatefips: integer (nullable = true)
 |-- originstatename: string (nullable = true)
 |-- originwac: integer (nullable = true)
 |-- destairportid: integer (nullable = true)
 |-- destairportseqid: integer (nullable = true)
 |-- destcitymarketid: integer (nullable = true)
 |-- dest: string (nullable = true)
 |-- destcityname: string (nullable = true)
 |-- deststate: string (nullable = true)
 |-- deststatefips: integer (nullable = true)
 |-- deststatename: string (nullable = true)
 |-- destwac: integer (nullable = true)
 |-- crsdeptime: integer (nullable = true)
 |-- deptime: integer (nullable = true)
 |-- depdelay: integer (nullable = true)
 |-- depdelayminutes: integer (nullable = true)
 |-- depdel15: integer (nullable = true)
 |-- departuredelaygroups: integer (nullable = true)
 |-- deptimeblk: integer (nullable = true)
 |-- taxiout: integer (nullable = true)
 |-- wheelsoff: integer (nullable = true)
 |-- wheelson: integer (nullable = true)
 |-- taxiin: integer (nullable = true)
 |-- crsarrtime: integer (nullable = true)
 |-- arrtime: integer (nullable = true)
 |-- arrdelay: integer (nullable = true)
 |-- arrdelayminutes: integer (nullable = true)
 |-- arrdel15: integer (nullable = true)
 |-- arrivaldelaygroups: integer (nullable = true)
 |-- arrtimeblk: string (nullable = true)
 |-- cancelled: integer (nullable = true)
 |-- cancellationcode: integer (nullable = true)
 |-- diverted: integer (nullable = true)
 |-- crselapsedtime: integer (nullable = true)
 |-- actualelapsedtime: integer (nullable = true)
 |-- airtime: integer (nullable = true)
 |-- flights: integer (nullable = true)
 |-- distance: integer (nullable = true)
 |-- distancegroup: integer (nullable = true)
 |-- carrierdelay: integer (nullable = true)
 |-- weatherdelay: integer (nullable = true)
 |-- nasdelay: integer (nullable = true)
 |-- securitydelay: integer (nullable = true)
 |-- lateaircraftdelay: integer (nullable = true)
 |-- firstdeptime: integer (nullable = true)
 |-- totaladdgtime: integer (nullable = true)
 |-- longestaddgtime: integer (nullable = true)
 |-- divairportlandings: integer (nullable = true)
 |-- divreacheddest: integer (nullable = true)
 |-- divactualelapsedtime: integer (nullable = true)
 |-- divarrdelay: integer (nullable = true)
 |-- divdistance: integer (nullable = true)
 |-- div1airport: integer (nullable = true)
 |-- div1airportid: integer (nullable = true)
 |-- div1airportseqid: integer (nullable = true)
 |-- div1wheelson: integer (nullable = true)
 |-- div1totalgtime: integer (nullable = true)
 |-- div1longestgtime: integer (nullable = true)
 |-- div1wheelsoff: integer (nullable = true)
 |-- div1tailnum: integer (nullable = true)
 |-- div2airport: integer (nullable = true)
 |-- div2airportid: integer (nullable = true)
 |-- div2airportseqid: integer (nullable = true)
 |-- div2wheelson: integer (nullable = true)
 |-- div2totalgtime: integer (nullable = true)
 |-- div2longestgtime: integer (nullable = true)
 |-- div2wheelsoff: integer (nullable = true)
 |-- div2tailnum: integer (nullable = true)
 |-- div3airport: integer (nullable = true)
 |-- div3airportid: integer (nullable = true)
 |-- div3airportseqid: integer (nullable = true)
 |-- div3wheelson: integer (nullable = true)
 |-- div3totalgtime: integer (nullable = true)
 |-- div3longestgtime: integer (nullable = true)
 |-- div3wheelsoff: integer (nullable = true)
 |-- div3tailnum: integer (nullable = true)
 |-- div4airport: integer (nullable = true)
 |-- div4airportid: integer (nullable = true)
 |-- div4airportseqid: integer (nullable = true)
 |-- div4wheelson: integer (nullable = true)
 |-- div4totalgtime: integer (nullable = true)
 |-- div4longestgtime: integer (nullable = true)
 |-- div4wheelsoff: integer (nullable = true)
 |-- div4tailnum: integer (nullable = true)
 |-- div5airport: integer (nullable = true)
 |-- div5airportid: integer (nullable = true)
 |-- div5airportseqid: integer (nullable = true)
 |-- div5wheelson: integer (nullable = true)
 |-- div5totalgtime: integer (nullable = true)
 |-- div5longestgtime: integer (nullable = true)
 |-- div5wheelsoff: integer (nullable = true)
 |-- div5tailnum: integer (nullable = true)
   */

    public static void main(String[] args) {
        // Take a data file location and output file destination as inputs
        if (args.length < 2) {
            throw new RuntimeException("Usage: SparkApp <data file location> <output file destination>");
        }
        String dataFile = args[0];
        String output = args[1];

        // Turn off logging except for error messages
        Logger.getLogger("org").setLevel(Level.ERROR);

        // ************************************************************
        // IMPORTANT - Change this comment before running on a cluster:
        SparkSession spark = createLocalSession();
        // SparkSession spark = createClusterSession();
        // ************************************************************

        Dataset<Row> r = warmup(spark, dataFile);
        r.javaRDD().repartition(1).saveAsTextFile(output + "warmup");

        JavaRDD<Row> rA = QA(spark, dataFile);
        rA.repartition(1).saveAsTextFile(output + "QA");

        JavaRDD<Row> rB = QB(spark, dataFile);
        rB.repartition(1).saveAsTextFile(output + "QB");

        JavaRDD<Row> rC = QC(spark, dataFile);
        rC.repartition(1).saveAsTextFile(output + "QC");

    }

    /**
     * Create a SparkSession for running code locally
     */
    public static SparkSession createLocalSession() {
        String absolutePath = new java.io.File(".").getAbsolutePath();
        System.out.println("Setting Hadoop home directory (for bin/winutils.exe) to: "+absolutePath);
        System.setProperty("hadoop.home.dir", absolutePath);
        return SparkSession.builder().appName("SparkApp").config("spark.master", "local").getOrCreate();
    }

    /**
     * Create a SparkSession for running code on a cluster
     */
    public static SparkSession createClusterSession() {
        return SparkSession.builder().appName("SparkApp").getOrCreate();
    }

    /**
     * A set of completed examples for you to look at and test on EMR
     */
    public static Dataset<Row> warmup(SparkSession spark, String dataFile) {
        // Read the flights data to a Dataset and a JavaRDD
        Dataset<Row> d = spark.read().parquet(dataFile);
        JavaRDD<Row> rdd = d.javaRDD();

        // Option 1:
        // Use the Dataset SparkSQL interface
        d.createOrReplaceTempView("flights");
        Dataset<Row> r1 = spark.sql("SELECT DISTINCT destcityname FROM flights ORDER BY destcityname");
        // r1.show();

        // Option 2:
        // Use the Dataset functional API interface
        Dataset<Row> r2 = d.select(d.col("destcityname")).distinct().orderBy(d.col("destcityname"));
        // r2.show();

        // Option 3:
        // Use the JavaRDD functional API interface
        JavaRDD<Row> r3 = rdd.map(t -> RowFactory.create(t.getString(DEST_CITY_NAME))).distinct()
                .sortBy(t -> t.getString(0), true, 1);
        // r3.foreach(cityString -> System.out.println(cityString));

        return r1;
    }

    /**
     * Select all flights that leave from 'Seattle, WA', and return the destination
     * city names. Only return each destination city name once. Return the results
     * in a RDD where the Row is a single column for the destination city name.
     * 
     * Use the Spark functional APIs or SparkSQL.
     * 
     * (5 points, 50 rows from local data, 79 rows from full data)
     */
    public static JavaRDD<Row> QA(SparkSession spark, String dataFile) {
        Dataset<Row> d = spark.read().parquet(dataFile);

        // Use the Dataset SparkSQL interface
        d.createOrReplaceTempView("flights");

        return spark.sql("SELECT DISTINCT destcityname FROM flights WHERE origincityname = 'Seattle, WA'")
                    .javaRDD();
    }

    /**
     * Find the number of non-cancelled (!= 1) flights per month-origin city pair,
     * return the results in an RDD where the Row has three columns that are the
     * origin city name, month, and count, in that order.
     * 
     * Only use the Spark functional APIs
     * 
     * (10 points, 281 rows from local data, 4383 rows from full data)
     */
    public static JavaRDD<Row> QB(SparkSession spark, String dataFile) {
        Dataset<Row> d = spark.read().parquet(dataFile);

        // Use the Dataset functional API interface
        d.createOrReplaceTempView("flights");

        return d.filter("cancelled != 1")                          // only keep non-cancelled flights
                .groupBy(d.col("origincityname"), d.col("month"))  // group by origin city and month
                .count()                                           // count number of flights
                .javaRDD();
    }

    /**
     * Compute the average delay from all departing flights for each city. Flights
     * with NULL delay values should not be counted. Return the results in an RDD
     * where the Row has two columns that are the origin city name and average, in
     * that order.
     * 
     * Only use the Spark functional APIs
     * 
     * (10 points, 281 rows from local data, 383 rows from full data)
     */
    public static JavaRDD<Row> QC(SparkSession spark, String dataFile) {
        Dataset<Row> d = spark.read().parquet(dataFile);

        // Use the Dataset functional API interface
        d.createOrReplaceTempView("flights");

        return d.filter(d.col("depdelayminutes").isNotNull())  // only keep non-null values
                .groupBy(d.col("origincityname"))              // group by origin city
                .avg("depdelayminutes")                        // calculate average departure delay
                .javaRDD();
    }

}
