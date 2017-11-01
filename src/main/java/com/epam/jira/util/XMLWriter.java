package com.epam.jira.util;

import com.epam.jira.core.JiraTestCase;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class XMLWriter {
    public static void writeXmlFile(List<JiraTestCase> tests, String filePath)  {

        try {
            DocumentBuilderFactory dFact = DocumentBuilderFactory.newInstance();
            DocumentBuilder build = dFact.newDocumentBuilder();
            Document doc = build.newDocument();

            // Create root node
            Element root = doc.createElement("tests");
            doc.appendChild(root);

            // Create child node for each test
            for (JiraTestCase test : tests) {
                Element testNode = doc.createElement("test");
                root.appendChild(testNode);

                Element key = doc.createElement("key");
                key.appendChild(doc.createTextNode(test.getJiraTestKey()));
                testNode.appendChild(key);

                Element status = doc.createElement("status");
                status.appendChild(doc.createTextNode(test.getStatus().toString()));
                testNode.appendChild(status);

                List<String> filePaths = test.getFilePaths();
                if (!filePaths.isEmpty()) {
                    Element attachments = doc.createElement("attachments");
                    testNode.appendChild(attachments);
                    for (String path : filePaths) {
                        Element attachment = doc.createElement("attachment");
                        attachment.appendChild(doc.createTextNode(path));
                        attachments.appendChild(attachment);
                    }
                }

                List<String> commentList = test.getComments();
                if (!commentList.isEmpty()) {
                    Element comments = doc.createElement("comments");
                    testNode.appendChild(comments);
                    for (String comment : commentList) {
                        Element commentNode = doc.createElement("comment");
                        commentNode.appendChild(doc.createTextNode(comment));
                        comments.appendChild(commentNode);
                    }
                }
            }

            // Save the document to the disk file
            TransformerFactory tranFactory = TransformerFactory.newInstance();
            Transformer aTransformer = tranFactory.newTransformer();

            // format the XML nicely
            aTransformer.setOutputProperty(OutputKeys.ENCODING, "ISO-8859-1");
            aTransformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
            aTransformer.setOutputProperty(OutputKeys.INDENT, "yes");

            DOMSource source = new DOMSource(doc);
            try {
                FileWriter fos = new FileWriter(filePath);
                StreamResult result = new StreamResult(fos);
                aTransformer.transform(source, result);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (TransformerException ex) {
            System.out.println("Error outputting document");
        } catch (ParserConfigurationException ex) {
            System.out.println("Error building document");
        }
    }
}
