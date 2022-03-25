package edu.uw.cs;

import org.junit.*;
import static org.junit.Assert.*;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.sql.Row;
import org.apache.log4j.*;


/**
 * Unit test for simple App.
 */
public class SparkAppTest {

    private static final String SMALL_DATA = "flights_small";

    @BeforeClass
    public static void quietLogging() {
        // Turn off logging except for error messages
        Logger.getLogger("org").setLevel(Level.ERROR);
    }

    @Test
    public void warmupTest() {
        SparkApp.warmup(SparkApp.createLocalSession(), SMALL_DATA);
    }

    @Test
    public void Q3ATest() {
        JavaRDD<Row> actual = SparkApp.QA(SparkApp.createLocalSession(), SMALL_DATA);
        assertEquals("Cardinality doesn't match!", 50, actual.count());
    }

    @Test
    public void Q3BTest() {
        JavaRDD<Row> actual = SparkApp.QB(SparkApp.createLocalSession(), SMALL_DATA);
        assertEquals("Cardinality doesn't match!", 281, actual.count());
    }

    @Test
    public void Q3CTest() {
        JavaRDD<Row> actual = SparkApp.QC(SparkApp.createLocalSession(), SMALL_DATA);
        assertEquals("Cardinality doesn't match!", 281, actual.count());
    }

}
