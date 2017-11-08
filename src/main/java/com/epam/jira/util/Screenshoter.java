package com.epam.jira.util;

import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.remote.RemoteWebDriver;

import java.io.File;


public class Screenshoter {
    private final static String SCREENSHOT_FILE = "scr_%d.png";
    private static WebDriver driverInstance;

    public static void initialize(WebDriver driver) {
        driverInstance = driver;
    }

    public static boolean isInitialized() {
        return driverInstance != null;
    }

    public static String takeScreenshot() {
        if (!isInitialized()) return null;

        if (!(driverInstance instanceof RemoteWebDriver)) {
            System.out.println("Unsupported driver type: " + driverInstance.getClass().getName());
            return null;
        }

        File screenshot = ((TakesScreenshot) driverInstance).getScreenshotAs(OutputType.FILE);
        String screenshotName = String.format(SCREENSHOT_FILE, System.nanoTime());
        String filePath = FileUtils.saveFile(screenshot, screenshotName);

        return filePath != null ? screenshotName : null;
    }
}
