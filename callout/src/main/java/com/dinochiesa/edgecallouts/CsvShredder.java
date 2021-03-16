// CsvShredder.java
//
// This is the source code for a Java callout for Apigee Edge.  This
// callout is very simple - it shreds a CSV, then sets a Java Map into a
// context variable, and then returns SUCCESS.
//
// ------------------------------------------------------------------

package com.dinochiesa.edgecallouts;

import java.text.SimpleDateFormat;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

import java.io.InputStreamReader;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;

import com.apigee.flow.execution.ExecutionResult;
import com.apigee.flow.execution.spi.Execution;
import com.apigee.flow.message.MessageContext;
import com.apigee.flow.execution.ExecutionContext;
import com.apigee.flow.message.Message;

import com.fasterxml.jackson.databind.ObjectMapper;

public class CsvShredder implements Execution {
    private final static String varprefix= "csv_";

    private final ObjectMapper om = new ObjectMapper();

    private Map properties; // read-only

    public CsvShredder(Map properties) {
        this.properties = properties;
    }

    private static String varName(String s) { return varprefix + s;}

    private List<String> getFieldList(MessageContext msgCtxt) throws IllegalStateException {
        String fieldlist = (String) this.properties.get("fieldlist");
        if (fieldlist == null || fieldlist.equals("")) {
            return null; // assume the first row gives the field list
        }
        fieldlist = resolvePropertyValue(fieldlist, msgCtxt);
        if (fieldlist == null || fieldlist.equals("")) {
            return null; // assume the first row gives the field list
        }

        // now split by commas
        String[] parts = StringUtils.split(fieldlist,",");
        List<String> list = new ArrayList<String>();
        for(int i=0; i< parts.length; i++) {
            list.add(parts[i].trim());
        }
        return list;
    }


    // If the value of a property value begins and ends with curlies,
    // and has no intervening spaces, eg, {apiproxy.name}, then
    // "resolve" the value by de-referencing the context variable whose
    // name appears between the curlies.
    private String resolvePropertyValue(String spec, MessageContext msgCtxt) {
        if (spec.startsWith("{") && spec.endsWith("}") && (spec.indexOf(" ") == -1)) {
            String varname = spec.substring(1,spec.length() - 1);
            String value = msgCtxt.getVariable(varname);
            return value;
        }
        return spec;
    }


    public ExecutionResult execute (final MessageContext msgCtxt,
                                    final ExecutionContext execContext) {
        Message msg = msgCtxt.getMessage();
        try {
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
            }
            else {
                records = CSVFormat.DEFAULT.withHeader(list.toArray(new String[1])).parse(in);
            }

            // 3. process each record in the CSV, convert to an element in a map
            for (CSVRecord record : records) {
                // assume first field is the primary key
                String firstField = record.get(0);
                map.put(firstField, record.toMap());  // Map<String,String>
            }

            // 4. set a variable to hold the generated Map<String, Map>
            msgCtxt.setVariable(varprefix + "result_java", map);

            // 5. for diagnostic purposes, serialize to JSON as well
            String jsonResult = om.writer()
                .withDefaultPrettyPrinter()
                .writeValueAsString(map);
            msgCtxt.setVariable(varprefix + "result_json", jsonResult);
        }
        catch (java.lang.Exception exc1) {
            //exc1.printStackTrace(); // will go to stdout of message processor
            msgCtxt.setVariable(varName("error"), exc1.getMessage());
            msgCtxt.setVariable(varName("stacktrace"), ExceptionUtils.getStackTrace(exc1));
            return ExecutionResult.ABORT;
        }

        return ExecutionResult.SUCCESS;
    }
}
