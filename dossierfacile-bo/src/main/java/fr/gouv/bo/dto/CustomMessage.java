package fr.gouv.bo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CustomMessage {
    private String timeSpent;
    private List<MessageItem> messageItems = new ArrayList<>();
    private List<GuarantorItem> guarantorItems = new ArrayList<>();
}
