package fr.dossierfacile.garbagecollector.model.file;

import fr.dossierfacile.common.entity.StorageFile;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import java.io.Serializable;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Table(name = "file")
public class GarbageFile implements Serializable {

    private static final long serialVersionUID = -6823677462929911744L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(cascade = CascadeType.REMOVE)
    @JoinColumn(name = "storage_file_id")
    private StorageFile storageFile;

    @OneToOne(cascade = CascadeType.REMOVE)
    @JoinColumn(name = "preview_file_id")
    private StorageFile preview;


}
