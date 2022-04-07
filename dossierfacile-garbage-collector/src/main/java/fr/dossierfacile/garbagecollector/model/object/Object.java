package fr.dossierfacile.garbagecollector.model.object;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import java.io.Serializable;

@Getter
@Setter
@Entity
@Table(name = "object")
public class Object implements Serializable {

    private static final long serialVersionUID = 609998597255420002L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "path")
    private String path;

    @Column(columnDefinition = "boolean default false")
    private boolean to_delete = false;
}
/*
  CREATE TABLE public.object
            (
            id bigserial NOT NULL,
            path text NULL,
            to_delete bool NULL DEFAULT false,
            CONSTRAINT path_pkey PRIMARY KEY (id)
            );
*/