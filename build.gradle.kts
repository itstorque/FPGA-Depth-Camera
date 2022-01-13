plugins {
    id("io.verik.mit.verik-6111-plugin") version "local-SNAPSHOT"
}

repositories {
    mavenLocal()
    mavenCentral()
}

verik6111 {
    part = "xc7a100tcsg324-3"
    synthesisTop = "FpgaTop"
}
