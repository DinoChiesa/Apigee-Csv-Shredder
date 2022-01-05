// CsvShredder.java
//
// This is the source code for a Java callout for Apigee.  This
// callout is very simple - it shreds a CSV, then sets a Java Map into a
// context variable, and then returns SUCCESS.
//
// Copyright 2016 Apigee Corp, 2017-2022 Google LLC.
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

  private Boolean getTrimSpaces(MessageContext msgCtxt) throws Exception {
    return _getBooleanProperty(msgCtxt, "trim-spaces", false);
  }

  private Boolean getContrivePrimaryKey(MessageContext msgCtxt) throws Exception {
    return _getBooleanProperty(msgCtxt, "contrive-primary-key", false);
  }

  static enum OutputFormat {
    MAP,
    LIST,
    NOTSPECIFIED
  }

  private OutputFormat getOutputFormat(MessageContext msgCtxt) throws Exception {
    String value = (String) this.properties.get("output-format");
    if (value == null || value.equals("")) {
      return OutputFormat.MAP;
    }
    value = resolveVariableReferences(value, msgCtxt);
    if (value == null || value.equals("")) {
      return OutputFormat.MAP;
    }

    if (value.equalsIgnoreCase("list")) {
      return OutputFormat.LIST;
    }
    if (value.equalsIgnoreCase("map")) {
      return OutputFormat.MAP;
    }
    throw new IllegalArgumentException("output-format");
  }

  private List<String> getFieldList(MessageContext msgCtxt) throws IllegalStateException {
    String fieldlist = (String) this.properties.get("fieldlist");
    if (fieldlist == null || fieldlist.equals("")) {
      return null; // assume the first row gives the field list
    }
    fieldlist = resolveVariableReferences(fieldlist, msgCtxt);
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

  private static String padLeft(String s, int length, char c) {
    int L = s.length();
    if (L >= length) {
      return s;
    }
    StringBuilder sb = new StringBuilder();
    while (sb.length() < length - L) {
      sb.append(c);
    }
    sb.append(s);
    return sb.toString();
  }

  CSVFormat getCsvReader(List<String> fieldList, boolean trimSpaces) {
    CSVFormat.Builder builder = CSVFormat.Builder.create();
    if (trimSpaces) {
      builder.setIgnoreSurroundingSpaces(true);
    }

    if (fieldList == null) {
      builder.setHeader();
    } else {
      builder.setHeader(fieldList.toArray(new String[1]));
    }
    return builder.build();
  }

  public ExecutionResult execute0(final MessageContext msgCtxt) throws Exception {
    Message msg = msgCtxt.getMessage();
    List<String> fieldList = getFieldList(msgCtxt);

    // 1. we want to read the content as a stream
    InputStreamReader in = new InputStreamReader(msg.getContentAsStream());

    // see info for handling header records here:
    // https://commons.apache.org/proper/commons-csv/apidocs/org/apache/commons/csv/CSVFormat.html

    // 2. read the CSV, maybe treat the first line as a header
    Iterable<CSVRecord> records = getCsvReader(fieldList, getTrimSpaces(msgCtxt)).parse(in);

    OutputFormat desiredOutputFormat = getOutputFormat(msgCtxt);
    if (desiredOutputFormat == OutputFormat.LIST) {
      // 3. convert the Iterable to a List
      List<Map<String, String>> list = new ArrayList<Map<String, String>>();
      for (CSVRecord record : records) {
        list.add(record.toMap()); // Map<String,String>
      }

      msgCtxt.setVariable(varName("result_format"), "list");

      // 4a. set a variable to hold the generated List<Map>
      msgCtxt.setVariable(varName("result_java"), list);

      // 4b. set a variable to hold the number of rows read
      msgCtxt.setVariable(varName("rows_read"), String.format("%d", list.size()));
      // 5. for diagnostic purposes, serialize to JSON as well
      String jsonResult = om.writer().withDefaultPrettyPrinter().writeValueAsString(list);
      msgCtxt.setVariable(varName("result_json"), jsonResult);
    } else if (desiredOutputFormat == OutputFormat.MAP) {
      // 3. process each record in the CSV, convert to an element in a map
      Map<String, Object> map = new HashMap<String, Object>();
      Boolean contrivePk = getContrivePrimaryKey(msgCtxt);
      int c = 0;
      for (CSVRecord record : records) {
        String primaryKey =
            contrivePk
                ? padLeft(String.format("%d", c), 10, '0')
                : record.get(0); // by default, first field is PK
        map.put(primaryKey, record.toMap()); // Map<String,String>
        c++;
      }

      msgCtxt.setVariable(varName("result_format"), "map");
      // 4a. set a variable to hold the generated Map<String, Map>
      msgCtxt.setVariable(varName("result_java"), map);

      // 4b. set a variable to hold the number of rows read
      msgCtxt.setVariable(varName("rows_read"), String.format("%d", map.size()));
      // 5. for diagnostic purposes, serialize to JSON as well
      String jsonResult = om.writer().withDefaultPrettyPrinter().writeValueAsString(map);
      msgCtxt.setVariable(varName("result_json"), jsonResult);
    }

    return ExecutionResult.SUCCESS;
  }
}
