package com.crystalline.aether.services.computation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Includer {
    private final ArrayList<String> includedTags;
    private final HashMap<String,String> libraries;
    public Includer(){
        libraries = new HashMap<>();
        includedTags = new ArrayList<>();
    }

    public Includer(Includer other){
        libraries = new HashMap<>(other.libraries);
        includedTags = new ArrayList<>(other.includedTags);
    }

    public Includer addSource(String source){
        Matcher m = Pattern.compile("\\/\\* *=* *(([A-Z_])*) *=* *\\*\\/").matcher(source);
        if(m.find()){
            libraries.put(m.group(1), source);
        }
        return this;
    }

    public String process(String rawSource){
        String processed = rawSource;
        Matcher m = Pattern.compile("\\<([A-Z_]*)\\>").matcher(processed);
        while(m.find()){
            String tag = m.group(1);
            if(libraries.containsKey(tag)&&(!includedTags.contains(tag))){
                processed = processed.replace("<"+tag+">", process(libraries.get(tag)));
                includedTags.add(tag);
            }else processed = processed.replace("<"+tag+">", "");
        }
        return processed;
    }
}
