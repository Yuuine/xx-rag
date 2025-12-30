package yuuine.xxrag.app.dto.response;

import lombok.Data;
import yuuine.ragapp.docService.entity.RagDocuments;

import java.util.List;

@Data
public class DocList {

    private List<RagDocuments> docs;

}
