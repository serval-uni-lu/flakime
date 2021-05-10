# Flakime 
This project allows simulating flakiness by injecting exception during test sources compilation at computed locations.

## Usage
The tool is packaged as a maven plugin and can be used by providing the following plugin info in the target project `pom.xml`. \
The execution stage is set to `test-compile`.
```xml
<plugin>
    <groupId>lu.uni.serval</groupId>
    <artifactId>flakime-maven-plugin</artifactId>
    <version>{FLAKI_ME_VERSION}</version>
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
| `flakeRate`          | float: 0..1                                            | 0.1                  |          | The threshold at which to consider a test with non-null probability to flake   |
| `strategy`           | string: {vocabulary,bernoulli}                         | bernoulli             |          | The strategy with which the flakiness probability of a test will be calculated |
| `testAnnotations`    | Array of string                                        | default for Junit v4 and v5                    |       | The Test annotations used in the test suite preceeded by `@`                   |
| `strategyParameters` | Array of key-value property                            | none                  |          | The parameters specific to each strategy implementation (see desc. bellow)     |
| `testPattern` | regex String                            | none                  |          | The regex used to identify tests in test classes (if no annotation were used)     |
| `skip` | boolean                           | false                  |          | Skip flakime execution|  
</br>

## Vocabulary strategy parameters :
| key                   | value implementation & range | default                                            | required                            | description                                                                                               |
|-----------------------|--------------------|----------------------------------------------------|-------------------------------------|-----------------------------------------------------------------------------------------------------------|
| `modelPath`           | string             |   None                                                 |  | The path to the pre-trained serialized model, if not present a new model will be trained                                                             |
| `randomForestTrees`   | integer : > 0      | 100                                                |                                     | The number of Trees the random forest will be composed of. (No effect if `trainModel` is set to false)    |
| `randomForestThreads` | integer : > 0      | Number of CPU cores available on the machine       |                                     | The number of threads used during the random forest training. (No effect if `trainModel` is set to false) |

#### Bernoulli strategy parameters :

*TBD*

#### Sample configuration :
```xml
<configuration>
  <flakeRate>0.99</flakeRate>
  <strategy>vocabulary</strategy>
  <testAnnotations>
    <annotation>@org.junit.jupiter.api.Test</annotation>
    <annotation>@org.junit.Test</annotation>
  </testAnnotations>
  <disableFlagName>$DISABLE_FLAG_NAME</disableFlagName>
  <strategyParameters>
    <trainModel>false</trainModel>
    <modelPath>rfc_classifier</modelPath>
    <randomForestTrees>20</randomForestTrees>
    <randomForestThreads>12</randomForestThreads>
  </strategyParameters>
</configuration>
```
## Background
### Bernoulli Strategy 
TBD

### Vocabulary Strategy
TBD
