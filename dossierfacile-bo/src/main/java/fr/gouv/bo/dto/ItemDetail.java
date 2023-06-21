package fr.gouv.bo.dto;

import fr.gouv.bo.utils.MarkdownUtil;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ItemDetail {

    private boolean check;
    private String message;
    private Integer idOptionMessage;

    public String getFormattedMessage() {
        return MarkdownUtil.markdownToHtml(message);
    }

}
