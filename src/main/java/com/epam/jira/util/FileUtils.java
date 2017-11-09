package com.epam.jira.util;

import com.epam.jira.entity.Issues;
import org.apache.commons.lang3.exception.ExceptionUtils;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.io.*;

public class FileUtils {

    private final static String TARGET_DIR = "\\target\\";
    private final static String ATTACHMENTS_DIR = TARGET_DIR + "attachments\\";

    public static void writeStackTrace(Throwable throwable, String filePath) {
        try {
            File temp = File.createTempFile("stacktrace", ".tmp");
            BufferedWriter out = new BufferedWriter(new FileWriter(temp));
            out.write(ExceptionUtils.getStackTrace(throwable));
            out.close();
            saveFile(temp, filePath);
            temp.delete();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Copy and save file to the attachments default directory. If file in the
     * default attachments directory already exists the file will be created in
     * child directory with name contains current time in nanoseconds using
     * System::nanoTime possibilities.
     * @param file the file to save
     * @param newFilePath the path relative to attachments dir
     * @return the path where file was actually saved
     */
    public static String saveFile(File file, String newFilePath) {
        try {
            String relativeFilePath = ATTACHMENTS_DIR;
            File copy = new File("." + relativeFilePath + newFilePath);
            if (copy.exists()) {
                relativeFilePath += System.nanoTime() + "\\";
                copy = new File("." + relativeFilePath + newFilePath);
            }
            org.apache.commons.io.FileUtils.copyFile(file, copy);
            return relativeFilePath + newFilePath;
        } catch (IOException e) {
            return null;
        }
    }

    public static void writeXml(Issues issues, String filePath) {
        try {
            JAXBContext jaxbCtx = JAXBContext.newInstance(Issues.class);
            Marshaller marshaller = jaxbCtx.createMarshaller();
            marshaller.setProperty(javax.xml.bind.Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            marshaller.marshal(issues, new File("." + TARGET_DIR + filePath));
        } catch (JAXBException ex) {
            System.out.println(ex.getMessage());
        }
    }

    public static String getTargetDir() {
        return TARGET_DIR;
    }

    public static String getAttachmentsDir() {
        return ATTACHMENTS_DIR;
    }
}
