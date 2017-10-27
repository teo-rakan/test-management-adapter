# TestManagementAnnotation

You could install this artifact on a specific local repository by using maven install-file goal.
```bash
mvn install::install-file -Dfile=test-management-1.2.jar 
                          -DgroupId=com.epam.jira 
                          -DartifactId=test-management 
                          -Dversion=1.2 
                          -Dpackaging=jar
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
  }
```
* Using **listeners** element in *testng.xml*
* Adding listeners through TestNG **addListener()** API

As for **JUnit**:
* Adding listeners through **JUnitCore** API
```bash
    [...]
    JUnitCore runner = new JUnitCore();
    runner.addListener(new ExecutionListener());
    runner.run(TestFeatureOne.class, TestFeatureTwo.class);
    [...]
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
