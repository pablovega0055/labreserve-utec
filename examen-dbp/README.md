# LabReserve UTEC (CS2031 DBP)

API REST para registrar laboratorios, publicar turnos de equipos y reservarlos.
Spring Boot 3.5 + PostgreSQL (Docker) + JPA + validaciones + **seguridad JWT** + eventos `@Async` + tests.
Arquitectura **Controller → Service → Repository**, siempre con DTOs.

---

## 0. Arranque

```bash
docker compose up -d          # Postgres en Docker
docker ps                     # "examen-postgres" Up / healthy
```
Luego: `ExamenApplication.java` → ▶ Run. Tests: `mvn clean verify` (usan H2, no necesitan Docker).

Postman: **Import** → `postman/Examen-DBP.postman_collection.json`. Los requests de login guardan el token solos.

### Usuarios iniciales (los crea `config/DataInitializer` al arrancar)
| username | password | rol |
|---|---|---|
| `admin` | `admin12345` | ADMIN |
| `technician` | `tech12345` | TECHNICIAN |

`POST /auth/register` siempre crea usuarios **STUDENT**.

---

## 1. Entidades

| Entidad | Campos |
|---|---|
| `User` (tabla `users`) | id, username (único), email (único), password (BCrypt), role `STUDENT/TECHNICIAN/ADMIN` |
| `Laboratory` | id, name (único), location, manager → `manager_id`, status `ACTIVE/MAINTENANCE/CLOSED` |
| `EquipmentSlot` | id, laboratory, equipment (`equipment_code`), startTime, endTime, capacity, status `AVAILABLE/FULL/CANCELLED` |
| `LabReservation` | id, slot → `slot_id`, student → `student_id`, purpose, reservedAt, status `CONFIRMED/CANCELLED` |

## 2. Endpoints

Todo requiere `Authorization: Bearer <token>` salvo `/auth/**` y `/ping`.

| Método | Ruta | Rol | Qué hace | Códigos |
|---|---|---|---|---|
| POST | `/auth/register` | público | `{username,email,password}` → `{id,username,email}` | 201 / 400 / 409 |
| POST | `/auth/login` | público | `{username,password}` → `{token,expiresIn}` (segundos) | 200 / 400 / 401 |
| POST | `/laboratories` | ADMIN | `{name,location,managerId,status?}` | 201 / 400 / 404 / 409 |
| GET | `/laboratories?page=0&size=10` | cualquiera | lista paginada | 200 |
| GET | `/laboratories/{id}` | cualquiera | uno | 200 / 404 |
| POST | `/equipment-slots` | TECHNICIAN, ADMIN | publicar turno `{laboratoryId,equipmentCode,startTime,endTime,capacity}` | 201 / 400 / 404 / 409 |
| GET | `/equipment-slots?page=0&size=10&laboratoryId=&status=` | cualquiera | `{content:[{id,laboratoryName,equipmentCode,...}],page,size,totalElements}` | 200 |
| POST | `/reservations` | STUDENT | `{slotId,purpose}` (el estudiante sale del token) | 201 / 400 / 404 / 409 |
| GET | `/reservations/me` | cualquiera | mis reservas paginadas | 200 |
| PATCH | `/reservations/{id}/cancel` | dueño, TECHNICIAN, ADMIN | cancela y libera cupo | 200 / 400 / 403 / 404 |

Sin token o token inválido → **401**. Rol incorrecto → **403**.

### Reglas de negocio
- Password mínimo 8 caracteres, email con formato válido, username/email únicos (409).
- Turno: `endTime > startTime`, fechas futuras, capacity 1–50, laboratorio `ACTIVE`, sin cruce de horario para el mismo equipo del mismo laboratorio (409).
- Reserva: turno futuro y no cancelado, un estudiante no reserva dos veces el mismo turno (409), no se pasa de `capacity` (409, con lock pesimista). Al llenarse el turno pasa a `FULL`; al cancelar vuelve a `AVAILABLE`.
- Al reservar se publica `ReservationCreatedEvent` (listener `@Async` + `AFTER_COMMIT`, simula el email).

---

## 3. Estructura

```
src/main/java/com/utec/dbp/
├── ExamenApplication.java  @SpringBootApplication + @EnableAsync
├── config/       DataInitializer (usuarios admin/technician)
├── security/     JwtService, JwtAuthorizationFilter, SecurityConfig, UserDetailsServiceImpl
├── model/        Entidades JPA y enums
├── repository/   JpaRepository (query methods, @EntityGraph, @Lock)
├── dto/          records Request (validaciones) / Response + PagedResponse
├── service/      lógica de negocio (@Transactional)
├── controller/   endpoints REST (@PreAuthorize por rol)
├── exception/    excepciones + GlobalExceptionHandler (400/401/403/404/409)
└── event/        ReservationCreatedEvent + listener @Async
```

---

## 4. Receta: el enunciado pide una entidad nueva (ej. `Student`)

Crea **6 archivos** copiando los de Product y cambiando nombres (Ctrl+R para reemplazar):

1. `model/Student.java`
2. `repository/StudentRepository.java`
3. `dto/StudentRequest.java`
4. `dto/StudentResponse.java`
5. `service/StudentService.java`
6. `controller/StudentController.java`

Truco rápido en IntelliJ: clic derecho sobre `Product.java` → **Copy** → Paste en la misma carpeta → nombre `Student`,
luego **Ctrl+R** (reemplazar) `Product` → `Student` y `product` → `student`.

### Plantilla de entidad
```java
package com.utec.dbp.model;

import jakarta.persistence.*;

@Entity
@Table(name = "students")
public class Student {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    private Integer age;

    public Student() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public Integer getAge() { return age; }
    public void setAge(Integer age) { this.age = age; }
}
```
> Tip: en IntelliJ, **Alt+Insert → Getter and Setter** genera todos los getters/setters.

### Plantilla de DTOs
```java
public record StudentRequest(
        @NotBlank String name,
        @NotBlank @Email String email,
        @NotNull @Min(16) @Max(99) Integer age
) {}

public record StudentResponse(Long id, String name, String email, Integer age) {
    public static StudentResponse from(Student s) {
        return new StudentResponse(s.getId(), s.getName(), s.getEmail(), s.getAge());
    }
}
```

---

## 5. Snippets frecuentes

### Relación 1 a N (Curso tiene muchos Estudiantes)
```java
// En Student (lado "muchos", dueño de la FK):
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "course_id")
private Course course;

// En Course (lado "uno"), opcional:
@OneToMany(mappedBy = "course", cascade = CascadeType.ALL, orphanRemoval = true)
private List<Student> students = new ArrayList<>();
```
> ⚠️ Nunca devuelvas entidades con relaciones bidireccionales en el JSON (bucle infinito). Usa DTOs.

### Relación N a N
```java
@ManyToMany
@JoinTable(name = "student_course",
        joinColumns = @JoinColumn(name = "student_id"),
        inverseJoinColumns = @JoinColumn(name = "course_id"))
private Set<Course> courses = new HashSet<>();
```

### Query methods útiles (en el Repository, sin escribir SQL)
```java
List<X> findByNameContainingIgnoreCase(String text);
List<X> findByPriceGreaterThanEqual(Double min);
List<X> findByCreatedAtAfter(LocalDateTime date);
List<X> findByStatusOrderByCreatedAtDesc(Status s);
List<X> findTop5ByOrderByPriceDesc();
long countByCategory(Category c);
boolean existsByEmail(String email);
Optional<X> findByEmail(String email);
List<Order> findByProductId(Long productId);   // navega por la relación
```

### Paginación
```java
// Controller
@GetMapping
public Page<ProductResponse> list(@RequestParam(defaultValue = "0") int page,
                                  @RequestParam(defaultValue = "10") int size) {
    return productRepository.findAll(PageRequest.of(page, size, Sort.by("id")))
            .map(ProductResponse::from);
}
```

### Evento + listener async (patrón de la semana 5)
```java
// 1. El evento (record)
public record StudentRegisteredEvent(Long id, String email) {}

// 2. Publicarlo en el Service
private final ApplicationEventPublisher eventPublisher;   // inyectar por constructor
eventPublisher.publishEvent(new StudentRegisteredEvent(saved.getId(), saved.getEmail()));

// 3. Escucharlo
@Component
public class StudentListener {
    private static final Logger log = LoggerFactory.getLogger(StudentListener.class);

    @Async
    @EventListener                     // o @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onRegistered(StudentRegisteredEvent e) {
        log.info("Enviando email de bienvenida a {}", e.email());
    }
}
```
`@EnableAsync` ya está en `ExamenApplication`.

### Tarea programada (si piden algo "cada X segundos")
```java
// Agrega @EnableScheduling en ExamenApplication
@Scheduled(fixedRate = 60000)   // cada 60 s
public void revisarStock() { ... }
```

### Excepciones → códigos HTTP (ya configurado)
| Lanzas | Responde |
|---|---|
| `new ResourceNotFoundException("...")` | 404 |
| `new BadRequestException("...")` | 400 |
| `new ConflictException("...")` | 409 |
| DTO con `@Valid` inválido | 400 con `fieldErrors` |

¿Te piden otro código? Copia `ConflictException` y agrega su `@ExceptionHandler` en `GlobalExceptionHandler`.

### Validaciones disponibles
`@NotNull` `@NotBlank` `@NotEmpty` `@Email` `@Size(min,max)` `@Min` `@Max` `@Positive` `@PositiveOrZero`
`@Past` `@Future` `@Pattern(regexp="...")` → siempre con `@Valid` en el parámetro `@RequestBody` del controller.

---

## 6. Si algo falla

| Síntoma | Solución |
|---|---|
| `Connection to localhost:5432 refused` | Docker Desktop no está abierto o no corriste `docker compose up -d` |
| `password authentication failed` | Cambiaste el `.env` después de crear el volumen → `docker compose down -v` y `up -d` |
| `Port 8080 was already in use` | Otra app corriendo: detén el ▶ anterior (cuadrado rojo) o pon `SERVER_PORT=8081` en `.env` |
| `port is already allocated` al hacer `docker compose up` | Otro Postgres de un lab anterior: `docker ps` → `docker stop <nombre>`, o cambia `DB_PORT` en `.env` |
| `no configuration file provided` | Estás en la carpeta equivocada: `cd` a donde está `docker-compose.yml` |
| Clases en rojo / `cannot find symbol jakarta...` | Panel Maven → 🔄 Reload |
| Error de Java/`release 21 not supported` | Project Structure → SDK = JDK 21 o superior |
| Tests fallan con `Java 26 ... not supported` (Mockito) | Run → Edit Configurations → VM options: `-Dnet.bytebuddy.experimental=true` |
| `relation "xxx" does not exist` | Revisa `@Table(name=...)`; `ddl-auto=update` crea tablas al arrancar |
| JSON infinito / StackOverflow | Estás devolviendo entidades con relaciones → usa DTO |
| 415 Unsupported Media Type en Postman | Body → **raw** → **JSON** (no Text) |

### Reiniciar BD desde cero
```bash
docker compose down -v
docker compose up -d
```

---

## 7. Subir a GitHub (si el examen lo pide)

Si descargaste el repo del examen como ZIP (no con `git clone`):
```bash
git init
git add .
git commit -m "examen"
git branch -M main
git remote add origin https://github.com/CS2031-DBP/<repo-del-examen>.git
git push -u origin main
```
Mejor aún: si te dan un repo, haz **`git clone <url>`** y copia dentro solo lo que te falte de este proyecto
(`pom.xml` dependencias, `docker-compose.yml`, `.env`, carpetas `exception/`, etc.).
