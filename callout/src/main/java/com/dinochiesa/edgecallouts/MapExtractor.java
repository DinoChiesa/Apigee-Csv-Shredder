// MapExtractor.java
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
import org.apache.commons.lang.exception.ExceptionUtils;

import com.apigee.flow.execution.ExecutionResult;
import com.apigee.flow.execution.spi.Execution;
import com.apigee.flow.message.MessageContext;
import com.apigee.flow.execution.ExecutionContext;
import com.apigee.flow.message.Message;

import com.fasterxml.jackson.databind.ObjectMapper;

public class MapExtractor implements Execution {

    private final ObjectMapper om = new ObjectMapper();

    private Map properties; // read-only

    public MapExtractor(Map properties) {
        this.properties = properties;
    }

    private String getFieldnameVariable(MessageContext msgCtxt) throws IllegalStateException {
        String fieldnameVariable = (String) this.properties.get("fieldnameVariable");
        if (fieldnameVariable == null || fieldnameVariable.equals("")) {
            throw new IllegalStateException("fieldnameVariable is null or empty.");
        }
        if (fieldnameVariable.indexOf(" ") != -1) {
            throw new IllegalStateException("fieldnameVariable includes a space.");
        }
        return fieldnameVariable;
    }

    private String getMapVariable(MessageContext msgCtxt) throws IllegalStateException {
        String mapVariable = (String) this.properties.get("mapVariable");
        if (mapVariable == null || mapVariable.equals("")) {
            throw new IllegalStateException("mapVariable is null or empty.");
        }
        if (mapVariable.indexOf(" ") != -1) {
            throw new IllegalStateException("mapVariable includes a space.");
        }
        return mapVariable;
    }

    public ExecutionResult execute (final MessageContext msgCtxt,
                                    final ExecutionContext execContext) {
        Message msg = msgCtxt.getMessage();
        String varprefix= "csv_";
        String varName = null;
        try {
            varName = getFieldnameVariable(msgCtxt);
            String fieldname = msgCtxt.getVariable(varName);
            fieldname = java.net.URLDecoder.decode(fieldname, "UTF-8");

            varName = getMapVariable(msgCtxt);
            @SuppressWarnings("unchecked")
            Map<String,Object> map = (Map<String,Object>) msgCtxt.getVariable(varName);

            if (map.containsKey(fieldname)) {
                @SuppressWarnings("unchecked")
                Map<String,String> map1 = (Map<String,String>) map.get(fieldname);

                String jsonResult = om.writer()
                    .withDefaultPrettyPrinter()
                    .writeValueAsString(map1);

                // set another variable to hold the json representation
                msgCtxt.setVariable(varprefix + "result_json", jsonResult);
            }
            else {
                msgCtxt.setVariable(varprefix + "result_json", "{}");
            }
        }
        catch (java.lang.Exception exc1) {
            exc1.printStackTrace();
            varName = varprefix + "error";
            msgCtxt.setVariable(varName, exc1.getMessage());
            varName = varprefix + "stacktrace";
            msgCtxt.setVariable(varName, ExceptionUtils.getStackTrace(exc1));
            return ExecutionResult.ABORT;
        }

        return ExecutionResult.SUCCESS;
    }
}
