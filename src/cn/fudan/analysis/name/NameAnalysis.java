package cn.fudan.analysis.name;

import cn.fudan.common.CodeContainer;

import java.io.*;

/**
 * Created by yuanxzhang on 12/03/2017.
 */
public class NameAnalysis {
    public static NameCollector analyzeNames(CodeContainer container) {
        return new NameCollector(container);
    }

    public static void persistNamesToFile(NameCollector collector, File file) {
        try {
            ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file));
            oos.writeObject(collector);
            oos.close();
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }
    }

    public static NameCollector readNamesFromFile(File file) {
        NameCollector collector = null;
        try {
            ObjectInputStream iis = new ObjectInputStream(new FileInputStream(file));
            collector = (NameCollector) iis.readObject();
            iis.close();
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }
        return collector;
    }
}
