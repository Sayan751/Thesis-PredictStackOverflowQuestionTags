package thesis.webapi.dto;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class PredictionDto {
	public UUID learnerId;
	public UUID questionId;
	public List<TagDto> general;
	public List<TagDto> topk;

	public PredictionDto() {
		initCollection(0);
	}

	public PredictionDto(UUID learnerId, UUID questionId, int k) {
		this.learnerId = learnerId;
		this.questionId = questionId;
		initCollection(k);
	}

	private void initCollection(int k) {
		general = new ArrayList<>();
		topk = new ArrayList<>(Collections.nCopies(k, null));
	}
}
