package tools.jackson.module.scala.util;

/**
 * A Java class, so no .tasty file of its own is ever on the classpath. What answers for one in
 * TastyUtilTest is a classloader that made it up - which only works if the class's own loader is
 * the one being asked.
 */
public final class TastyProbe {
}
