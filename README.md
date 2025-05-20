
# 💬 Chat TCP Multicliente – Java

Este proyecto implementa un sistema cliente-servidor basado en **TCP** que permite:

- Conectarse como cliente y registrarse con un nombre.
- Enviar mensajes privados a otros usuarios conectados.
- Crear grupos de chat.
- Enviar mensajes a todos los miembros de un grupo.

---

## 📁 Estructura del proyecto

```
ChatTCP/
│
├── server/
│   ├── Server.java
│   └── ClientHandler.java
│
├── client/
│   └── Client.java
│
├── README.md
└── run.sh / run.bat (opcional)
```

---

## ⚙️ Requisitos

- Java JDK 8 o superior  
- Editor de código como VSCode o IntelliJ  
- Compilador `javac` y ejecutor `java` configurados en tu `PATH`

---

## ▶️ Instrucciones para ejecutar

### 1. Compilar todos los archivos

Desde la raíz del proyecto:

```bash
javac -d bin Server/*.java 
javac -d bin Client/*.java
```

---

### 2. Ejecutar el servidor (Desde la raiz del proyecto)

En una terminal nueva :

```bash
java -cp bin Server.Server
```

Deberías ver:

```
Servidor TCP iniciado en el puerto 6789...
```

---

### 3. Ejecutar un cliente (Desde la raiz del proyecto)

En una terminal distinta (puedes abrir varias):

```bash
java -cp bin Client.Client
```

Se te pedirá ingresar tu nombre:

```
Ingresa tu nombre:
> camilo
```

---

## 🧑‍💻 Menú del cliente

Una vez conectado, verás este menú:

```
MENÚ:
1. Enviar mensaje a usuario
2. Crear grupo
3. Enviar mensaje a grupo
4. Salir
Elige opción:
```

### Opción 1 – Enviar mensaje a otro usuario

- Ingresa el nombre del destinatario.
- Luego el mensaje.

### Opción 2 – Crear grupo

- Ingresa el nombre del grupo.
- Te unes automáticamente como miembro.

### Opción 3 – Enviar mensaje a grupo

- Ingresa el nombre del grupo.
- El mensaje se enviará a todos sus miembros (menos tú).

---

## 🧪 Consejos de prueba

- Ejecuta múltiples clientes (en terminales distintas).
- Usa nombres diferentes para cada cliente.
- Prueba enviar mensajes privados o crear y usar grupos.

---


