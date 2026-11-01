# Restaurante — proyecto Java en evolución

Proyecto académico de **estructuras de datos** desarrollado en Java y Maven. La versión actual es una **demostración de consola**, no un sistema de restaurante listo para producción.

## Qué hace hoy

- Muestra un catálogo de dos platillos de ejemplo.
- Muestra empleados almacenados en una `Stack`.
- Muestra tres órdenes pendientes almacenadas en una `Queue`.
- Ejecuta una prueba automática que comprueba que los platillos no se duplican y se muestran las tres órdenes.

**Limitaciones actuales:** los datos están definidos en el código y se pierden al cerrar la aplicación. No hay registro de ventas, cobro, inventario, usuarios, interfaz web ni base de datos. Por el momento, la clase `Proveedor` representa platillos; su nombre se conserva para no realizar un cambio de dominio apresurado.

## Requisitos

- JDK **17 o posterior**.
- **Apache Maven**.

## Ejecutar

Desde la raíz del repositorio:

```bash
mvn -f Restaurante/pom.xml clean verify
java -cp Restaurante/target/classes com.demo.restaurante.Restaurante
```

La primera instrucción compila y ejecuta las pruebas con JUnit 5. La segunda muestra el resultado en consola.

## Estructura

```text
Restaurante/
├── pom.xml
└── src/
    ├── main/java/com/demo/restaurante/
    │   ├── Empleado.java
    │   ├── Orden.java
    │   ├── Proveedor.java
    │   └── Restaurante.java
    └── test/java/com/demo/restaurante/
        └── RestauranteTest.java
```

## Próximas fases previstas

1. Definir correctamente las entidades del dominio: platillo, empleado y pedido.
2. Incorporar operaciones de negocio reales: crear pedidos, calcular totales y gestionar estados.
3. Añadir persistencia y una interfaz de usuario, una vez definido el alcance de la aplicación.
4. Ampliar las pruebas, documentación y capturas para el portafolio.

> La preparación de portafolio busca mostrar una evolución técnica comprobable, sin presentar funcionalidades todavía inexistentes.
