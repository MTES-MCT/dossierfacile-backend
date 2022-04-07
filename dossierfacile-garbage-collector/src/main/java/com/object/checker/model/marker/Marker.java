package com.object.checker.model.marker;


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
@Table(name = "marker")
public class Marker implements Serializable {

    private static final long serialVersionUID = -3603815939453106021L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "path")
    private String path;

}

/*
  CREATE TABLE public.marker
            (
            id bigserial NOT NULL,
            path text NULL,
            CONSTRAINT path_pkey PRIMARY KEY (id)
            );
*/
