# TestManagementAnnotation

You could install this artifact on a specific local repository by using maven install-file goal.
```bash
mvn install::install-file -Dfile=test-management-1.2.jar 
                          -DgroupId=com.epam.jira 
                          -DartifactId=test-management 
                          -Dversion=1.2 
                          -Dpackaging=jar
```

After that you need to add next dependency to your pom-file: 
```bash
<dependency>
    <groupId>com.epam.jira</groupId>
    <artifactId>test-management</artifactId>
    <version>1.3</version>
</dependency>
```

## ExecutionListener
Add `ExecutionListener` to your JUnit or TestNG listeners. 

* Using **maven-surefire-plugin** in your *pom.xml*

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

Also there are several other ways of doing this for **TestNG**:
* Using **@Listeners** annotation at class level
```bash
  @Listeners({com.epam.jira.testng.ExecutionListener.class})
  public class TestClass {
      // ...
  }
```
* Using **listeners** element in *testng.xml*
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
* Adding listeners through TestNG **addListener()** API
```bash
  public static void main(String[] args) {
    TestNG testNG = new TestNG();
    testNG.setTestClasses(new Class[] { TestClass.class });
    testNG.addListener(new ExecutionListener());
    testNG.run();
  }
```

As for **JUnit**:
* Adding listeners through **JUnitCore** API
```bash
  public static void main(String[] args) {
    JUnitCore runner = new JUnitCore();
    runner.addListener(new ExecutionListener());
    runner.run(TestFeatureOne.class, TestFeatureTwo.class);
  }
```

## @JIRATestKey
Mark tests with `@JIRATestKey` tag.

```bash
  @Test
  @JIRATestKey(key = "TEST-08")
  public void testSomething() {
    Assert.assertTrue(true);
  }
```

After running the `tm.xml` results file will be created in your project `target` directory.
