package is.hello.buruberi.util;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks an API component as depending on behavior that has been stable
 * between Android versions, and different vendors, but is not guaranteed
 * to always be available. Code that uses components marked with this
 * annotation should gracefully degrade if the components become unavailable.
 */
@Target({ElementType.METHOD, ElementType.FIELD, ElementType.TYPE})
@Retention(RetentionPolicy.SOURCE)
@Inherited
@Documented
public @interface NonGuaranteed {
}
