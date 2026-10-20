package fr.gouv.bo.exception;

public class MonthlySumChangedException extends BadRequestException {
    public MonthlySumChangedException() {
        super("Un montant mensuel a changé depuis l'ouverture du dossier. Rechargez la page de traitement avant de poursuivre.");
    }
}
