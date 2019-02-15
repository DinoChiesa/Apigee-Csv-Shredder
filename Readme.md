# Csv Shredder Java Callout

This directory contains the Java source code and pom.xml file required to
compile a pair of Java callouts for Apigee Edge.  The first callout shreds a CSV, returns a Java map object, and returns success.  The second callout retrieves items from a Java Map object, by key.

These callouts can work together, demonstrating:

1. how to parse a CSV within Apigee Edge

2. how to store a Java object in the Edge cache

3. How to retrieve a Java object from Cache, and then parse the object on subsequent API calls


## Disclaimer

This example is not an official Google product, nor is it part of an official Google product.


## Using this policy

You do not need to build the source code in order to use the policy in Apigee Edge.
All you need is the built JAR, and the appropriate configuration for the policy.
If you want to build it, feel free.  The instructions are at the bottom of this readme.


1. create a cache in the Apigee Edge environment called 'csv-cache'.  This is used by the
demonstration apiproxy.  You can use the Admin UI to do so.

2. Now deploy the API Proxy bundle with your favorite tool, for example [importAndDeploy.js](https://github.com/DinoChiesa/apigee-edge-js/blob/master/examples/importAndDeploy.js)
   ```node importAndDeploy.js -v -o $ORG -e $ENV -d bundle```

3. Use a client to load a CSV into the cache, via the proxy. Eg,
   ```curl -i -X POST
       -H content-type:text/csv
       https://$ORG-$ENV.apigee.net/csv-shredder/shred?name=sample
       --data-binary @sample.csv```

4. Use a client to query from the cache, via the proxy. Eg,
   ```curl -i -X GET
       https://$ORG-$ENV.apigee.net/csv-shredder/shred/sample/PRIMARY_KEY```



## Dependencies

Maven will resolve all the dependencies during the build / compile phase. 
The jars that are dependencies must be available  as resources for the proxy at runtime.
The maven pom file should copy those files to the right place, automatically.

## Notes

1. The shredder produces a Java object of type Map<String,Map<String,String>>
   and caches it. It uses the queryparam "name" to store the cached item.
   For this demonstration, you can have as many different cached maps as you like, each accessible by name.

2. The first row of the CSV is expected to be the header row, which defines the names of the fields in each row.

3. For all subsequent rows, the first field in each row of the CSV is
   treated as the primary key, in other words, the key for the map.  The remaining fields
   are a map of "field name" => "value", where the field names are those
   that are defined in the first row.


## Simple Example

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
    https://$ORG-$ENV.apigee.net/csv-shredder/shred?name=simple \
     --data-binary @csv/super-simple.csv
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

```curl -i https://$ORG-$ENV.apigee.net/csv-shredder/field/simple/A```

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

## Another example

To shred a CSV and load it into a Java Map, which then gets inserted into cache:

shred:
```
  curl -i -X POST \
    -H content-type:text/csv \
    https://$ORG-$ENV.apigee.net/csv-shredder/shred?name=sacramento \
     --data-binary @csv/Sacramento-RealEstate-Transactions.csv
```

(I got this sample CSV data for Sacramento real estate transactions from SpatialKey:
  https://support.spatialkey.com/spatialkey-sample-csv-data/)

query:

```curl -i "https://$ORG-$ENV.apigee.net/csv-shredder/field/sacramento/51%20OMAHA%20CT"```

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


## Building

1. unpack (if you can read this, you've already done that).

2. configure the build on your machine by loading the Apigee jars into your local cache.
  ```bash ./buildsetup.sh```

3. Build with maven.
  ```mvn clean package```

  The above will copy the generated JAR and its dependencies to the bundle directory.


## LICENSE

This material is copyright 2016 Apigee Corp, 2019 Google LLC.
This is licensed under the Apache 2.0 license. See the [LICENSE](LICENSE) file.


## Bugs

There are no unit tests for this project.
