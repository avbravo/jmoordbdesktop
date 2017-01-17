# jmoordb
Java Mapper for MongoDB and OrientDB

Es un Framework para integrar MongoDB/OrientDB con las aplicaciones Java de una manera sencilla.

Sintaxis similiar a JPA

##Soporta
 Documentos embebidos mediante la anotación @Embedded

  Documentos relacionados mediante la anotación @Referenced

#Entity
@Getter

@Setter

public class Paises {

  @Id
  
  private String idpais;
  
  private String pais;
  
  @Embedded
  
  private Planetas planetas;
  
  @Referenced(document="Continentes",field="idcontinente)
  
  private Continentes continentes;
  
}

#Facade
Las operaciones CRUD se implementan atraves de un Facade.

   paisesFacade.save(paises);
   
   

