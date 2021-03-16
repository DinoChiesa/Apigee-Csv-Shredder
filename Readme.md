# Csv Shredder Java Callout

This directory contains the Java source code and pom.xml file required to
compile a set of Java callouts for Apigee, that involve parsing CSVs, and handling the output of that data.

| callout        | description                                                                                         |
| -------------- | --------------------------------------------------------------------------------------------------- |
| `CsvShredder`  | parses a CSV, creates a Java map object from that data, and stores the map into a context variable. It also stores a JSON version of that data into a different context variable. |
| `MapExtractor` | retrieves items from a Java Map object, by key.                                                     |


These callouts can work together or independently.

For example, you could use `CsvShredder` to parse a CSV within Apigee, then use
`PopulateCache` to store the resulting Java Map object in the Apigee cache.  Then `LookupCache`
to retrieve a Java object from Cache, and then `MapExtractor` to query the
cached object on subsequent API calls.


As another example, you could use `CsvShredder` to parse a CSV within Apigee, then just emit the resulting JSON to the response.


## Disclaimer

This example is not an official Google product, nor is it part of an official Google product.


## Two classes

* *com.google.apigee.callouts.CsvShredder* - reads the ambient request or response message,
  parse the CSV, serialize as a Java map, and also as a json object.

* *com.google.apigee.callouts.MapExtractor* - extract a value from a context variable that contains a Java map.

See the example bundle for configuration.



## Using these callouts

You do not need to build the source code in order to use the callouts in Apigee.
All you need is the built JAR, and the appropriate configuration for the callouts.
If you want to build it, feel free. The instructions are at the bottom of this readme.


1. create a cache called 'csv-cache' in the Apigee environment.  This is used by the
demonstration apiproxy.  You can use the Admin UI to do so.

2. Now deploy the API Proxy bundle with your favorite tool, for example [importAndDeploy.js](https://github.com/DinoChiesa/apigee-edge-js-examples/blob/main/importAndDeploy.js)
   ```
   # for Apigee Edge SaaS or Apigee X
   node importAndDeploy.js -v -o $ORG -e $ENV -d bundle
   ```

3. Use a client to load a CSV into the cache, via the proxy. Eg,
   ```
   # for Apigee X
   endpoint=https://whatever-your-endpoint-is

   curl -i -X POST \
       -H content-type:text/csv \
       $endpoint/csv-shredder/shred?name=sample \
       --data-binary @sample.csv
   ```

4. Use a client to query from the cache, via the proxy. Eg,
   ```
   curl -i -X GET \
       $endpoint/csv-shredder/shred/sample/PRIMARY_KEY
   ```


## Dependencies

Maven will resolve all the dependencies during the build / compile phase.
The jars that are dependencies must be available  as resources for the proxy at runtime.
The maven pom file should copy those files to the right place, automatically.

## Notes

1. The shredder produces a Java object of type `Map<String,Map<String,String>>`
   and caches it. It uses the queryparam "name" to store the cached item.
   For this demonstration, you can have as many different cached maps as you like, each accessible by name.

2. The first row of the CSV is expected to be the header row, which defines the names of the fields in each row.

3. For all subsequent rows, the first field in each row of the CSV is
   treated as the primary key, in other words, the key for the map. The remaining fields
   are a map of "field name" => "value", where the field names are those
   that are defined in the first row.


## Example 1: Simple CSV

For example, the `super-simple.csv` file has these contents:

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
    $endpoint/csv-shredder/shred?name=simple \
     --data-binary @csv/super-simple.csv
```

Notice that the file is specified to curl with `--data-binary`. If you use `-d`
or `--data-ascii`, curl will eliminate newlines, which will cause the CSV to be
mangled before it is sent to the API Proxy.


The cache will then hold a `Map` with 3 key/value pairs. it will look like this:

```
  {
     A => { field1 => B, field2 => C, PK => A },
     D => { field1 => E, field2 => F, PK => D },
     G => { field1 => H, field2 => I, PK => G }
  }
```

To query the map, you must specify the map name, and the value of the "primary key", both of which are passed as url path elements.

For example,

```
curl -i https://$ORG-$ENV.apigee.net/csv-shredder/field/simple/A
```

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

## Example 2: Sacramento Real Estate Transactions

I got this sample CSV data for Sacramento real estate transactions from SpatialKey:
  https://support.spatialkey.com/spatialkey-sample-csv-data/

To shred this more complicated CSV and load it into a Java Map, which then gets inserted into cache:

shred:
```
  curl -i -X POST \
    -H content-type:text/csv \
    $endpoint/csv-shredder/shred?name=sacramento \
     --data-binary @csv/Sacramento-RealEstate-Transactions.csv
```


query:

```
curl -i "https://$ORG-$ENV.apigee.net/csv-shredder/field/sacramento/51%20OMAHA%20CT"
```

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


## Example 3: Converting CSV to JSON

This example just sends in a CSV, and gets back an equivalent JSON in response.

```
  curl -i -X POST \
    -H content-type:text/csv \
    $endpoint/csv-shredder/tojson \
     --data-binary @csv/Sacramento-RealEstate-Transactions.csv
```



## Building

1. unpack (if you can read this, you've already done that).

2. configure the build on your machine by loading the Apigee jars into your local cache.
   ```
   bash ./buildsetup.sh
   ```

3. Build with maven.
   ```
   mvn clean package
   ```

  The above will copy the generated JAR and its dependencies to the bundle directory.


## LICENSE

This material is Copyright 2016 Apigee Corp, 2019-2021 Google LLC.
This is licensed under the Apache 2.0 license. See the [LICENSE](LICENSE) file.


## Bugs

There are no unit tests for this project.
