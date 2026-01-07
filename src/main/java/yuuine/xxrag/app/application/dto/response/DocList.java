package yuuine.xxrag.app.application.dto.response;

import lombok.Data;
import yuuine.xxrag.app.domain.model.RagDocuments;

import java.util.List;

@Data
public class DocList {

    private List<RagDocuments> docs;

}
