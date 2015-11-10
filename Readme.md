# Csv Shredder Java Callout 

This directory contains the Java source code and pom.xml file required to
compile a Java callout for Apigee Edge.  The callout shreds a CSV, returns a Java map, and returns success.

## Building:

1. unpack (if you can read this, you've already done that).

2. configure the build on your machine by loading the Apigee jars into your local cache.   
  ```bash ./buildsetup.sh```

3. Build with maven.  
  ```mvn clean package```

4. The above will copy the generated JAR and its dependencies to the bundle directory.  Now deploy the API Proxy bundle with, Eg,    
   ```./pushapi -v -d -o demo28 -e prod -n csv-shredder bundle```

5. Use a client to load a CSV into the cache, via the proxy. Eg,   
   ```curl -i -X POST \ 
       -H content-type:text/csv \
       http://demo28-prod.apigee.net/csv-shredder/shred?name=sample \
       --data-binary @sample.csv```

6. Use a client to query from the cache, via the proxy. Eg,   
   ```curl -i -X GET \ 
       http://demo28-prod.apigee.net/csv-shredder/shred/sample/PRIMARY_KEY```



## Dependencies

- Apigee Edge expressions v1.0
- Apigee Edge message-flow v1.0
- Apache commons lang 2.6
- Apache commons csv 1.2
- FasterXML Jackson 2.5.0

These jars must be available on the classpath for the compile to
succeed. Using maven to build via the pom.xml file will download all of these files for
you, automatically. 

### If for some reason you want to download these dependencies manually: 

The first 2 jars are available in Apigee Edge. The first two are
produced by Apigee; contact Apigee support to obtain these jars to allow
the compile, or get them here: 
https://github.com/apigee/api-platform-samples/tree/master/doc-samples/java-cookbook/lib

The Apache commons lang jar is implicitly available in Apigee Edge at runtime, therefore in the pom.xml, the jar is marked with "compile" scope. You can download it from maven.org. The commons csv is not implicitly available in Edge; in the pom file, it is not marked as "compile" scope. 

The Jackson jar is similarly available from the public maven.org repo. Jackson is not marked with compile scope in the pom.xml. 


## Notes

The shredder produces a Java object of type Map<String,Map<String,String>>
and caches it. It uses the queryparam "name" to store the cached item.
For this demonstration, you can have as many different cached maps as you like, each accessible by name. 

The first row of the CSV is expected to be the header row, which defines the names of the fields in each row. 

For all subsequent rows, the first field in each row of the CSV is
treated as the primary key, in other words, the key for the map.  The remaining fields
are a map of "field name" => "value", where the field names are those
that are defined in the first row.  


### Simple Example 

For example, the super-simple.csv file has these contents:

```
   PK,field1,field2
   A,B,C
   D,E,F
   G,H,I
```

To shred that csv, use this command:

```
  curl -i -X POST \
    -H content-type:text/csv \
    http://demo28-prod.apigee.net/csv-shredder/shred?name=simple \
     --data-binary @super-simple.csv
```

Notice that the file is specified to curl with --data-binary. If you use -d or --data-ascii, curl will eliminate newlines, which will cause the CSV to be mangled before it is sent to the API Proxy. 


The cache will then hold a Map with 3 key/value pairs. it will look like this: 

```
  { 
     A => { field1 => B, field2 => C, PK => A },
     D => { field1 => E, field2 => F, PK => D },
     G => { field1 => H, field2 => I, PK => G }
  }
```

To query the map, you must specify the map name, and the "primary key", both of which are passed as url path elements. 

For example, 

```curl -i http://demo28-prod.apigee.net/csv-shredder/field/simple/A```

result: 
```
{
  "status": "ok",
  "data": {
    "field1" : "B",
    "field2" : "C",
    "PK" : "A"
  }
}
```

### Another example: 

To shred a CSV and load it into a Java Map, which then gets inserted into cache: 

shred: 
```
  curl -i -X POST \
    -H content-type:text/csv \
    http://demo28-prod.apigee.net/csv-shredder/shred?name=sacramento \
     --data-binary @Sacramento-RealEstate-Transactions.csv
```

(I got this sample CSV data for Sacramento real estate transactions from SpatialKey: 
  https://support.spatialkey.com/spatialkey-sample-csv-data/)

query: 

  ```curl -i "http://demo28-prod.apigee.net/csv-shredder/field/sacramento/51%20OMAHA%20CT"```

result:

```
{
  "status": "ok",
  "data": {
    "baths" : "1",
    "zip" : "95823",
    "beds" : "3",
    "price" : "68212",
    "street" : "51 OMAHA CT",
    "state" : "CA",
    "longitude" : "-121.431028",
    "latitude" : "38.478902",
    "type" : "Residential",
    "sqft" : "1167",
    "sale_date" : "Wed May 21 00:00:00 EDT 2008",
    "city" : "SACRAMENTO"
  }
}
```


## Bugs

There are no unit tests for this project.
