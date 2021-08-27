# Flakime 
This project allows simulating flakiness by injecting exception during test sources compilation at computed locations.

## Usage
The tool is packaged as a maven plugin and can be used by providing the following plugin info in the target project `pom.xml`. \
The execution stage is set to `test-compile`.
```xml
<plugin>
    <groupId>anonymised</groupId>
    <artifactId>flakime-maven-plugin</artifactId>
    <version>{FLAKIME_VERSION}</version>
    <executions>
      <execution>
        <goals>
          <goal>flakime-injector</goal>
        </goals>
      </execution>
    </executions>
</plugin>
```


## Common configuration :
| tag                  | implementation & range                                 | default  | required | description                                                                    |
|----------------------|--------------------------------------------------------|-----------------------|----------|--------------------------------------------------------------------------------|
| `disableFlagName`    | string                                                 | FLAKIME_DISABLE       |          | The environment variable name of the flag disabling flakime   |
| `disableReport`      | boolean                                                | false                 |          | Allows to disable the generation of output files reporting the flake point for each test methods
| `flakeRate`          | float: 0..1                                            | 0.1                  |          | The nominal flake rate you wish to inject   |
| `model`           | string: {vocabulary,bernoulli}                         | bernoulli             |          | The model with which the flakiness probability of a test will be calculated |
| `annotationFilters`    | Array of string                                        | default for Junit v4 and v5                    |       | Test annotation to consider for flakime `@`                   |
| `methodFilters` | Array of regex String                           | none                  |          | Method name to consider for flakime|  
| `classFilters` | Array of regex String                           | none                  |          | Class name to consider for flakime|  
| `modelParameters` | Array of key-value property                            | none                  |          | The parameters specific to each model implementation (see desc. bellow)     |
| `skip` | boolean                           | false                  |          | Skip flakime execution|  
<br>

## Vocabulary model parameters :
| key                   | value implementation & range | default                                            | required                            | description                                                                                               |
|-----------------------|--------------------|----------------------------------------------------|-------------------------------------|-----------------------------------------------------------------------------------------------------------|
| `modelPath`           | string             | ./rfc_classifier                                                 |  | The path to the pre-trained serialized model, if not present a new model will be trained                                                             |
| `randomForestTrees`   | integer : > 0      | 100                                                |                                     | The number of Trees the random forest will be composed of. (No effect if `trainModel` is set to false)    |
| `randomForestThreads` | integer : > 0      | Number of CPU cores available on the machine       |                                     | The number of threads used during the random forest training. (No effect if `trainModel` is set to false) |

#### Sample configuration :
```xml
<configuration>
  <skip>false</skip>  
  <flakeRate>0.05</flakeRate>
  <disableReport>false</disableReport>
  <model>vocabulary</model>
  <annotationFilters>
    <annotation>@org.junit.jupiter.api.Test</annotation>
    <annotation>@org.junit.Test</annotation>
  </annotationFilters>
  <methodFilters>
    <method>^test</method>
  </methodFilters>
  <classFilters>
    <class>^Test</class>
  </classFilters>
  <disableFlagName>$MUT_IN_PROGRESS</disableFlagName>
  <modelParameters>
    <modelPath>rfc_classifier</modelPath>
    <randomForestTrees>100</randomForestTrees>
    <randomForestThreads>12</randomForestThreads>
  </modelParameters>
</configuration>