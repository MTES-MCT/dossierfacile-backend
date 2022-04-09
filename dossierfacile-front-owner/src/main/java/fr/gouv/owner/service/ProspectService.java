package fr.gouv.owner.service;



import fr.dossierfacile.common.entity.*;
import fr.dossierfacile.common.enums.TenantSituation;
import fr.gouv.owner.dto.CoProspectDTO;
import fr.gouv.owner.dto.ProspectDTO;
import fr.gouv.owner.repository.PropertyRepository;
import fr.gouv.owner.repository.ProspectRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

import static fr.dossierfacile.common.enums.TenantType.CREATE;

@Service
@Slf4j
public class ProspectService {
    @Autowired
    private ProspectRepository prospectRepository;
    @Autowired
    private PropertyRepository propertyRepository;
    @Autowired
    private PropertyService propertyService;
    @Autowired
    private ApartmentSharingService apartmentSharingService;
    @Autowired
    private MailService mailService;

    public Prospect find(Long id) {
        return prospectRepository.getOne(id);
    }

    private void updateProspect(ProspectDTO prospectDTO, Prospect prospect) {
        prospect.setFirstName(prospectDTO.getFirstName());
        prospect.setLastName(prospectDTO.getLastName());
        prospect.setEmail(prospectDTO.getEmail().toLowerCase());
        prospect.setPhone(prospectDTO.getPhone());
        prospect.setGuarantor(prospectDTO.getGuarantor());
        prospect.setSalary(prospectDTO.getSalary());

        if (prospectDTO.getSituation() != null) {
            prospect.setTenantSituation(TenantSituation.values()[prospectDTO.getSituation()]);
        } else {
            prospect.setTenantSituation(TenantSituation.CDD);
        }
    }

    private void createCoProspects(Prospect prospect, List<CoProspectDTO> coProspectDTOList) {
        for (CoProspectDTO coProspectDTO : coProspectDTOList) {
            Prospect coProspect = new Prospect(coProspectDTO.getFirstName(),coProspectDTO.getLastName(),coProspectDTO.getEmail().toLowerCase(), prospect);
            this.save(coProspect);
        }
    }

    public Prospect save(Prospect prospect) {
        return prospectRepository.save(prospect);
    }

    public Prospect createProspect(ProspectDTO prospectDTO, boolean fromAgentAccount) {
        Property property = propertyService.findOne(prospectDTO.getPropertyId());
        Prospect prospect = prospectRepository.findFirst1ByEmailAndProperty(prospectDTO.getEmail(), property);
        if (prospect != null) {
            updateProspect(prospectDTO, prospect);
        } else {
            prospect = new Prospect(
                    prospectDTO.getFirstName(), prospectDTO.getLastName(), prospectDTO.getEmail().toLowerCase(),
                    prospectDTO.getPhone(), prospectDTO.getSalary(), prospectDTO.getSituation());

            prospect.setProperty(property);

            prospect.setPropertyId(property.getPropertyId());
        }
        if (fromAgentAccount) {
            mailService.sendEmailForGiveAccessToProperty(prospect);
        } else {
            mailService.sendEmailForGiveAccessToPropertyOwner(prospect);
            property.setCantEmailSentProspect(property.getCantEmailSentProspect() + 1);
            propertyRepository.save(property);
        }
        apartmentSharingService.createApartmentSharing(prospect);
        this.save(prospect);
        this.createCoProspects(prospect, prospectDTO.getCoProspects());
        return prospect;
    }

    public void addTenantToProperty(Tenant tenant, Property property, boolean access) {
        Prospect prospect = this.findByEmailAndProperty(tenant.getEmail(), property);
        if (prospect == null) {
            prospect = new Prospect(tenant);
            prospect.setProperty(property);
            prospect.setPropertyId(property.getPropertyId());
            apartmentSharingService.createApartmentSharing(prospect);
        } else {
            prospect.setFirstName(tenant.getFirstName());
            prospect.setLastName(tenant.getLastName());

            prospect.setGuarantor(tenant.getGuarantors().size() >=1  ? "yes" : "no");

            prospect.setTenantSituation(tenant.getTenantSituation());
            prospect.setSalary(tenant.getTotalSalary());
        }
        prospect.setAccessFull(access);
        prospect.setTenant(tenant);
        prospect.setSubscriptionDate(LocalDateTime.now());
        if (!access) {
            prospect.setTenant(null);
            prospect.setProperty(null);
            prospect.setApartmentSharing(null);
        }
        this.save(prospect);

    }

    public List<Prospect> findAllProspectByProperty(Property property) {
        return prospectRepository.findAllByPropertyAndProspectType(property, CREATE);
    }
    public Prospect findByEmailAndProperty(String email, Property property) {
        return prospectRepository.findFirst1ByEmailAndProperty(email, property);
    }
    public void delete(Long id) {
        prospectRepository.deleteById(id);
    }

    public void saveCustomMessage(Prospect prospect, ProspectDTO prospectDTO) {
        prospect.setCustomMessage(prospectDTO.getCustomMessage());
        prospectRepository.save(prospect);
    }

}
