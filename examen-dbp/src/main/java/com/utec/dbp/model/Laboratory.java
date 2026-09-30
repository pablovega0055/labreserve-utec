package com.utec.dbp.model;

import jakarta.persistence.*;

@Entity
@Table(name = "laboratories")
public class Laboratory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(nullable = false)
    private String location;

    // Responsable del laboratorio (TECHNICIAN o ADMIN) -> columna manager_id
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "manager_id", nullable = false)
    private User manager;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LaboratoryStatus status = LaboratoryStatus.ACTIVE;

    public Laboratory() {
    }

    public Laboratory(String name, String location, User manager, LaboratoryStatus status) {
        this.name = name;
        this.location = location;
        this.manager = manager;
        this.status = status;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public User getManager() { return manager; }
    public void setManager(User manager) { this.manager = manager; }
    public LaboratoryStatus getStatus() { return status; }
    public void setStatus(LaboratoryStatus status) { this.status = status; }
}
