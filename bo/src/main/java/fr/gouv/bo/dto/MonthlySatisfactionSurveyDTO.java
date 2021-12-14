package fr.gouv.bo.dto;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class MonthlySatisfactionSurveyDTO {

    private int contacted;
    private int replies;
    private double replyRate;
    private int nbYes;
    private double nbYesRepliesRatio;

}
