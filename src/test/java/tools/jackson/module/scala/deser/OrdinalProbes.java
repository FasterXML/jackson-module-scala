package tools.jackson.module.scala.deser;

import java.util.NoSuchElementException;

/**
 * Stand-ins for the shapes EnumDeserializerShared reflects on. Written in Java so that the static
 * methods it looks for - fromOrdinal, values - can be given behaviour no real Scala 3 enum would
 * have, which is the behaviour the handling around those calls exists for.
 */
public final class OrdinalProbes {

    private OrdinalProbes() {
    }

    /** fromOrdinal fails the ordinary way: there is no case at that ordinal. */
    public static final class Absent {
        public static Object fromOrdinal(int ordinal) {
            throw new NoSuchElementException(String.valueOf(ordinal));
        }
    }

    /** fromOrdinal fails with an error that belongs to the caller rather than to the question. */
    public static final class Fatal {
        public static Object fromOrdinal(int ordinal) {
            throw new OutOfMemoryError("thrown by the enum, not by the JVM");
        }
    }

    /** fromOrdinal fails by interrupting, which is also the caller's business. */
    public static final class Interrupting {
        public static Object fromOrdinal(int ordinal) throws InterruptedException {
            throw new InterruptedException("thrown by the enum");
        }
    }

    /** fromOrdinal answers for every ordinal there is, so walking them never reaches an end. */
    public static final class Unbounded {
        public static Object fromOrdinal(int ordinal) {
            return "case" + ordinal;
        }

        public static Object[] values() {
            return new Object[] { "case0", "case1" };
        }
    }
}
