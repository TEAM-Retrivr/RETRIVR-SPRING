package retrivr.retrivrspring.application.vo;

import java.util.UUID;

public record LockerSearchResultWithScore(UUID lockerId, Double score) {}
