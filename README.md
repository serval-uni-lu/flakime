# Flakime 
This project allows to simulate flakiness by injecting exception during compile time at selected locations.

##Usage
The tool is packaged as a maven plugin and can be used by providing the following plugin info in the target project `pom.xml`.

```
<plugin>
        <groupId>lu.uni.serval</groupId>
        <artifactId>flakime-maven-plugin</artifactId>
        <version>0.1.0-SNAPSHOT</version>
        <executions>
          <execution>
            <goals>
              <goal>flakime-injector</goal>
            </goals>
          </execution>
        </executions>
        <configuration>
          <flakeRate>0.99</flakeRate>
          <strategy>vocabulary</strategy>
          <testAnnotations>
            <annotation>@org.junit.jupiter.api.Test</annotation>
            <annotation>@org.junit.Test</annotation>
          </testAnnotations>
          <strategyParameters>
            <property>
              <name>trainModel</name>
              <value>false</value>
            </property>
            <property>
              <name>modelPath</name>
              <value>rfc_classifier</value>
            </property>
            <property>
              <name>randomForestTrees</name>
              <value>20</value>
            </property>
            <property>
              <name>randomForestThreads</name>
              <value>12</value>
            </property>
          </strategyParameters>
        </configuration>
      </plugin>
```

Available strategies are :

###Bernoulli
TBD

###Vocabulary
TBD