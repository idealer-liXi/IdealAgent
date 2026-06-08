package com.idealagent.domain.ai.model.dto;

import java.util.List;

public record CanvasSaveDTO(
        List<CanvasNodeDTO> nodes,
        List<CanvasRelationDTO> relations,
        List<CanvasRelationDTO> deletedRelations) {
}
