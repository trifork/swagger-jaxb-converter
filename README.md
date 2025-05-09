# swagger-jaxb-converter
Equips swagger-maven-plugin-jakarta with the ability to generate openAPI specs from jaxb annotations

The swagger-maven-plugin-jakarta creates an openAPI spec for a project by scanning for JAX-RS annotations to detect operations. The data model is determined by scanning for swagger as well as jackson annotations. 

This component adds the ability to scan for JAXB annotations (@XmlType, @XmlElement, @XmlAttribute), so that swagger-maven-plugin-jakarta can be used on REST services that are based on the combination of JAX-RS JAXB. 

## Usage: 

Add swagger-jax-converter to the plugin classpath of swagger-maven-plugin-jakarta by adding it to the plugin classpath: 

```xml
    <build>
        <plugins>
            <!-- ... -->
            <plugin>
                <groupId>io.swagger.core.v3</groupId>
                <artifactId>swagger-maven-plugin-jakarta</artifactId>
                <version>2.2.30</version>
                <dependencies>
                    <dependency>
                        <groupId>com.trifork.swagger-jaxb-converter</groupId>
                        <artifactId>swagger-jaxb-converter</artifactId>
                        <version>1.0.0</version>
                    </dependency>
                </dependencies>
                <executions>
                    <execution>
                        <id>generate-openapi-spec</id>
                        <phase>compile</phase>
                        <goals>
                            <goal>resolve</goal>
                        </goals>
                        <configuration>
                            <resourcePackages>
                                <package>com.myproject.restservice</package>
                            </resourcePackages>
                            <outputFileName>openapi</outputFileName>
                            <outputFormat>JSONANDYAML</outputFormat>
                            <outputPath>${project.build.directory}/generated-sources/openapi</outputPath>
                            <sortOutput>true</sortOutput>
                            <prettyPrint>true</prettyPrint>
                            <configurationFilePath>
                                ${project.resources[0].directory}/openapi-conf.yaml</configurationFilePath>
                        </configuration>
                    </execution>
                </executions>
            </plugin>
            <!-- ... -->
        </plugins>
    </build>
```
