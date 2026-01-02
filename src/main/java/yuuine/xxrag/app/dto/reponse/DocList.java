package yuuine.xxrag.app.dto.reponse;

import lombok.Data;
import yuuine.xxrag.app.docService.entity.RagDocuments;

import java.util.List;

@Data
public class DocList {

    private List<RagDocuments> docs;

}
