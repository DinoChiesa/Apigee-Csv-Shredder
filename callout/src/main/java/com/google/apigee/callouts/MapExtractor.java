// MapExtractor.java
//
// ------------------------------------------------------------------

package com.google.apigee.callouts;

import com.apigee.flow.execution.ExecutionResult;
import com.apigee.flow.execution.spi.Execution;
import com.apigee.flow.message.MessageContext;
import java.util.Map;

public class MapExtractor extends CalloutBase implements Execution {

  public MapExtractor(Map properties) {
    super(properties);
  }

  public String getVarnamePrefix() {
    return "csv";
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

  public ExecutionResult execute0(final MessageContext msgCtxt) throws Exception {
    String fieldname = msgCtxt.getVariable(getFieldnameVariable(msgCtxt));
    fieldname = java.net.URLDecoder.decode(fieldname, "UTF-8");

    @SuppressWarnings("unchecked")
    Map<String, Object> map = (Map<String, Object>) msgCtxt.getVariable(getMapVariable(msgCtxt));

    if (map.containsKey(fieldname)) {
      @SuppressWarnings("unchecked")
      Map<String, String> map1 = (Map<String, String>) map.get(fieldname);

      String jsonResult = om.writer().withDefaultPrettyPrinter().writeValueAsString(map1);

      // set another variable to hold the json representation
      msgCtxt.setVariable(varName("result_json"), jsonResult);
    } else {
      msgCtxt.setVariable(varName("result_json"), "{}");
    }

    return ExecutionResult.SUCCESS;
  }
}
