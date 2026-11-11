# 🎓 GUÍA DE DEMOSTRACIÓN PRÁCTICA EN VIVO (DOCENTE)
## Clase: S10 | M10: Despliegue de Backend Spring WebFlux y MongoDB en Kubernetes

> [!IMPORTANT]
> **Propósito de este documento:** Este material **NO** es una tarea para el estudiante. Es el **guion técnico, pedagógico y paso a paso** que el **DOCENTE** ejecutará en vivo frente a la clase para demostrar visualmente cómo desplegar y conectar un microservicio reactivo en Spring WebFlux con MongoDB sobre Kubernetes.

---

## 1. ⏱️ GUION Y TIEMPOS DE LA DEMOSTRACIÓN (Overview — 40 Minutos)

| Bloque | Tiempo | Actividad en Pantalla | Punto de Inflexión Pedagógico (Pausa del Profesor) |
| :--- | :---: | :--- | :--- |
| **Bloque 1: Contexto y Arquitectura** | `00:00 - 06:00` (6 min) | Mostrar el diagrama de arquitectura en [`README.md`](file:///c:/Users/jean.carlos.simon_bl/Documents/Valle%20Grande/S10-M10-Despliegue-Spring-WebFlux-y-MongoDB/README.md) y revisar [`application.yml`](file:///c:/Users/jean.carlos.simon_bl/Documents/Valle%20Grande/S10-M10-Despliegue-Spring-WebFlux-y-MongoDB/src/main/resources/application.yml). | **Pausa 1:** ¿Por qué no debemos quemar (`hardcode`) las credenciales ni la IP de MongoDB dentro del `application.yml`? (Principio III de 12-Factor App). |
| **Bloque 2: Aislamiento y Configuración** | `06:00 - 15:00` (9 min) | Crear el Namespace `practica-s10`, aplicar [`01-app-configmap.yaml`](file:///c:/Users/jean.carlos.simon_bl/Documents/Valle%20Grande/S10-M10-Despliegue-Spring-WebFlux-y-MongoDB/k8s/01-app-configmap.yaml) y [`02-db-secret.yaml`](file:///c:/Users/jean.carlos.simon_bl/Documents/Valle%20Grande/S10-M10-Despliegue-Spring-WebFlux-y-MongoDB/k8s/02-db-secret.yaml). | **Pausa 2:** Diferencia crítica entre `ConfigMap` (datos de topología) y `Secret` (datos sensibles en Base64). Aclarar que Base64 es codificación, no cifrado criptográfico. |
| **Bloque 3: Capa de Datos NoSQL (MongoDB)** | `15:00 - 23:00` (8 min) | Desplegar [`03-mongo-stack.yaml`](file:///c:/Users/jean.carlos.simon_bl/Documents/Valle%20Grande/S10-M10-Despliegue-Spring-WebFlux-y-MongoDB/k8s/03-mongo-stack.yaml) y verificar el Service `mongo-service`. | **Pausa 3:** ¿Por qué MongoDB usa `ClusterIP` y jamás `NodePort` o `LoadBalancer`? Explicar seguridad por aislamiento de red en el clúster. |
| **Bloque 4: Capa Reactiva (Spring WebFlux)** | `23:00 - 31:00` (8 min) | Desplegar [`04-backend-stack.yaml`](file:///c:/Users/jean.carlos.simon_bl/Documents/Valle%20Grande/S10-M10-Despliegue-Spring-WebFlux-y-MongoDB/k8s/04-backend-stack.yaml) y mostrar la inyección con `envFrom`. | **Pausa 4:** Cómo CoreDNS resuelve `DB_HOST: mongo-service` y cómo Netty levanta sin bloquear hilos conectándose reactivamente a MongoDB. |
| **Bloque 5: Validación en Vivo y Cierre** | `31:00 - 40:00` (9 min) | Ejecutar las 3 pruebas en vivo (`kubectl get`, `kubectl exec -- env`, y peticiones HTTP al `NodePort 30080`). | **Pausa 5:** Realizar las 3 preguntas de retroalimentación para comprobar que el grupo comprendió el flujo completo. |

---

## 2. 📖 ESCENARIO DE DEMOSTRACIÓN (La Historia)

### Narrativa para iniciar la sesión:
> *"Imaginen que somos el equipo de Arquitectura Cloud e Ingeniería DevSecOps de una plataforma de comercio electrónico de alto tráfico. Durante campañas como Cyber Wow, recibimos miles de consultas por segundo al catálogo de productos.*
>
> *Si desplegáramos un backend tradicional bloqueante (Spring MVC con Tomcat), cada petición ocuparía un hilo esperando a la base de datos y los contenedores colapsarían por memoria. Por eso hoy desplegaremos un **microservicio 100% reactivo con Spring WebFlux sobre Netty** conectado al **driver reactivo de MongoDB**.*
>
> *Pero hay un reto adicional de seguridad: el equipo de Auditoría nos prohíbe subir contraseñas a GitHub o exponer la base de datos a Internet. Por lo tanto, antes de lanzar cualquier contenedor en Kubernetes, vamos a externalizar las rutas públicas en un **ConfigMap**, proteger las credenciales en un **Secret**, blindar MongoDB con un servicio interno **ClusterIP**, y exponer únicamente nuestro backend WebFlux mediante un **NodePort**."*

---

## 3. 🛠️ PASO A PASO TÉCNICO PARA EL PROFESOR (Con Código Explicado)

### Paso 1: Creación del Namespace (`practica-s10`) para Aislamiento

**Comando a ejecutar en terminal:**
```bash
kubectl create namespace practica-s10
kubectl get ns practica-s10
```

🗣️ **Nota docente (Qué decir en voz alta):**
> *"Nunca despleguemos cargas de trabajo en el namespace `default` en un entorno real. Al crear `practica-s10`, encapsulamos nuestros Pods, Services, ConfigMaps y Secrets en una burbuja lógica. Si al terminar la clase borramos este namespace, el clúster queda completamente limpio."*

---

### Paso 2: Creación del ConfigMap (`01-app-configmap.yaml`)

Mostrar en el editor el archivo [`k8s/01-app-configmap.yaml`](file:///c:/Users/jean.carlos.simon_bl/Documents/Valle%20Grande/S10-M10-Despliegue-Spring-WebFlux-y-MongoDB/k8s/01-app-configmap.yaml):

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

**Comando a ejecutar en terminal:**
```bash
kubectl apply -f k8s/01-app-configmap.yaml
```

🗣️ **Nota docente (Qué decir en voz alta):**
> *"Observen las 4 variables aquí: `DB_HOST`, `DB_PORT`, `DB_NAME` y `APP_PORT`. ¿Por qué van en un `ConfigMap` y no en un `Secret`? Porque son parámetros de configuración de infraestructura no sensibles: saber que la aplicación escucha en el puerto `8080` o que el servicio interno se llama `mongo-service` no compromete la seguridad. Además, mantenerlos en texto plano facilita que cualquier ingeniero de operaciones audite a qué host apunta el microservicio sin tener que decodificar nada."*

---

### Paso 3: Creación del Secret (`02-db-secret.yaml`)

Antes de aplicar el archivo, **demostrar en vivo en la terminal** cómo se genera el Base64:

```bash
echo -n 'admin' | base64
# Salida: YWRtaW4=

echo -n 'secret123' | base64
# Salida: c2VjcmV0MTIz
```

*(Nota para el profesor si usa PowerShell en Windows)*:
```powershell
[Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes("admin"))
[Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes("secret123"))
```

Mostrar en pantalla [`k8s/02-db-secret.yaml`](file:///c:/Users/jean.carlos.simon_bl/Documents/Valle%20Grande/S10-M10-Despliegue-Spring-WebFlux-y-MongoDB/k8s/02-db-secret.yaml):

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

**Comando a ejecutar en terminal:**
```bash
kubectl apply -f k8s/02-db-secret.yaml
```

🗣️ **Nota docente (Qué decir en voz alta):**
> *"Fíjense por qué usamos `-n` en `echo -n 'admin' | base64`: si omitimos `-n`, `echo` agrega un salto de línea invisible (`\n`) al final de la contraseña y MongoDB rechazará la autenticación. Además, recuerden: cuando Kubernetes inyecte este Secret dentro del Pod de MongoDB y dentro del Pod de Spring WebFlux, Kubernetes lo decodificará automáticamente a texto plano en la memoria del contenedor."*

---

### Paso 4: Despliegue del Stack MongoDB (`03-mongo-stack.yaml`)

Mostrar en pantalla [`k8s/03-mongo-stack.yaml`](file:///c:/Users/jean.carlos.simon_bl/Documents/Valle%20Grande/S10-M10-Despliegue-Spring-WebFlux-y-MongoDB/k8s/03-mongo-stack.yaml):

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: mongo-deployment
  namespace: practica-s10
  labels:
    app: mongo-db
    tier: database
spec:
  replicas: 1
  selector:
    matchLabels:
      app: mongo-db
      tier: database
  template:
    metadata:
      labels:
        app: mongo-db
        tier: database
    spec:
      containers:
        - name: mongodb
          image: mongo:7.0
          imagePullPolicy: IfNotPresent
          ports:
            - containerPort: 27017
              name: mongo-port
          envFrom:
            - secretRef:
                name: db-secret
          resources:
            requests:
              memory: "256Mi"
              cpu: "200m"
            limits:
              memory: "512Mi"
              cpu: "500m"
          volumeMounts:
            - name: mongo-storage
              mountPath: /data/db
      volumes:
        - name: mongo-storage
          emptyDir: {}
---
apiVersion: v1
kind: Service
metadata:
  name: mongo-service
  namespace: practica-s10
  labels:
    app: mongo-db
    tier: database
spec:
  type: ClusterIP
  selector:
    app: mongo-db
    tier: database
  ports:
    - protocol: TCP
      port: 27017
      targetPort: 27017
      name: tcp-mongo
```

**Comando a ejecutar en terminal:**
```bash
kubectl apply -f k8s/03-mongo-stack.yaml
kubectl get pods,svc -l app=mongo-db -n practica-s10
```

🗣️ **Nota docente (Qué decir en voz alta):**
> *"Presten mucha atención a dos detalles arquitectónicos:*
> *1. El `Service` se llama exactamente `mongo-service`, que coincide letra por letra con `DB_HOST: "mongo-service"` que pusimos en el ConfigMap.*
> *2. El tipo del Service es **`ClusterIP`**. ¿Por qué no `NodePort`? Porque una base de datos jamás debe exponerse fuera del clúster. Con `ClusterIP`, solo los Pods que viven dentro de nuestro clúster Kubernetes pueden hablar con MongoDB en el puerto `27017`. Cualquier intento de conexión directa desde el exterior es bloqueado por diseño."*

---

### Paso 5: Despliegue del Backend Spring WebFlux (`04-backend-stack.yaml`)

Mostrar en pantalla [`k8s/04-backend-stack.yaml`](file:///c:/Users/jean.carlos.simon_bl/Documents/Valle%20Grande/S10-M10-Despliegue-Spring-WebFlux-y-MongoDB/k8s/04-backend-stack.yaml):

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: backend-webflux-deployment
  namespace: practica-s10
  labels:
    app: backend-webflux
    tier: backend
spec:
  replicas: 2
  selector:
    matchLabels:
      app: backend-webflux
      tier: backend
  template:
    metadata:
      labels:
        app: backend-webflux
        tier: backend
    spec:
      containers:
        - name: spring-webflux-api
          image: vallegrande/webflux-mongo-k8s:1.0.0
          imagePullPolicy: IfNotPresent
          ports:
            - containerPort: 8080
              name: http-netty
          envFrom:
            - configMapRef:
                name: app-configmap
            - secretRef:
                name: db-secret
          resources:
            requests:
              memory: "256Mi"
              cpu: "250m"
            limits:
              memory: "512Mi"
              cpu: "500m"
          readinessProbe:
            httpGet:
              path: /actuator/health/readiness
              port: 8080
            initialDelaySeconds: 15
            periodSeconds: 10
          livenessProbe:
            httpGet:
              path: /actuator/health/liveness
              port: 8080
            initialDelaySeconds: 30
            periodSeconds: 15
---
apiVersion: v1
kind: Service
metadata:
  name: backend-webflux-service
  namespace: practica-s10
  labels:
    app: backend-webflux
    tier: backend
spec:
  type: NodePort
  selector:
    app: backend-webflux
    tier: backend
  ports:
    - protocol: TCP
      port: 8080
      targetPort: 8080
      nodePort: 30080
      name: http-webflux
```

**Comando a ejecutar en terminal:**
```bash
kubectl apply -f k8s/04-backend-stack.yaml
```

🗣️ **Nota docente (Qué decir en voz alta):**
> *"Vean el bloque `envFrom` del contenedor `spring-webflux-api`: con solo cuatro líneas le decimos a Kubernetes que tome todas las llaves del `app-configmap` y del `db-secret` y las inyecte como variables de entorno del sistema operativo dentro del contenedor.*
>
> *Cuando la JVM de Spring Boot arranca, lee su archivo `application.yml`, reemplaza `${DB_HOST}`, `${DB_NAME}`, `${MONGO_INITDB_ROOT_USERNAME}` y `${MONGO_INITDB_ROOT_PASSWORD}` en memoria, y abre el pool de conexiones reactivas no bloqueantes hacia MongoDB. Y gracias al `Service` tipo `NodePort`, abrimos el puerto `30080` para poder consumir el API desde nuestro navegador o Postman."*

---

## 4. 🧪 MOMENTOS DE VALIDACIÓN EN VIVO (¿Cómo demostramos que funciona?)

### Prueba 1: Salud Integral del Clúster

Ejecutar frente a los alumnos:
```bash
kubectl get pods,svc,configmaps,secrets -n practica-s10
```

**Lo que el profesor debe señalar en pantalla:**
- `pod/mongo-deployment-xxx` en estado `1/1 Running`.
- Las 2 réplicas de `pod/backend-webflux-deployment-xxx` en estado `1/1 Running`.
- `service/mongo-service` como `ClusterIP` con `<none>` en `EXTERNAL-IP` y puerto `27017/TCP`.
- `service/backend-webflux-service` como `NodePort` mapeando `8080:30080/TCP`.

---

### Prueba 2: Verificación de Inyección dentro del Contenedor (`kubectl exec`)

Ejecutar en vivo para demostrar que las variables existen dentro del Pod del backend:

```bash
# Obtener el nombre del primer Pod del backend
kubectl get pods -n practica-s10 -l app=backend-webflux

# Reemplazar <pod-backend> con el nombre real mostrado arriba
kubectl exec -it <pod-backend> -n practica-s10 -- env
```

**Lo que el profesor debe señalar en pantalla:**
- Resaltar con el cursor las líneas `DB_HOST=mongo-service`, `DB_NAME=vallegrande_reactive_db`, `APP_PORT=8080`, `MONGO_INITDB_ROOT_USERNAME=admin` y `MONGO_INITDB_ROOT_PASSWORD=secret123`.
- Mostrar cómo el valor que en el YAML estaba en Base64 (`YWRtaW4=`) ahora aparece decodificado (`admin`) dentro de la memoria del contenedor.

---

### Prueba 3: Demostración Reactiva desde el Navegador / Postman / Terminal

1. **Consultar el catálogo de productos (`Flux<Product>` desde MongoDB):**
   Abrir en el navegador o Postman:
   ```text
   http://localhost:30080/api/v1/products
   ```
   Mostrar el arreglo JSON con los 4 productos que el microservicio insertó reactivamente al iniciar.

2. **Demostrar la inyección y el Event Loop de Netty (`/info`):**
   Abrir en el navegador o ejecutar con `curl`:
   ```text
   http://localhost:30080/api/v1/products/info
   ```
   Señalar el campo `"reactiveEventLoopThread": "reactor-http-epoll-..."` y refrescar varias veces para que los alumnos vean cómo `"podHostname"` alterna entre los 2 Pods del Deployment gracias al balanceo de carga del Service de Kubernetes.

3. **Demostrar el Streaming Reactivo No Bloqueante (Server-Sent Events):**
   Abrir en el navegador o ejecutar:
   ```bash
   curl -N http://localhost:30080/api/v1/products/stream
   ```
   Observar en vivo cómo los documentos llegan uno por segundo como un flujo continuo (`Flux`) sin bloquear el hilo de Netty.

---

## 5. 🎯 CONCLUSIÓN Y PREGUNTAS DE RETROALIMENTACIÓN

Al finalizar la demostración práctica, formular estas **3 preguntas clave** a la clase para validar el aprendizaje:

### Pregunta 1: Sobre Seguridad de Red en Kubernetes
> **❓ Pregunta para la clase:** *"¿Qué pasaría con la seguridad de nuestra arquitectura si en el archivo `03-mongo-stack.yaml` hubiéramos configurado el Service de MongoDB como `type: NodePort` en lugar de `type: ClusterIP`?"*
- **✅ Respuesta esperada del alumno:** Kubernetes abriría un puerto en todos los nodos del clúster exponiendo el puerto `27017` de MongoDB hacia la red externa, rompiendo el aislamiento de capa de datos y exponiendo la base de datos a ataques de fuerza bruta o accesos no autorizados.

### Pregunta 2: Sobre Desacoplamiento y ConfigMaps
> **❓ Pregunta para la clase:** *"Si mañana decidimos cambiar el nombre de la base de datos de `vallegrande_reactive_db` a `produccion_db`, ¿necesitamos recompilar el código Java con Maven y volver a construir la imagen Docker?"*
- **✅ Respuesta esperada del alumno:** No. Gracias a que externalizamos `DB_NAME` en el `01-app-configmap.yaml` e inyectamos las variables con `envFrom`, solo debemos editar el `ConfigMap` en Kubernetes y reiniciar los Pods (`kubectl rollout restart deployment backend-webflux-deployment -n practica-s10`) usando exactamente la misma imagen Docker.

### Pregunta 3: Sobre el Paradigma Reactivo End-to-End
> **❓ Pregunta para la clase:** *"¿Por qué no basta con poner `@RestController` de Spring WebFlux si seguimos usando el driver tradicional bloqueante de base de datos en lugar de `spring-data-mongodb-reactive`?"*
- **✅ Respuesta esperada del alumno:** Porque Spring WebFlux utiliza un número muy pequeño de hilos en su *Event Loop* (Netty). Si el driver de base de datos bloquea el hilo esperando respuesta de red, los pocos hilos del Event Loop se congelan y todo el microservicio deja de responder ante nuevas peticiones concurrentes.

