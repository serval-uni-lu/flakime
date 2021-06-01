![build](https://github.com/UL-SnT-Serval/flakime/actions/workflows/build.yml/badge.svg)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

# Flakime 
This project allows simulating flakiness by injecting exception during test sources compilation at computed locations.

## Usage
The tool is packaged as a maven plugin and can be used by providing the following plugin info in the target project `pom.xml`. \
The execution stage is set to `test-compile`.
```xml
<plugin>
    <groupId>lu.uni.serval</groupId>
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
```
## Background

### Vocabulary Strategy
To demonstrate the capability of FlakiMe to simulate rich flakiness scenarios, we take advantage of a state-of-the-art 
flakiness prediction approach that identifies tests that could be flaky. This approach relies on source code tokens 
similarity with real  flaky tests  in vivo {Pinto2020, Haben2021, Camara2021}.

More specifically, the approach introduced by {Pinto2020} uses a set of 1,874 flaky tests extracted from 24 open-source projects
{Bell2018} to construct training samples that they could feed to a classifier. They represent a training sample with its binary label 
( test being flaky or not ) and its features: for each token contained in all the 1,874 flaky tests, whether the token is present 
(i.e., number of occurrences in the sample). The Random Forest Classifier algorithm (RFC) achieved the best performance 
(the highest precision and F-$1$ score) towards other algorithms from their empirical evaluation.

To be able to use their approach in FlakiMe we have extended the initial training set with the token of the targeted 
project tests (i.e., the project under flakiness simulation) and trained the RFC following {Pinto2020} process.  
After which, we could use the resulting model to assess the flakiness probability of a set of tokens specific to the targeted project. 
FlakiMe extracts the set of token at compile time for each statement of the targeted test resulting in a flakepoint after 
each statement with its computed \emph{flakiness probability}. 
Following the vocabulary-based prediction model, tests that execute similar pieces of code have a dependent likelihood to be flaky.
Overall, including this approach in FlakiMe allows drawing on real-world flakiness distribution data.

##References
 - **pinto2020** : Pinto, Gustavo and Miranda, Breno and Dissanayake, Supun and D'Amorim, Marcelo and Treude, Christoph and Bertolino, Antonia
"What is the Vocabulary of Flaky Tests?" in Proceedings of the 17th International Conference on Mining Software Repositories
 - **Haben2021** : Haben, Guillaume and Habchi, Sarra and Papadakis, Mike and Cordy, Maxime and Le Traon, Yves 
   "A Replication Study on the Usability of Code Vocabulary in Predicting Flaky Tests" in Proceedings of the 18th International Conference on Mining Software Repositories
 - **Camara2021** : Camara, B. H. P. and Silva, M. A. G. and Endo, A. T. and Vergilio, S. R.,
    "What is the Vocabulary of Flaky Tests? An Extended Replication" in Proceedings of the 29th IEEE/ACM International Conference on Program Comprehension


