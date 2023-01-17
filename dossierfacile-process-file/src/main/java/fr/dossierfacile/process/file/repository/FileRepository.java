package fr.dossierfacile.process.file.repository;

import fr.dossierfacile.common.entity.File;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FileRepository extends JpaRepository<File, Long> {

}
