package fr.gouv.owner.register_owner;


public interface RegisterOwnerFactory {
    SaveStep get(String step);
}
