package com.epam.jira.util;

import com.epam.jira.core.JiraTestCase;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.util.List;

public class FileUtils {

    public static void writeStackTrace(Throwable throwable, String filePath) {
        try {
            PrintWriter writer = new PrintWriter("." + filePath, "UTF-8");
            writer.print(ExceptionUtils.getStackTrace(throwable));
            writer.close();
        } catch (FileNotFoundException | UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    public static void writeXmlFile(List<JiraTestCase> tests, String filePath) {
        try {
            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = docFactory.newDocumentBuilder();
            Document doc = builder.newDocument();

            // Create root node
            Element root = doc.createElement("tests");
            doc.appendChild(root);

            // Create child node for each test
            tests.forEach(test -> root.appendChild(createTestElement(doc, test)));

            // Save the document to the disk file
            writeToXmlFile(doc, filePath);
        } catch (TransformerException ex) {
            System.out.println("Error outputting document");
        } catch (ParserConfigurationException ex) {
            System.out.println("Error building document");
        }
    }

    private static void writeToXmlFile(Document doc, String filePath) throws TransformerException {
        TransformerFactory tranFactory = TransformerFactory.newInstance();
        Transformer aTransformer = tranFactory.newTransformer();

        // format the XML nicely
        aTransformer.setOutputProperty(OutputKeys.ENCODING, "ISO-8859-1");
        aTransformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
        aTransformer.setOutputProperty(OutputKeys.INDENT, "yes");

        DOMSource source = new DOMSource(doc);
        try {
            FileWriter fos = new FileWriter("." + filePath);
            StreamResult result = new StreamResult(fos);
            aTransformer.transform(source, result);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static Element createTestElement(Document doc, JiraTestCase test) {
        Element testNode = doc.createElement("test");

        // Create Jira Test key node
        Element key = doc.createElement("key");
        key.appendChild(doc.createTextNode(test.getJiraTestKey()));
        testNode.appendChild(key);

        // Create test status node
        Element status = doc.createElement("status");
        status.appendChild(doc.createTextNode(test.getStatus().toString()));
        testNode.appendChild(status);

        // Add attachments if necessary
        List<String> filePaths = test.getFilePaths();
        if (!filePaths.isEmpty())
            testNode.appendChild(createAttachmentsElement(doc, filePaths));

        // Add comments if necessary
        List<String> comments = test.getComments();
        if (!comments.isEmpty())
            testNode.appendChild(createCommentsElement(doc, comments));

        return testNode;
    }

    private static Element createAttachmentsElement(Document doc, List<String> filePaths) {
        Element attachments = doc.createElement("attachments");

        for (String path : filePaths) {
            Element attachment = doc.createElement("attachment");
            attachment.appendChild(doc.createTextNode(path));
            attachments.appendChild(attachment);
        }
        return attachments;
    }

    private static Element createCommentsElement(Document doc, List<String> commentList) {
        Element comments = doc.createElement("comments");

        for (String comment : commentList) {
            Element commentNode = doc.createElement("comment");
            commentNode.appendChild(doc.createTextNode(comment));
            comments.appendChild(commentNode);
        }
        return comments;
    }
}
