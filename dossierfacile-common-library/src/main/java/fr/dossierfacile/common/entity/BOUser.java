package fr.dossierfacile.common.entity;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.io.Serializable;

@Entity
@Table(name="bo_user")
@DiscriminatorValue("BO")
public class BOUser extends User implements Serializable {
}
