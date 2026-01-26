---
layout: default
---
# Write an IIDM importer

From Powsybl's `Importer` interface, it is possible to add a new file format from which
an IIDM data model can be loaded.

In order to do so, you will need to:
- Write an implementation of the `Importer` interface
- Declare the new class as a service implementation with the `@AutoService` annotation
- Build your jar

## Configuring your module

In order to implement a new `Importer`, add the following dependencies in your `pom.xml` file:
- `auto-service (com.google.auto.service)`: Configuration/metadata generator for `ServiceLoader`-style providers
- `powsybl-iidm-converter-api`:  IIDM network import/export API

```xml
<dependencies>
    <dependency>
        <groupId>com.google.auto.service</groupId>
        <artifactId>auto-service</artifactId>
        <version>1.0-rc2</version>
    </dependency>
    <dependency>
        <groupId>com.powsybl</groupId>
        <artifactId>powsybl-iidm-converter-api</artifactId>
        <version>${powsybl.core.version}</version>
    </dependency>
</dependencies>
```

## Implementation

As said above, you will need to write your own implementation of the `Importer` interface and declare it as a service
implementation. Here is an empty class template of an `Importer` implementation:

```java
import com.powsybl.commons.datasource.DataSource;
import com.powsybl.commons.datasource.ReadOnlyDataSource;
import com.powsybl.iidm.import_.Importer;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.parameters.Parameter;

import java.util.List;
import java.util.Properties;

@AutoService(Importer.class)
public class MyImporter implements Importer {

    /**
     * Get a unique identifier of the format.
     *
     * @return the unique ID for the given format
     */
    @Override
    public String getFormat() {
        return null;
    }

    /**
     * This override is optional. By default, it returns Collections.emptyList()
     * 
     * @return description of import parameters
     */
    @Override
    public List<Parameter> getParameters() {
        return Collections.emptyList();
    }

    /**
     * Get some information about this importer.
     *
     * @return information about the importer
     */
    @Override
    public String getComment() {
        return null;
    }

    /**
     * Check if the data source is importable
     *
     * @param dataSource the data source
     * @return true if the data source is importable, false otherwise
     */
    @Override
    public boolean exists(ReadOnlyDataSource dataSource) {
        return false;
    }

    /**
     * Create a model.
     *
     * @param dataSource data source
     * @param parameters some properties to configure the import
     * @return the model
     */
    @Override
    public Network importData(ReadOnlyDataSource dataSource, Properties parameters) {
        // business logic to import a network from a data source in a given format
        return null;
    }

    /**
     * Copy data from one data source to another.
     *
     * @param fromDataSource from data source
     * @param toDataSource   destination data source
     */
    @Override
    public void copy(ReadOnlyDataSource fromDataSource, DataSource toDataSource) {
        // business logic to copy a network from a data source to another file in a given format
    }
}
```

## Deployment

### Generating jar

Once your implementation is ready, run the following command to create your project jar:
```
$ cd <PROJECT_HOME>
$ mvn clean package
```

The jar file will be generated in `<PROJECT_HOME>/target`.

### Adding the format in iTools

[iTools](../itools/index.md) allows the user to convert a network from one format to another via the
`convert-network` command line.

You can add your custom import format, allowing files in this format to be converted using the command, by copying the
generated jar in your powsybl distribution:
```
$> cp target/my-exporter.jar <POWYSBL_HOME>/share/java
``` 

## Examples

The code of a simple CSV Importer is available in [powsybl-tutorials](https://github.com/powsybl/powsybl-tutorials) as a
complete example of this tutorial.

To try it, clone the project and deploy as below:
```
$ git clone https://github.com/powsybl/powsybl-tutorials.git
$ cd powsybl-tutorials/csv-importer
$ mvn clean package
```
