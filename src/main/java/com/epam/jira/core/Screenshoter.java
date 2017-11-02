package com.epam.jira.core;

import org.apache.commons.io.FileUtils;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.remote.RemoteWebDriver;

import java.io.File;
import java.io.IOException;


public class Screenshoter {
    private static WebDriver driverInstance;

    public static void initialize(WebDriver driver) {
        driverInstance = driver;
    }

    public static boolean isInitialized() {
        return driverInstance != null;
    }

    public static String takeScreenshot(String screensFolder) {
        if (!isInitialized()) return null;

        if (!(driverInstance instanceof RemoteWebDriver)) {
            System.out.println("Unsupported driver type: " + driverInstance.getClass().getName());
            return null;
        }

        File screenshot = ((TakesScreenshot) driverInstance).getScreenshotAs(OutputType.FILE);
        try {
            String screenshotName =  "scr_" + System.nanoTime() + ".png";
            File copy = new File("." + screensFolder + screenshotName);
            FileUtils.copyFile(screenshot, copy);
            return screenshotName;
        } catch (IOException e) {
            System.out.println("Failed to make screenshot.");
            return null;
        }
    }
}
