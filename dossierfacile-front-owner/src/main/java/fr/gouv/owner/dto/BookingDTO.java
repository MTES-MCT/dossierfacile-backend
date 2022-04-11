package fr.gouv.owner.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class BookingDTO {

    private int agentOperatorId;

    private String visitDate;

    private String hour;

    private int duration;

    private long prospectId;

    private String prospectFirstName;

    private String prospectLastName;

    private int propertyId;

    private String propertyName;
}
