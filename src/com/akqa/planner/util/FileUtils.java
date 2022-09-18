package com.akqa.planner.util;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class FileUtils {

    public static String DELIMITER = ",";

    // Read a CSV file and create a nested list of list of strings
    public static List<List<String>> readCSV(String fileName){
        // Declare return type
        List<List<String>> data = new ArrayList<>();

        try{
            String line;
            BufferedReader br = new BufferedReader(new FileReader(fileName));
            while ((line = br.readLine()) != null) {
                String[] values = line.split(DELIMITER);
                data.add(Arrays.asList(values).stream().map( s -> s.trim()).collect(Collectors.toList()));
            }

        } catch (FileNotFoundException e) {
                e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return data;
    }

    public static void writeCSV(List<List<String>> data, String fileName) {
        try{
            BufferedWriter bw = new BufferedWriter(new FileWriter((fileName)));
            for(List<String> row : data){
                for(String col: row){
                    bw.write(col + ",");
                }
                bw.write("\r\n");
            }
            bw.flush();
            bw.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

