package org.joel.kimwanyisacco.authentication.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.joel.kimwanyisacco.common.model.BaseEntity;

@Entity
@Table(name = "roles")
public class Role extends BaseEntity {

    private String name;
    private String description;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
