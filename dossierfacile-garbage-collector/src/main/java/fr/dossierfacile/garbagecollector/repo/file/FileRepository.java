package fr.dossierfacile.garbagecollector.repo.file;

import fr.dossierfacile.garbagecollector.model.file.File;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FileRepository extends JpaRepository<File, Long> {

    @Query(value = "select\n" +
            "EXISTS (select d from Document d where d.name=:path) or \n" +
            "EXISTS (select f from File f where f.path=:path or f.preview=:path) or \n" +
            "EXISTS (select apt from apartment_sharing apt where apt.url_dossier_pdf_document=:path)", nativeQuery = true)
    boolean existsObject(@Param("path") String path);
}
