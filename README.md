# Scout26 (temporary working name)

This is a clean, Fabric 26.1.x wearable-bag mod bootstrap. It intentionally contains no bag logic, Trinkets dependency, or mixins yet.

The temporary identifiers are `scout26` and `com.example.scout26`. They are not release identifiers; the final public name, mod ID, Java package, and license remain open project decisions.

## Prerequisites

- Java 25 JDK
- Internet access for Gradle dependency resolution

## Build

```powershell
$env:JAVA_HOME = 'C:\\Program Files\\Microsoft\\jdk-25.0.4.7-hotspot'
$env:Path = "$env:JAVA_HOME\\bin;$env:Path"
.\\gradlew.bat clean build
```

