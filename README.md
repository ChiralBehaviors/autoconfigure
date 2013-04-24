autoconfigure
=============

A simple, non invasive auto configuration utility for dealing with the hell that is the current state of distributed systems

License: Apache 2.0

Requires Maven 3 and Java 1.7 to build.

To build:

    cd <root dir>
    mvn clean install

See the [autoconfigure wiki](https://github.com/Hellblazer/autoconfigure/wiki) for more information.
If you're just impatient, see the ZookeeperExample functional test.  You'll be clueless until you
read the wiki, but it'll satisfy your impetuous need to get to the meat of things right away.

### Maven configuration

For snapshots, include the hellblazer snapshot repository:

    <repository>
        <id>hellblazer-snapshots</id>
        <url>https://repository-hal900000.forge.cloudbees.com/snapshot/</url>
    </repository>
    
add as dependency:

    <dependency>
        <groupId>com.hellblazer</groupId>
        <artifactId>autoconfigure</artifactId>
        <version>0.0.1-SNAPSHOT</version>
    </dependency>

