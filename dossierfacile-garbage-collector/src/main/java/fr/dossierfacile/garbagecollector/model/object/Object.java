package fr.dossierfacile.garbagecollector.model.object;

import com.fasterxml.jackson.annotation.JsonView;
import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.jpa.datatables.mapping.DataTablesOutput;

@Getter
@Setter
@Entity
@Table(name = "object")
public class Object implements Serializable {

    private static final long serialVersionUID = 609998597255420002L;

    @JsonView(DataTablesOutput.View.class)
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonView(DataTablesOutput.View.class)
    @Column(name = "path")
    private String path;

    @Column(name = "to_delete", columnDefinition = "boolean default false")
    private boolean toDelete = false;
}