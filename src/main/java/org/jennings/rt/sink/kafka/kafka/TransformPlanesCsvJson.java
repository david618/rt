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

import com.esri.core.geometry.OperatorExportToJson;
import com.esri.core.geometry.Point;
import com.esri.core.geometry.SpatialReference;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 * @author david
 */
public class TransformPlanesCsvJson implements Transform {

    static final Pattern PATTERN = Pattern.compile("(([^\"][^,]*)|\"([^\"]*)\"),?");

    @Override
    public String transform(String line) {
        try {
            ArrayList<String> vals = new ArrayList<>();
            Matcher matcher = PATTERN.matcher(line);
            int i = 0;
            while (matcher.find()) {
                if (matcher.group(2) != null) {
                    //System.out.print(matcher.group(2) + "|");
                    vals.add(i, matcher.group(2));
                } else if (matcher.group(3) != null) {
                    //System.out.print(matcher.group(3) + "|");
                    vals.add(i, matcher.group(3));
                }
                i += 1;
            }

            //0,1506911657612,230.15,2497.68,41.29,1,"Wright-Patterson Air Force Base","Mielec Airport",-1,-14.95027,59.27356
            Integer id = Integer.parseInt(vals.get(0));
            Long ts = Long.parseLong(vals.get(1));
            Double spd = Double.parseDouble(vals.get(2));
            Double dst = Double.parseDouble(vals.get(3));
            Double brg = Double.parseDouble(vals.get(4));
            Integer rid = Integer.parseInt(vals.get(5));
            String orig = vals.get(6);
            String dest = vals.get(7);
            Integer s2d = Integer.parseInt(vals.get(8));
            Double lon = Double.parseDouble(vals.get(9));
            Double lat = Double.parseDouble(vals.get(10));

            JSONObject attrJson = new JSONObject();
            attrJson.put("id", id);
            attrJson.put("ts", ts);
            attrJson.put("speed", spd);
            attrJson.put("dist", dst);
            attrJson.put("bearing", brg);
            attrJson.put("rtid", rid);
            attrJson.put("orig", orig);
            attrJson.put("dest", dest);
            attrJson.put("secsToDep", s2d);
            attrJson.put("lon", lon);
            attrJson.put("lat", lat);

            Point pt = new Point(lon, lat);

            SpatialReference sr = SpatialReference.create(4326);

            String geomJsonString = OperatorExportToJson.local().execute(sr, pt);
            JSONObject geomJson = new JSONObject(geomJsonString);
            
            JSONObject combined = new JSONObject();
            combined.put("attr", attrJson);
            combined.put("geom", geomJson);            
            
            return combined.toString();
        } catch (NumberFormatException | JSONException e) {
            
            return "{\"error\":\"" + e.getMessage() + "\"";
        }

        
    }

}
