package com.utec.dbp.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.utec.dbp.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Test de integracion: levanta toda la app (con seguridad JWT real) sobre H2.
// @Transactional -> cada test hace rollback y no ensucia a los demas.
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class LabReserveIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private UserRepository userRepository;

    private static final String REGISTER_BODY = """
            {"username":"pablo.vega","email":"pablo.vega@utec.edu.pe","password":"pablovega2026"}
            """;

    private String login(String username, String password) throws Exception {
        String body = "{\"username\":\"%s\",\"password\":\"%s\"}".formatted(username, password);
        String json = mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + objectMapper.readTree(json).get("token").asText();
    }

    private void registerPablo() throws Exception {
        mockMvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON).content(REGISTER_BODY))
                .andExpect(status().isCreated());
    }

    @Test
    void register_201_devuelveIdUsernameEmail() throws Exception {
        mockMvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON).content(REGISTER_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.username").value("pablo.vega"))
                .andExpect(jsonPath("$.email").value("pablo.vega@utec.edu.pe"))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void register_duplicado_409() throws Exception {
        registerPablo();
        mockMvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON).content(REGISTER_BODY))
                .andExpect(status().isConflict());
    }

    @Test
    void register_invalido_400() throws Exception {
        String body = """
                {"username":"pablo.vega","email":"no-es-email","password":"123"}
                """;
        mockMvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.email").exists())
                .andExpect(jsonPath("$.fieldErrors.password").exists());
    }

    @Test
    void login_200_devuelveTokenYExpiresIn() throws Exception {
        registerPablo();
        String body = """
                {"username":"pablo.vega","password":"pablovega2026"}
                """;
        mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.expiresIn").value(3600));
    }

    @Test
    void login_passwordIncorrecto_401() throws Exception {
        registerPablo();
        String body = """
                {"username":"pablo.vega","password":"otraClave123"}
                """;
        mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void equipmentSlots_sinToken_401() throws Exception {
        mockMvc.perform(get("/equipment-slots"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void equipmentSlots_tokenInvalido_401() throws Exception {
        mockMvc.perform(get("/equipment-slots").header("Authorization", "Bearer token.falso.123"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void flujoCompleto_laboratorio_turno_busqueda_reserva() throws Exception {
        String adminToken = login("admin", "admin12345");
        String techToken = login("technician", "tech12345");
        registerPablo();
        String studentToken = login("pablo.vega", "pablovega2026");
        Long techId = userRepository.findByUsername("technician").orElseThrow().getId();

        // 1. ADMIN registra laboratorio
        String labBody = """
                {"name":"FabLab","location":"Pabellon A - 3er piso","managerId":%d}
                """.formatted(techId);
        String labJson = mockMvc.perform(post("/laboratories").header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content(labBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("FabLab"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andReturn().getResponse().getContentAsString();
        long labId = objectMapper.readTree(labJson).get("id").asLong();

        // Un STUDENT no puede registrar laboratorios
        mockMvc.perform(post("/laboratories").header("Authorization", studentToken)
                        .contentType(MediaType.APPLICATION_JSON).content(labBody))
                .andExpect(status().isForbidden());

        // 2. TECHNICIAN publica turno
        LocalDateTime start = LocalDateTime.now().plusDays(2).truncatedTo(ChronoUnit.HOURS);
        String slotBody = """
                {"laboratoryId":%d,"equipmentCode":"IMP3D-01","startTime":"%s","endTime":"%s","capacity":1}
                """.formatted(labId, start, start.plusHours(2));
        String slotJson = mockMvc.perform(post("/equipment-slots").header("Authorization", techToken)
                        .contentType(MediaType.APPLICATION_JSON).content(slotBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.laboratoryName").value("FabLab"))
                .andExpect(jsonPath("$.equipmentCode").value("IMP3D-01"))
                .andReturn().getResponse().getContentAsString();
        long slotId = objectMapper.readTree(slotJson).get("id").asLong();

        // Mismo equipo y horario cruzado -> 409
        mockMvc.perform(post("/equipment-slots").header("Authorization", techToken)
                        .contentType(MediaType.APPLICATION_JSON).content(slotBody))
                .andExpect(status().isConflict());

        // 3. STUDENT busca turnos (paginado)
        mockMvc.perform(get("/equipment-slots").param("page", "0").param("size", "5")
                        .header("Authorization", studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id").value(slotId))
                .andExpect(jsonPath("$.content[0].laboratoryName").value("FabLab"))
                .andExpect(jsonPath("$.content[0].equipmentCode").value("IMP3D-01"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(5))
                .andExpect(jsonPath("$.totalElements").value(1));

        // 4. STUDENT reserva -> capacity 1, el turno queda FULL
        String reservationBody = """
                {"slotId":%d,"purpose":"Imprimir prototipo del curso"}
                """.formatted(slotId);
        mockMvc.perform(post("/reservations").header("Authorization", studentToken)
                        .contentType(MediaType.APPLICATION_JSON).content(reservationBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.slotId").value(slotId))
                .andExpect(jsonPath("$.status").value("CONFIRMED"));

        mockMvc.perform(get("/equipment-slots").param("status", "FULL").header("Authorization", studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));

        // Reservar de nuevo el mismo turno -> 409
        mockMvc.perform(post("/reservations").header("Authorization", studentToken)
                        .contentType(MediaType.APPLICATION_JSON).content(reservationBody))
                .andExpect(status().isConflict());

        // 5. Mis reservas
        mockMvc.perform(get("/reservations/me").header("Authorization", studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].laboratoryName").value("FabLab"));
    }
}
