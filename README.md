# TestManagementAnnotation

You could install this artifact on a specific local repository by using maven install-file goal.
```bash
mvn install::install-file -Dfile=test-management-1.2.jar 
                          -DgroupId=com.epam.jira 
                          -DartifactId=test-management 
                          -Dversion=1.2 
                          -Dpackaging=jar
```
You should add ExecutionListener to your JUnit or TestNG listeners. After running the `tm.xml` results file will be created in your project `target` directory.
