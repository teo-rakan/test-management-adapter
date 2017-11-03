package com.epam.jira.util;

import com.epam.jira.entity.Issues;
import org.apache.commons.lang3.exception.ExceptionUtils;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.io.*;

public class FileUtils {

    private final static String TARGET_DIR = "\\target\\";

    public static void writeStackTrace(Throwable throwable, String filePath) {
        try {
            PrintWriter writer = new PrintWriter("." + TARGET_DIR + filePath, "UTF-8");
            writer.print(ExceptionUtils.getStackTrace(throwable));
            writer.close();
        } catch (FileNotFoundException | UnsupportedEncodingException e) {
            e.printStackTrace();
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
}
