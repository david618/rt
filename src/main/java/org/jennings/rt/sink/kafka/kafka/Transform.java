/*

Interface for parsing CSV to JSON

Input
Line of Text (csv, json, xml)


Output 
Line of Text (csv, json, xml)


 */
package org.jennings.rt.sink.kafka.kafka;
/**
 *
 * @author david
 */
public interface Transform {
    public String transform(String line);

}
