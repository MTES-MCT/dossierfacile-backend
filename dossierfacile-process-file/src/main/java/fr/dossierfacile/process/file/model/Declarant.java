package fr.dossierfacile.process.file.model;

public class Declarant {
    private String nom;
    private String nomNaissance;
    private String prenoms;
    private String dateNaissance;

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getNomNaissance() {
        return nomNaissance;
    }

    public void setNomNaissance(String nomNaissance) {
        this.nomNaissance = nomNaissance;
    }

    public String getPrenoms() {
        return prenoms;
    }

    public void setPrenoms(String prenoms) {
        this.prenoms = prenoms;
    }

    public String getDateNaissance() {
        return dateNaissance;
    }

    public void setDateNaissance(String dateNaissance) {
        this.dateNaissance = dateNaissance;
    }

    @Override
    public String toString() {
        if (nomNaissance != null) {
            return "name: " + nom + " " + prenoms + ", nameOfBirth: " + nomNaissance;

        }
        if (nom != null) {
            return "name: " + nom + " " + prenoms;
        }
        return "";
    }
}
