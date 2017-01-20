package com.avbravo.jmoordb.anotations ;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
/**
*
* @author 
*/


@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.FIELD})
public @interface Referenced {
 String documment();
 String field();
 String javatype() default "String";
 String facade();
 boolean lazy() default false;
}

