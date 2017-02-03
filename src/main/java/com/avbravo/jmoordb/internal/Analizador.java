/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.avbravo.jmoordb.internal;

import com.avbravo.jmoordb.DatePatternBeans;
import com.avbravo.jmoordb.EmbeddedBeans;
import com.avbravo.jmoordb.FieldBeans;
import com.avbravo.jmoordb.PrimaryKey;
import com.avbravo.jmoordb.ReferencedBeans;
import com.avbravo.jmoordb.anotations.DatePattern;
import com.avbravo.jmoordb.anotations.Embedded;
import com.avbravo.jmoordb.anotations.Id;
import com.avbravo.jmoordb.anotations.Referenced;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author avbravo
 */
public class Analizador {

    List<PrimaryKey> primaryKeyList = new ArrayList<>();
    List<EmbeddedBeans> embeddedBeansList = new ArrayList<>();
    List<DatePatternBeans> datePatternBeansList = new ArrayList<>();
    List<FieldBeans> fieldBeansList = new ArrayList<>();
     List<ReferencedBeans> referencedBeansList = new ArrayList<>();

    Exception exception;

    public List<PrimaryKey> getPrimaryKeyList() {
        return primaryKeyList;
    }

    public void setPrimaryKeyList(List<PrimaryKey> primaryKeyList) {
        this.primaryKeyList = primaryKeyList;
    }

    public List<EmbeddedBeans> getEmbeddedBeansList() {
        return embeddedBeansList;
    }

    public void setEmbeddedBeansList(List<EmbeddedBeans> embeddedBeansList) {
        this.embeddedBeansList = embeddedBeansList;
    }

    public List<DatePatternBeans> getDatePatternBeansList() {
        return datePatternBeansList;
    }

    public void setDatePatternBeansList(List<DatePatternBeans> datePatternBeansList) {
        this.datePatternBeansList = datePatternBeansList;
    }

    public List<FieldBeans> getFieldBeansList() {
        return fieldBeansList;
    }

    public void setFieldBeansList(List<FieldBeans> fieldBeansList) {
        this.fieldBeansList = fieldBeansList;
    }

    public Exception getException() {
        return exception;
    }

    public void setException(Exception exception) {
        this.exception = exception;
    }

    public List<ReferencedBeans> getReferencedBeansList() {
        return referencedBeansList;
    }

    public void setReferencedBeansList(List<ReferencedBeans> referencedBeansList) {
        this.referencedBeansList = referencedBeansList;
    }

    public Analizador() {
         primaryKeyList = new ArrayList<>();
        embeddedBeansList = new ArrayList<>();
        referencedBeansList = new ArrayList<>();
        datePatternBeansList = new ArrayList<>();
        fieldBeansList = new ArrayList<>();
    }
    
    
    public Boolean analizar(Field[] fields) {
        try {

            for (final Field field : fields) {
                Annotation anotacion = field.getAnnotation(Id.class);
                Annotation anotacionEmbedded = field.getAnnotation(Embedded.class);
                Annotation anotacionReferenced = field.getAnnotation(Referenced.class);
                Annotation anotacionDateFormat = field.getAnnotation(DatePattern.class);

                Embedded embedded = field.getAnnotation(Embedded.class);
                Referenced referenced = field.getAnnotation(Referenced.class);
                DatePattern datePattern = field.getAnnotation(DatePattern.class);

                field.setAccessible(true);
                FieldBeans fieldBeans = new FieldBeans();
                fieldBeans.setIsKey(false);
                fieldBeans.setIsEmbedded(false);
                fieldBeans.setIsReferenced(false);
                fieldBeans.setName(field.getName());
                fieldBeans.setType(field.getType().getName());

                //PrimaryKey
                if (anotacion != null) {
                    verifyPrimaryKey(field, anotacion);
                    fieldBeans.setIsKey(true);

                }

                if (anotacionEmbedded != null) {

                    verifyEmbedded(field, anotacionEmbedded);
                    fieldBeans.setIsEmbedded(true);

                }

                if (anotacionReferenced != null) {

                    verifyReferenced(field, anotacionReferenced, referenced);
                    fieldBeans.setIsReferenced(true);

                }
                if (anotacionDateFormat != null) {

                    verifyDatePattern(field, anotacionReferenced, datePattern);
                    //fieldBeans.setIsReferenced(true);

                }

                fieldBeansList.add(fieldBeans);

            }
            //Llave primary
            if (primaryKeyList.isEmpty()) {
                exception = new Exception("No have primaryKey() ");

            }
            if (fieldBeansList.isEmpty()) {
                exception = new Exception("No have fields() ");
            }

        } catch (Exception e) {
            exception = new Exception("analizar() " + e.getLocalizedMessage());
        }
        return false;
    }

    /**
     *
     * @param variable
     * @param anotacion
     * @return
     */
    private Boolean verifyPrimaryKey(Field variable, Annotation anotacion) {
        try {
            final Id anotacionPK = (Id) anotacion;
            PrimaryKey primaryKey = new PrimaryKey();

            Boolean found = false;
            for (PrimaryKey pk : primaryKeyList) {
                if (pk.getName().equals(primaryKey.getName())) {
                    found = true;
                }
            }

            primaryKey.setName(variable.getName());
            primaryKey.setType(variable.getType().getName());

            // obtengo el valor del atributo
            if (!found) {
                primaryKeyList.add(primaryKey);
            }
            return true;
        } catch (Exception e) {
            System.out.println("verifyPrimaryKey() " + e.getLocalizedMessage());
        }
        return false;
    }
    
      private Boolean verifyEmbedded(Field variable, Annotation anotacion) {
        try {
            // final Embedded anotacionPK = (Embedded) anotacionEmbedded;
            EmbeddedBeans embeddedBeans = new EmbeddedBeans();
            embeddedBeans.setName(variable.getName());
            embeddedBeans.setType(variable.getType().getName());
            embeddedBeansList.add(embeddedBeans);
            return true;
        } catch (Exception e) {
            System.out.println("verifyEmbedded() " + e.getLocalizedMessage());
        }
        return false;
    }

    /**
     * guarda la informacion de la anotacion
     *
     * @param variable
     * @param anotacion
     * @param referenced
     * @return
     */
    private Boolean verifyReferenced(Field variable, Annotation anotacion, Referenced referenced) {
        try {

            ReferencedBeans referencedBeans = new ReferencedBeans();
            referencedBeans.setName(variable.getName());
            referencedBeans.setType(variable.getType().getName());
            referencedBeans.setDocument(referenced.documment());
            referencedBeans.setField(referenced.field());
            referencedBeans.setJavatype(referenced.javatype());
            referencedBeans.setFacade(referenced.facade());
            referencedBeans.setLazy(referenced.lazy());

            referencedBeansList.add(referencedBeans);
            return true;
        } catch (Exception e) {
            System.out.println("verifyReferenced() " + e.getLocalizedMessage());
        }
        return false;
    }

    /**
     *
     *
     * @param variable
     * @param anotacion
     * @param referenced
     * @return
     */
    private Boolean verifyDatePattern(Field variable, Annotation anotacion, DatePattern datePattern) {
        try {

            DatePatternBeans datePatternBeans = new DatePatternBeans();
            datePatternBeans.setName(variable.getName());
            datePatternBeans.setType(variable.getType().getName());
            datePatternBeans.setDateformat(datePattern.dateformat());

            datePatternBeansList.add(datePatternBeans);
            return true;
        } catch (Exception e) {
            System.out.println("verifyReferenced() " + e.getLocalizedMessage());
        }
        return false;
    }

}
