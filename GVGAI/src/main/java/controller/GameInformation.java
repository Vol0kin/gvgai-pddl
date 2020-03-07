package controller;

import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Map;

public class GameInformation {
    public String domainName;
    public Map<String, ArrayList<String>> correspondence;
    public String a;
/*
    public GameInformation(String file) {
        Yaml yaml = new Yaml();

        try {
            InputStream inputStream = new FileInputStream(new File(file));
            Map<String, Object> obj = yaml.load(inputStream);
            System.out.println(obj);
            System.out.println(obj.keySet());
            System.out.println(obj.get("gameInformation"));
            Map<String, Object> a = (Map<String, Object>) obj.get("gameInformation");
            System.out.println(a);
        } catch (FileNotFoundException e) {
            System.out.println(e.getStackTrace());
        }

        /*InputStream inputStream = this.getClass()
                .getClassLoader()
                .getResourceAsStream(file);

    }*/
}
