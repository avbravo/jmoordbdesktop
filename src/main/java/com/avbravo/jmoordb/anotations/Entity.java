package com.avbravo.jmoordb.anotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 *
 * @author
 */

@Retention(RetentionPolicy.RUNTIME)
public @interface Entity {

    String document();

    String dateformat() default ("dd/MM/yyyy HH:mm:ss a");

    boolean lazy() default true;
}
