# Test Management Adapter

This dependency gathers basic build information and useful artifacts (titled values, user defined files, screenshoots, stack traces) for further processing by Jenkins Test Management plugin.

## How it works

The key thing is that this adapter are working in tandem with Jenkins Test Management plugin which processes gathered information and updates Jira issues via REST API.

![Scheme](https://github.com/teo-rakan/test-management-adapter/blob/master/images/readme_scheme.jpg)

## Installation

At this time, Test Management adapter installation is possible only in **local** repository. Unfortunately, the adapter has not yet been placed into the Maven Central Repository.

### Using the command line

```bash
mvn install::install-file -Dfile=test-management-adapter-1.7-jar-with-dependencies.jar 
                          -DgroupId=com.epam.jira 
                          -DartifactId=test-management-adapter 
                          -Dversion=1.7
                          -Dpackaging=jar
```
**For copy-paste:** `mvn install::install-file -Dfile=test-management-adapter-1.7-jar-with-dependencies.jar -DgroupId=com.epam.jira -DartifactId=test-management-adapter -Dversion=1.7 -Dpackaging=jar`

After that you need to add next dependency to your pom-file: 
```bash
<dependency>
    <groupId>com.epam.jira</groupId>
    <artifactId>test-management-adapter</artifactId>
    <version>1.7</version>
</dependency>
```

## Execution Listener
Add `ExecutionListener` to your TestNG listeners by one of the following methods:

### Using _maven-surefire-plugin_ in your _pom.xml_

```bash
  <build>
      <plugins>
          [...]
          <plugin>
              <groupId>org.apache.maven.plugins</groupId>
              <artifactId>maven-surefire-plugin</artifactId>
              <version>2.20.1</version>
              <configuration>
                  <properties>
                      [...]
                      <property>
                          <name>listener</name>
                          <value>com.epam.jira.testng.ExecutionListener</value>
                      </property>
                      [...]
                  </properties>
              </configuration>
          </plugin>
          [...]
      </plugins>
  </build>
```

### Using _@Listeners_ annotation at class level

```bash
  @Listeners({com.epam.jira.testng.ExecutionListener.class})
  public class TestClass {
      // ...
  }
```

### Using _listeners_ element in _testng.xml_

```bash
  <?xml version="1.0" encoding="UTF-8"?>
  <suite name="Suite" parallel="false">
	  <listeners>
		  <listener class-name="com.epam.jira.testng.ExecutionListener" />
	  </listeners>
	  <test name="Test">
		  <classes>
			  [...]
		  </classes>
	  </test>
  </suite>
```

### Adding listeners through TestNG _addListener()_ API

```bash
  public static void main(String[] args) {
    TestNG testNG = new TestNG();
    testNG.setTestClasses(new Class[] { TestClass.class });
    testNG.addListener(new ExecutionListener());
    testNG.run();
  }
```

## Screenshots

You will need to initialize Screenshoter class with WebDriver instance in order to attach screenshots to JIRA issue in the fail cases.

```bash
    @BeforeClass
    public void initialize() {
        Screenshoter.initialize(driver);
    }
```

## Store information

You can store useful informatian such as string values (with titles) or files using **JiraInfoProvider** class.

```bash
    JiraInfoProvider.saveFile(new File("path_to_file"));
    JiraInfoProvider.saveValue("Title", "Some value");
```

## Retry failed tests

You can rerun your failed tests if needed. You can do that by one of the following methods:

### Add _AnnotationTransformer_ to your TestNG Listeners

You can do it in the same way as [Execution Listener](#execution-listener)   :warning: except **@Listeners** annotation

### Using @Test annotation retryAnalyzer property

```bash
    @JIRATestKey(key = "EPMFARMATS-1010")
    @Test(retryAnalyzer = RetryAnalyzer.class)
    public void testSomething() {
        ...
    }
```

If you want to rerun your test several times, you will need to use **@RetryCountIfFailed** annotation. The count of reruns will be desplayed in your Test report summary field.

```bash
    @JIRATestKey(key = "EPMFARMATS-1010")
    @RetryCountIfFailed(2)
    @Test(retryAnalyzer = RetryAnalyzer.class)
    public void testSomething() {
        ...
    }
```

## @JIRATestKey
Mark tests with **@JIRATestKey** annotation and specify corresponding issue key as its **key** parameter value.

```bash
  @Test
  @JIRATestKey(key = "EPMFARMATS-1010")
  public void testSomething() {
    Assert.assertTrue(true);
  }
```

After running the `tm-testng.xml` results file with attachments will be created in your project `target` directory.
