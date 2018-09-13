package cn.fudan.common.util;

import cn.fudan.analysis.tree.ClassNode;
import cn.fudan.analysis.tree.PackageNode;
import cn.fudan.analysis.util.DexHelper;
import org.jf.dexlib2.iface.Method;
import org.jf.dexlib2.iface.MethodParameter;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Created by yuanxzhang on 08/03/2017.
 */
public class HashHelper {

    public static String hash(String content) {
        return md5_16(content);
    }

    public static String md5_16(String content) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(content.getBytes());
            byte b[] = md.digest();

            int i;
            StringBuffer buf = new StringBuffer("");
            for (int offset = 0; offset < b.length; offset++) {
                i = b[offset];
                if (i < 0)
                    i += 256;
                if (i < 16)
                    buf.append("0");
                buf.append(Integer.toHexString(i));
            }

            return buf.toString().substring(8, 24);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String md5_32(String content) {
        return md5_32(content.getBytes());
    }

    public static String md5_32(byte[] content) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");

            md.update(content);

            byte[] b = md.digest();
            String appMD5 = "";
            for (int i = 0; i < b.length; i++) {
                appMD5 += Integer.toString((b[i] & 0xff) + 0x100, 16).substring(1);
            }
            return appMD5;
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String md5_32(File file) {
        try {
            FileInputStream fis = new FileInputStream(file);
            BufferedInputStream bis = new BufferedInputStream(fis);
            byte[] data = new byte[(int)file.length()];
            bis.read(data);
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(data);
            fis.close();

            byte[] b = md.digest();
            String appMD5 = "";
            for (int i = 0; i < b.length; i++) {
                appMD5 += Integer.toString((b[i] & 0xff) + 0x100, 16).substring(1);
            }
            return appMD5;
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
    }
}
