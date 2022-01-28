package fr.dossierfacile.api.dossierfacileapiowner.mail;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MailJetModel {
    private String fromEmail;
    private String fromName;
    private String toEmail;
    private String toName;
    private String replyToEmail;
    private String replyToName;
    private String ccEmail;
    private String ccName;
    private String bccEmail;
    private String bccName;
    private String subject;
    private Map<String, String> variables;
    private Integer templateID;
}
