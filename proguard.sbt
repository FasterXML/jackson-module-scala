proguardSettings

ProguardKeys.options in Proguard ++= Seq("-dontnote", "-dontwarn", "-ignorewarnings", "-printmapping")

ProguardKeys.options in Proguard += "-keep public class com.fasterxml.jackson.module.scala.**"

ProguardKeys.inputs in Proguard ~= { inputs => inputs.filter {
    case x if x.getAbsolutePath.contains("jackson.core") => false
    case x if x.getAbsolutePath.contains("jsr305") => false
    case x if x.getName == "scala-library.jar" => false
    case _ => true
} }