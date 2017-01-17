# jmoordb
Java Mapper for MongoDB and OrientDB

Es un Framework para integrar MongoDB/OrientDB con las aplicaciones Java de una manera sencilla.

Soporta documentos embebidos mediante la anotación @Embedded

Soporta relaciones mediante la anotación @Referenced

#Entity
@Getter

@Setter

public class Paises {

  @Id
  
  private String idpais;
  
  private String pais;
  
  @Embedded
  
  private Ubicacion ubicacion;
  
  @Referenced(document="Continentes",field="idcontinente)
  
  private Continentes continentes;
  
}

#Facade
Las operaciones CRUD se implementan atraves de un Facade.
   paisesFacade.save(paises);
   
   

