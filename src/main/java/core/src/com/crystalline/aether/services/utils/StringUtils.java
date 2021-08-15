package com.crystalline.aether.services.utils;

import com.badlogic.gdx.files.FileHandle;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class StringUtils {

    public static String readFileAsString(FileHandle file){
        StringBuffer fileData = new StringBuffer();
        try (BufferedReader reader = new BufferedReader(new FileReader(file.file()))){
            char[] buf = new char[1024];
            int numRead=0;
            while((numRead=reader.read(buf)) != -1){
                String readData = String.valueOf(buf, 0, numRead);
                fileData.append(readData);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return fileData.toString();
    }
}
