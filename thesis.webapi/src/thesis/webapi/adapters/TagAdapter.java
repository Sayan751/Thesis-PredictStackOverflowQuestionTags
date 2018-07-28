package thesis.webapi.adapters;

import java.util.ArrayList;
import java.util.Collection;

import thesis.data.entities.Tag;
import thesis.webapi.dto.TagDto;

public class TagAdapter {
	public static TagDto toDto(Tag tag) {
		TagDto retVal = new TagDto();
		retVal.id = tag.getId();
		retVal.createdOn = tag.getCreatedOn()
				.toString();
		retVal.index = tag.getIndex();
		retVal.name = tag.getName();
		return retVal;
	}

	public static Collection<TagDto> toDto(Collection<Tag> tags) {
		Collection<TagDto> retVal = new ArrayList<TagDto>();
		tags.forEach(tag -> retVal.add(toDto(tag)));
		return retVal;
	}
}
