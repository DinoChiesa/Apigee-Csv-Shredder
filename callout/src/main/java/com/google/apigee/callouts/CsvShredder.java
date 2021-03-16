// CsvShredder.java
//
// This is the source code for a Java callout for Apigee.  This
// callout is very simple - it shreds a CSV, then sets a Java Map into a
// context variable, and then returns SUCCESS.
//
// Copyright 2016 Apigee Corp, 2017-2021 Google LLC.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     https://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//
// ------------------------------------------------------------------

package com.google.apigee.callouts;

import com.apigee.flow.execution.ExecutionResult;
import com.apigee.flow.execution.spi.Execution;
import com.apigee.flow.message.Message;
import com.apigee.flow.message.MessageContext;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;

public class CsvShredder extends CalloutBase implements Execution {
  public CsvShredder(Map properties) {
    super(properties);
  }

  public String getVarnamePrefix() {
    return "csv";
  }

  private List<String> getFieldList(MessageContext msgCtxt) throws IllegalStateException {
    String fieldlist = (String) this.properties.get("fieldlist");
    if (fieldlist == null || fieldlist.equals("")) {
      return null; // assume the first row gives the field list
    }
    fieldlist = resolvePropertyValue(fieldlist, msgCtxt);
    if (fieldlist == null || fieldlist.equals("")) {
      return null; // assume the first row gives the field list
    }

    // now split by commas (spaces are ok)
    String[] parts = fieldlist.split(" *, *");
    List<String> list = new ArrayList<String>();
    for (int i = 0; i < parts.length; i++) {
      list.add(parts[i].trim());
    }
    return list;
  }

  public ExecutionResult execute0(final MessageContext msgCtxt) throws Exception {
    Message msg = msgCtxt.getMessage();
    List<String> list = getFieldList(msgCtxt);

    // 1. we want to read the content as a stream
    InputStreamReader in = new InputStreamReader(msg.getContentAsStream());

    // see info for handling header records here:
    // https://commons.apache.org/proper/commons-csv/apidocs/org/apache/commons/csv/CSVFormat.html

    // 2. read the CSV, maybe treat the first line as a header
    Map<String, Object> map = new HashMap<String, Object>();
    Iterable<CSVRecord> records = null;
    if (list == null) {
      records = CSVFormat.DEFAULT.withHeader().parse(in);
    } else {
      records = CSVFormat.DEFAULT.withHeader(list.toArray(new String[1])).parse(in);
    }

    // 3. process each record in the CSV, convert to an element in a map
    for (CSVRecord record : records) {
      // assume first field is the primary key
      String firstField = record.get(0);
      map.put(firstField, record.toMap()); // Map<String,String>
    }

    // 4. set a variable to hold the generated Map<String, Map>
    msgCtxt.setVariable(varName("result_java"), map);

    // 5. for diagnostic purposes, serialize to JSON as well
    String jsonResult = om.writer().withDefaultPrettyPrinter().writeValueAsString(map);
    msgCtxt.setVariable(varName("result_json"), jsonResult);

    return ExecutionResult.SUCCESS;
  }
}
