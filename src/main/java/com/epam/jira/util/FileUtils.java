package com.epam.jira.util;

import com.epam.jira.entity.Issues;
import org.apache.commons.lang3.exception.ExceptionUtils;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.io.*;
import java.time.LocalDateTime;


/**
 * FileUtils is a util class which provides useful methods for file writing.
 */
public class FileUtils {

    private final static String TARGET_DIR = "\\target\\";
    private final static String ATTACHMENTS_DIR = TARGET_DIR + "attachments\\";

    public static String save(Throwable throwable) {
        String message = null;
        if (throwable != null) {
            String filePath = String.format("stacktrace_%s.txt", LocalDateTime.now().toString().replace(":", "-"));
            String exceptionMessage = throwable.getMessage();
            if (exceptionMessage.contains("\n"))
                exceptionMessage = exceptionMessage.substring(0, exceptionMessage.indexOf('\n'));


            FileUtils.writeStackTrace(throwable, filePath);
            message = "Failed due to: " + throwable.getClass().getName() + ": " + exceptionMessage
                    + ".\nFull stack trace attached as " + filePath;
        }
        return message;
    }

    /**
     * Writes stack trace in temporary file and save it to attachments directory
     * @param throwable The exception for getting stacktrace
     * @param filePath The path for output file
     */
    private static void writeStackTrace(Throwable throwable, String filePath) {
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

    /**
     * Parse xml file using JAXB possibilities. The entities for marshaling are the same as in
     * Test Management Jira plugin.
     * @param issues the list of issues for writing
     * @param filePath the path to output file
     */
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

    static String getTargetDir() {
        return TARGET_DIR;
    }

    public static String getAttachmentsDir() {
        return ATTACHMENTS_DIR;
    }
}
