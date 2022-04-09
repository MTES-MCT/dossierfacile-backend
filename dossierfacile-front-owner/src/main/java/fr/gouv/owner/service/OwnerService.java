package fr.gouv.owner.service;

import fr.dossierfacile.common.entity.*;
import fr.dossierfacile.common.enums.StepRegisterOwner;
import fr.gouv.owner.dto.OwnerDTO;
import fr.gouv.owner.register_owner.RegisterOwnerFactory;
import fr.gouv.owner.repository.OwnerRepository;
import fr.gouv.owner.repository.TenantRepository;
import fr.gouv.owner.repository.UserRepository;
import fr.gouv.owner.security.SubscriptionStatus;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.security.Principal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static fr.dossierfacile.common.enums.TenantType.CREATE;

@Service
@AllArgsConstructor
@Slf4j
public class OwnerService {

    private final OwnerRepository ownerRepository;
    private final UserRepository userRepository;
    private final PropertyService propertyService;
    private final ApartmentSharingService apartmentSharingService;
    private final ProspectService prospectService;
    private final RegisterOwnerFactory registerOwnerFactory;
    private final TenantRepository tenantRepository;

    public void saveOwner(Owner owner){ ownerRepository.save(owner); }

    public Owner getOwner(Principal principal){return ownerRepository.findOneByEmail(principal.getName());}

    public void linkOwnerTenant(Property property, String token) {
        if (null == token || !token.equals("")) {
            ApartmentSharing apartmentSharing = apartmentSharingService.findApartmentSharingByBothToken(token);
            if (apartmentSharing != null) {
                Tenant tenant = tenantRepository.findOneByApartmentSharingAndTenantType(apartmentSharing, CREATE);
                prospectService.addTenantToProperty(tenant, property, apartmentSharing.getToken().equals(token));
            }
        }
    }

    public void deleteOwner(Owner owner) {
        ownerRepository.delete(owner);
    }

    public String getStatusOwnerLink(String email, Owner ownerToken) {
        User user = userRepository.findOneByEmail(email);
        if (user instanceof Tenant) {
            Tenant tenant = (Tenant) user;
            ApartmentSharing apartmentSharing = tenant.getApartmentSharing();
            Tenant principalTenant = tenantRepository.findOneByApartmentSharingAndTenantType(apartmentSharing, CREATE);
            if (!principalTenant.equals(tenant)) {
                return "Demandez au créateur de la colocation pour donner accès à votre dossier";
            }
        } else if (user instanceof Owner) {
            if (ownerToken.equals(user)) {
                return "Ceci est le lien que vous devez transmettre à vos locataires. Ces derniers pourront alors s'inscrire auprès de vous et apparaitront dans votre page personnelle";
            } else {
                return "Vous ne pouvez accéder à cette page";
            }
        }
        return null;
    }

    public Long countCreatedAccount() {
        return ownerRepository.countOwners();
    }

    public Owner findByEmail(String email) {
        return ownerRepository.findOneByEmail(email);
    }

    public Owner find(int id) {
        return ownerRepository.getOne(id);
    }

    private Map<String, List<List<String>>> weekStatistic(Owner owner, List<List<String>> rangeOfWeeks, int weekNumber) {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter dayMonthFormat = DateTimeFormatter.ofPattern("ddMMM");
        Map<String, List<List<String>>> weekStatistics = new LinkedHashMap<>();
        for (List<String> startDayAndEndDayOfWeek : rangeOfWeeks) {
            String startDayOfWeek = startDayAndEndDayOfWeek.get(0);
            String endDayOfWeek = startDayAndEndDayOfWeek.get(1);

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            LocalDateTime startDate = LocalDateTime.parse(startDayOfWeek + " 00:00:00", formatter);
            LocalDateTime endDate = LocalDateTime.parse(endDayOfWeek + " 23:59:59", formatter);

            String date = now.getYear() + "/" + weekNumber + " " + startDate.format(dayMonthFormat) + "-" + endDate.format(dayMonthFormat);

            List<List<String>> propertyStatistics = addPropertyStatistic(owner, endDate, startDate);

            if (!propertyStatistics.isEmpty()) {
                weekStatistics.put(date, propertyStatistics);
            }
            weekNumber++;
        }
        return weekStatistics;
    }

    private List<List<String>> addPropertyStatistic(Owner owner, LocalDateTime endDate, LocalDateTime startDate) {
        List<List<String>> propertyStatistics = new ArrayList<>();
        List<Property> propertyList = propertyService.findAllByOwner(owner);
        for (Property property : propertyList) {
            List<Prospect> prospectList = prospectService.findAllProspectByProperty(property).stream().filter(p -> (p.getSubscriptionDate().isEqual(endDate) || p.getSubscriptionDate().isBefore(endDate)) && (p.getSubscriptionDate().isEqual(startDate) || p.getSubscriptionDate().isAfter(startDate))).collect(Collectors.toList());
            int allSubscriptions = prospectList.size();
            if (allSubscriptions > 0) {
                List<String> statistic = new ArrayList<>();
                String propertyName = property.getName();
                statistic.add(0, propertyName);
                statistic.add(1, Integer.toString(allSubscriptions));
                propertyStatistics.add(statistic);
            }
        }
        return propertyStatistics;
    }

    public void save(Owner owner) {
        ownerRepository.save(owner);
    }

    public Owner saveStep(OwnerDTO ownerDTO, StepRegisterOwner step) {
        return registerOwnerFactory.get(step.getLabel()).saveStep(ownerDTO);
    }

    public Owner findBySlug(String slug) {
        return ownerRepository.findBySlug(slug);
    }

    public  SubscriptionStatus subscribeTenant(String token, boolean access, Tenant tenant) {
        Property property = propertyService.findOneByToken(token);
        if (property == null) {
            return SubscriptionStatus.TOKEN_DOES_NOT_EXIST;
        }
        prospectService.addTenantToProperty(tenant, property, access);
        return SubscriptionStatus.SUCCESS;
    }
}
