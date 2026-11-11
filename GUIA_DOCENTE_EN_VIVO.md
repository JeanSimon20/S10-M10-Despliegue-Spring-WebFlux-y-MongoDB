# Apuntes de Clase y Guion de Demostración
## S10 | M10: Despliegue de Backend Spring WebFlux y MongoDB en Kubernetes

## 1. Distribución de Tiempos (40 minutos)

| Bloque | Tiempo estimado | Actividad | Qué recalcar en la pausa |
| :--- | :---: | :--- | :--- |
| **1. Contexto y arquitectura** | 00 - 06 min | Revisar el esquema del proyecto y el archivo `application.yml`. | Por qué nunca dejamos IPs ni contraseñas fijas dentro del código fuente. |
| **2. Namespace, ConfigMap y Secret** | 06 - 15 min | Crear `practica-s10`, aplicar `01-app-configmap.yaml` y `02-db-secret.yaml`. | Diferencia entre configuración pública (`ConfigMap`) y credenciales en Base64 (`Secret`). |
| **3. Base de datos MongoDB** | 15 - 23 min | Aplicar `03-mongo-stack.yaml` y revisar el Service creado. | Por qué la base de datos usa `ClusterIP` y no debe exponerse hacia afuera. |
| **4. Backend Spring WebFlux** | 23 - 31 min | Aplicar `04-backend-stack.yaml` con 2 réplicas e inyección `envFrom`. | Cómo el DNS interno de Kubernetes resuelve `mongo-service` y cómo trabaja el Event Loop de Netty. |
| **5. Pruebas y cierre** | 31 - 40 min | Validar con `kubectl get`, `kubectl exec` y peticiones al puerto `30080`. | Preguntas finales para comprobar que quedó claro el flujo completo. |

---

## 2. Escenario de la Demostración

Vamos a desplegar un microservicio de catálogo de productos hecho con **Spring WebFlux** que guarda y consulta información en **MongoDB** usando su driver reactivo no bloqueante.

El objetivo de la sesión es ver en la práctica dos cosas:
1. Cómo un servicio reactivo procesa peticiones sobre hilos de Event Loop (`reactor-http-epoll`) sin quedarse bloqueado esperando a la base de datos.
2. Cómo separamos la configuración (`ConfigMap`) y los accesos (`Secret`) antes de levantar los Pods, manteniendo la base de datos cerrada al exterior (`ClusterIP`) y exponiendo solo el API (`NodePort`).

---

## 3. Secuencia de Comandos y Explicación en Clase

### Paso 1: Crear el Namespace (`practica-s10`)

```bash
kubectl create namespace practica-s10
kubectl get ns practica-s10
```

**Comentario para la clase:**
En lugar de mezclar todo en el namespace `default`, creamos `practica-s10` para tener aislados los recursos de esta sesión. Si al final queremos limpiar el laboratorio, basta con eliminar el namespace.

---

### Paso 2: Crear el ConfigMap (`01-app-configmap.yaml`)

Archivo `k8s/01-app-configmap.yaml`:

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: app-configmap
  namespace: practica-s10
  labels:
    app: webflux-mongo-stack
    component: configuration
    tier: backend
data:
  DB_HOST: "mongo-service"
  DB_PORT: "27017"
  DB_NAME: "vallegrande_reactive_db"
  APP_PORT: "8080"
```

```bash
kubectl apply -f k8s/01-app-configmap.yaml
```

**Comentario para la clase:**
Aquí colocamos `DB_HOST`, `DB_PORT`, `DB_NAME` y `APP_PORT` porque son datos de configuración que no son secretos. Cualquier miembro del equipo puede ver a qué puerto o nombre de servicio apunta el backend sin comprometer la seguridad.

---

### Paso 3: Crear el Secret (`02-db-secret.yaml`)

Antes de aplicar el manifiesto, mostrar en la terminal cómo obtener los valores en Base64:

```bash
echo -n 'admin' | base64
# YWRtaW4=

echo -n 'secret123' | base64
# c2VjcmV0MTIz
```

*(Si se ejecuta desde PowerShell en Windows)*:
```powershell
[Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes("admin"))
[Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes("secret123"))
```

Archivo `k8s/02-db-secret.yaml`:

```yaml
apiVersion: v1
kind: Secret
metadata:
  name: db-secret
  namespace: practica-s10
  labels:
    app: webflux-mongo-stack
    component: security
    tier: database
type: Opaque
data:
  MONGO_INITDB_ROOT_USERNAME: YWRtaW4=
  MONGO_INITDB_ROOT_PASSWORD: c2VjcmV0MTIz
```

```bash
kubectl apply -f k8s/02-db-secret.yaml
```

**Comentario para la clase:**
Es importante usar el parámetro `-n` en `echo -n` para no meter un salto de línea al final de la contraseña. Cuando Kubernetes inyecte este Secret en los contenedores, lo decodificará automáticamente y las aplicaciones lo verán como texto normal en sus variables de entorno.

---

### Paso 4: Desplegar MongoDB (`03-mongo-stack.yaml`)

```bash
kubectl apply -f k8s/03-mongo-stack.yaml
kubectl get pods,svc -l app=mongo-db -n practica-s10
```

**Comentario para la clase:**
Fíjense que el Service se llama `mongo-service`, exactamente igual que el valor que pusimos en `DB_HOST` dentro del ConfigMap. Además, es de tipo `ClusterIP`: eso asegura que el puerto `27017` solo sea alcanzable por otros Pods dentro del clúster y nunca desde afuera.

---

### Paso 5: Desplegar el Backend Spring WebFlux (`04-backend-stack.yaml`)

```bash
kubectl apply -f k8s/04-backend-stack.yaml
kubectl get pods -n practica-s10 -w
```

**Comentario para la clase:**
En este Deployment usamos `envFrom` para cargar de una sola vez todas las llaves del `app-configmap` y del `db-secret`. Al iniciar Spring Boot, el archivo `application.yml` toma esas variables del entorno, arma la URI de conexión hacia `mongo-service:27017` y levanta Netty en el puerto `8080`, expuesto hacia nuestra máquina mediante el `NodePort 30080`.

---

## 4. Validaciones en Vivo

### Prueba 1: Revisar el estado general en el namespace
```bash
kubectl get pods,svc,configmaps,secrets -n practica-s10
```
Verificar que el Pod de MongoDB y las 2 réplicas del backend estén en estado `Running`.

### Prueba 2: Comprobar las variables dentro del contenedor
```bash
kubectl get pods -n practica-s10 -l app=backend-webflux
kubectl exec -it <pod-backend> -n practica-s10 -- env
```
Mostrar en la salida de la terminal que `DB_HOST`, `DB_NAME`, `APP_PORT`, `MONGO_INITDB_ROOT_USERNAME` y `MONGO_INITDB_ROOT_PASSWORD` están inyectadas dentro del Pod.

### Prueba 3: Peticiones HTTP desde el navegador o Postman
1. **Ver catálogo inicial cargado en MongoDB:**
   Abrir `http://localhost:30080/api/v1/products`
2. **Ver variables inyectadas, hilo de Netty y balanceo entre Pods:**
   Abrir `http://localhost:30080/api/v1/products/info` y recargar un par de veces para notar cómo cambia `podHostname` entre las dos réplicas.
3. **Probar el flujo continuo (Server-Sent Events):**
   Abrir `http://localhost:30080/api/v1/products/stream` o ejecutar `curl -N http://localhost:30080/api/v1/products/stream`.

---

## 5. Preguntas de Cierre para los Alumnos

1. **¿Qué problema de seguridad tendríamos si el Service de MongoDB (`03-mongo-stack.yaml`) fuera `NodePort` en lugar de `ClusterIP`?**
   *Idea clave:* Expondríamos el puerto `27017` de la base de datos fuera del clúster, permitiendo que cualquiera en la red intente conectarse directamente a MongoDB.

2. **Si necesitamos cambiar el nombre de la base de datos en otro ambiente, ¿hay que volver a compilar el proyecto Java y crear otra imagen Docker?**
   *Idea clave:* No, porque `DB_NAME` viene del `ConfigMap`. Solo se actualiza el `ConfigMap` y se reinician los Pods del Deployment usando la misma imagen.

3. **¿Por qué usamos `spring-data-mongodb-reactive` en lugar del driver tradicional de MongoDB cuando trabajamos con Spring WebFlux?**
   *Idea clave:* Porque WebFlux trabaja con pocos hilos en su Event Loop. Si el driver de base de datos fuera bloqueante, detendría esos hilos en cada consulta y se perdería toda la ventaja reactiva.
