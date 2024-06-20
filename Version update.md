
### 1. Maven
Older version:

```xml
<maven.compiler.source>11</maven.compiler.source>
<maven.compiler.target>11</maven.compiler.target>
<maven.compiler.version>3.8.0</maven.compiler.version>
<maven.jar.plugin.version>3.0.2</maven.jar.plugin.version>
<maven.surefire.plugin.version>2.22.0</maven.surefire.plugin.version>
<maven.jacoco.version>0.8.5</maven.jacoco.version>
```

Updated version:

```xml
<maven.compiler.source>17</maven.compiler.source>
<maven.compiler.target>17</maven.compiler.target>
<maven.compiler.version>3.8.1</maven.compiler.version>
<maven.jar.plugin.version>3.3.0</maven.jar.plugin.version>
<maven.surefire.plugin.version>3.2.5</maven.surefire.plugin.version>
<maven.jacoco.version>0.8.8</maven.jacoco.version>
```

### 2. Spring boot and its related components

older version:

```xml
<spring.boot.version>2.0.2.RELEASE</spring.boot.version>
<spring.data.version>2.0.9.RELEASE</spring.data.version>
<spring-cloud-config.version>2.0.0.RELEASE</spring-cloud-config.version>
```

updated version:

```xml
<spring.boot.version>2.7.18</spring.boot.version>
<spring.data.version>2.7.18</spring.data.version>
<spring-cloud-config.version>3.1.3</spring-cloud-config.version>
```

## 3. junit

Older:

```xml
<junit.version>4.12</junit.version>
```

Updated:

```xml
<junit.version>4.13.2</junit.version>
```

## 4. lombok

Older:

```xml
<lombok.version>1.18.8</lombok.version>
```

### 
```xml
<lombok.version>1.18.30</lombok.version>
```

### 5.  Added plugin to support lombok (needed for java 17):

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    <version>${maven.compiler.version}</version>
    <configuration>
        <source>${maven.compiler.source}</source>
        <target>${maven.compiler.target}</target>
        <fork>true</fork>
        <compilerArgs>
            <arg>-J--add-opens=jdk.compiler/com.sun.tools.javac.code=ALL-UNNAMED</arg>
            <arg>-J--add-opens=jdk.compiler/com.sun.tools.javac.comp=ALL-UNNAMED</arg>
            <arg>-J--add-opens=jdk.compiler/com.sun.tools.javac.file=ALL-UNNAMED</arg>
            <arg>-J--add-opens=jdk.compiler/com.sun.tools.javac.main=ALL-UNNAMED</arg>
            <arg>-J--add-opens=jdk.compiler/com.sun.tools.javac.model=ALL-UNNAMED</arg>
            <arg>-J--add-opens=jdk.compiler/com.sun.tools.javac.parser=ALL-UNNAMED</arg>
            <arg>-J--add-opens=jdk.compiler/com.sun.tools.javac.processing=ALL-UNNAMED</arg>
            <arg>-J--add-opens=jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED</arg>
            <arg>-J--add-opens=jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED</arg>
            <arg>-J--add-opens=jdk.compiler/com.sun.tools.javac.jvm=ALL-UNNAMED</arg>
        </compilerArgs>
        <annotationProcessorPaths>
            <path>
                <groupId>org.projectlombok</groupId>
                <artifactId>lombok</artifactId>
                <version>${lombok.version}</version>
            </path>
        </annotationProcessorPaths>
    </configuration>
</plugin>
```
