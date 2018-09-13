package cn.fudan.libpecker.main;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class TestDriver {
    public static void main(String[] args) {
        try {
            List<String> lines = FileUtils.readLines(new File("test/apk_lib_list.txt"));

            for (String line : lines) {
                String[] parts = line.split(":");

                String apkPath = "test/apk/"+parts[0]+".apk";
                String libPath = "test/lib/"+parts[1]+".dex";
                double actualSimilarity = Double.parseDouble(parts[2]);

                double similarity = ProfileBasedLibPecker.singleMain(apkPath, libPath);
                if (Math.abs(actualSimilarity-similarity) < 0.0001) {
                    System.out.println("=== pass test: "+apkPath+" "+libPath);
                    System.out.println("similarity: "+actualSimilarity);
                }
                else {
                    System.err.println("=== fail test: "+apkPath+" "+libPath);
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
