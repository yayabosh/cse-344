# CSE 344 Winter 2022 Homework 6: Parallel Data Processing and Spark

**Objectives:**  To write distributed queries. To learn about Spark and running distributed data processing in the cloud using AWS.

**Assigned date:** Tuesday, February 22, 2022.

**Due date:** Monday, February 28, 2022 at 11:59pm.

**What to turn in:**

Your written transactions answers in the `cost_estimation.md` file. Add your Spark code to the single Java file, `SparkApp.java` in the `src` directory, along with the text outputs from AWS (output for QA in QA.txt, for QB in QB.txt, for QC in QC.txt). A skeleton `SparkApp.java` has been provided for you.

**Resources:**

- [Spark programming guide](https://spark.apache.org/docs/2.4.4/rdd-programming-guide.html)
- [Spark Javadoc](https://spark.apache.org/docs/2.4.4/api/java/index.html)
- [Amazon web services EMR (Elastic MapReduce) documentation](https://aws.amazon.com/documentation/emr/)
- [Amazon S3 documentation](https://aws.amazon.com/documentation/s3/)

## Cost Estimation

(15 points) Estimate the cost of the below physical plan under pipelined execution given the following statistics and indexes. You will not need all the statistics nor indexes. You may assume uniform distribution and independence of attribute values where applicable. We have computed some of the estimated cardinalities of intermediate results already (verify them for fun!). Include your work (equations) and your complete estimate.

| R(a)        |             |             |
|-------------|-------------|-------------|
| B(R) = 1000 | T(R) = 10^5 | Clustered index on R(a) |
| min(a, R) = 150 | max(a, R) = 250 |  |

| S(a, b, c)  |             |             |
|-------------|-------------|-------------|
| B(S) = 2000 | T(S) = 4*10^4 | Unclustered index on S(a) |
| min(a, S) = 0 | max(a, S) = 250 | Unclustered index on S(b) |
| V(b, S) =  1000 | V(c, S) =  10 | Clustered index on S(c) |

| U(b, d)     |             |             |
|-------------|-------------|-------------|
| B(U) = 500 | T(U) = 10^4 | Unclustered index on U(b) |
| V(b, U) =  250 | |
| min(d, U) = 1 | max(d, U) = 1000 | |

<img src="https://courses.cs.washington.edu/courses/cse414/20wi/hw6/costest.jpg" width="700"/>

For the written part of this assignment, put your answers in the `cost_estimation.md` file in [markdown format](https://www.markdownguide.org/basic-syntax).

## Spark Programming Assignment

In this homework, you will be writing Spark and Spark SQL code, to be executed both locally on your machine and also using Amazon Web Services via AWS Academy.

We will be using a similar flights dataset used in previous homeworks. This time, however, we will be using the *entire* data dump from the [US Bereau of Transportation Statistics](https://www.transtats.bts.gov/DL_SelectFields.asp?Table_ID=236&DB_Short_Name=On-Time), which consists of information about all domestic US flights from 1987 to 2011 or so. The data is in [Parquet](https://parquet.apache.org/) format. Your local runs/tests will use a subset of the data (in the `flights_small` directory) and your cloud jobs will use the full data.

Here is a rough outline on how to do the HW:

A. Accept the Invitation for AWS Academy from your email.

B. Complete the HW locally and make sure it runs with maven.

C. Run your solutions on AWS Elastic MapReduce one at a time when you are fairly confident with your solutions

### A. Join AWS Academy

1. There should be an invitation to join AWS Academy via Canvas. Please accept that ASAP.


Now that you have accepted the invite you should focus on working locally and getting it to work. Then in a later section we will link you to a PowerPoint with a detailed explanation on how to use AWS Academy to run your code with S3 and EMR.

### B. Get Code Working Locally

We have created empty method bodies for each of the questions below (QA, QB, and QC). *Do not change any of the method signatures*. You are free to define extra methods and classes if you need to. We have also provided a warmup method that shows fully-functional examples of three ways that the same query could be solved using Spark's different APIs.

There are many ways to write the code for this assignment. Here are some documentation links that we think would get you started up about what is available in the Spark functional APIs:
* [Spark 2.4.5 Manual](https://spark.apache.org/docs/2.4.5/)
* [Spark 2.4.5 Javadocs](https://spark.apache.org/docs/2.4.5/api/java/index.html)
* [Dataset](https://spark.apache.org/docs/2.4.5/api/java/org/apache/spark/sql/Dataset.html)
* [Row](https://spark.apache.org/docs/2.4.5/api/java/org/apache/spark/sql/Row.html) (see also RowFactory)
* [JavaRDD](https://spark.apache.org/docs/2.4.5/api/java/index.html?org/apache/spark/api/java/JavaRDD.html) (see also JavaPairRDD)
* [Tuple2](https://www.scala-lang.org/api/2.9.1/scala/Tuple2.html)

The quickstart documentation also more depth and examples of using [RDDs](https://spark.apache.org/docs/2.4.5/rdd-programming-guide.html) and [Datasets](https://spark.apache.org/docs/2.4.5/sql-getting-started.html).

For questions a, b, and c, you will get the points for writing a correct query.

(a) (15 points) Complete the method QA in SparkApp.java. Use the Spark functional APIs or SparkSQL. Select all flights that leave from 'Seattle, WA', and return the destination city names. Only return each destination city name once. Return the results in an RDD where the Row is a single column for the destination city name.

(b) (30 points) Complete the method QB in SparkApp.java. Only use the Spark functional APIs. Find the number of non-canceled (!= 1) flights per month-origin city pair. Return the results in an RDD where the row has three columns that are the origin city name, month, and count, in that order.

(c) (30 points) Complete the method QC in SparkApp.java.  Only use the Spark functional APIs. Compute the average delay from all departing flights for each city. Flights with NULL delay values should not be counted, and canceled flights should not be counted. Return the results in an RDD where the row has two columns that are the origin city name and average, in that order.

#### Testing Locally
We provide cardinality testing when you run

`$ mvn test`

You are responsible for verifying you have the correct format and contents in your results.

#### Running Local Jobs
To actually execute the main method, toggle the SparkSession initialization on lines 147 and 148 of SparkApp.java to allow it to run on locally (local SparkSession, not cluster). Run from the top level
 directory (with pom.xml in it):

```
$ mvn clean compile assembly:single
$ java -jar target/sparkapp-1.0-jar-with-dependencies.jar flights_small output/
```

Note that, on Windows, this should be executed in the root directory of this repo, so that the program can find `bin/winutils.exe`. (The code uses the directory of execution `.`; you can change this in the `createLocalSession()` method if you must.) For reference, `winutils.exe` was obtained for this Hadoop version from a [Github repo](https://github.com/cdarlint/winutils).

For this quarter, we added code to add compatibility with Java 9+. It should work fine. In case there is a problem, you can force a Java 8 execution by downloading a Java 8 JRE and setting your JAVA_HOME variable to your Java 8 runtime. We have also tested it with Java 11 and it seems to work with that as well.

#### Interpreting the Output After Running Locally
At this point you should have run these commands:
```
$ mvn clean compile assembly:single
$ java -jar target/sparkapp-1.0-jar-with-dependencies.jar flights_small output/
```
and gotten output in the following folders:
1. ```outputwarmup```
2. ```outputQA```
3. ```outputQB```
4. ```outputQC```

Within each folder, there should be 2 files: ```_SUCCESS ``` and ```part-00000 ```. Your output will be stored in the ```part-00000 ``` file so if ```mvn test``` isn't working you can look at your local output from there and start debugging.


### C. Run Code on Elastic Map Reduce (EMR)
(10 points)

Follow this [Powerpoint](https://docs.google.com/presentation/d/1MJMuKViDU_8js9SEfWeHMg-XqP5Mipz0uxvQ51MiHg0/edit?usp=sharing) to use AWS Academy for this assignment. Please note that you will only be able to view the powerpoint if you are logged into your CSE email.

Note for submission:
You want to copy paste the outputs of the EMR jobs for the 3 queries in the following files: `QA.txt`, `QB.txt` and `QC.txt`.

## Submission Instructions

Turn in your `cost_estimation.md`, `QA.txt`, `QB.txt`, `QC.txt`, `SparkApp.java` to gradescope for full points.
