package com.avbravo.jmoordb.anotations ;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
/**
*
* @author avbravo
*/


@Retention(RetentionPolicy.RUNTIME)

public @interface Embedded {

String name() default "";


}
