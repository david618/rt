/*
 * (C) Copyright 2017 David Jennings
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     David Jennings
 */
package org.jennings.rt.sink.kafka.kafka;

import org.json.JSONObject;

/**
 *
 * @author david
 */
public class TransformPlanesJsonCsv implements Transform {

    @Override
    public String transform(String line) {
        
        String del = ",";
        
        try {
            JSONObject json = new JSONObject(line);
            
            String outLine = "";
            
            JSONObject attrJson = json.getJSONObject("attr");
            
            outLine += attrJson.getInt("id");
            outLine += del + attrJson.getLong("ts");
            outLine += del + attrJson.getDouble("speed");
            outLine += del + attrJson.getDouble("dist");
            outLine += del + attrJson.getDouble("bearing");
            outLine += del + attrJson.getInt("rtid");
            outLine += del + "\"" + attrJson.getString("orig") + "\"";
            outLine += del + "\"" + attrJson.getString("dest") + "\"";
            outLine += del + attrJson.getInt("secsToDep");
            outLine += del + attrJson.getDouble("lon");
            outLine += del + attrJson.getDouble("lat");
            
            JSONObject geomJson = json.getJSONObject("geom");
            
            outLine += del + "'''" + geomJson.toString() + "'''";
            
            outLine += "\n";
            
            return outLine;
            
        } catch (Exception e) {
            return "{\"error\":\"" + e.getMessage() + "\"";
        }
    }
    
}
