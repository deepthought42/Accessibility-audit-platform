package com.looksee.models.dto;

import com.looksee.models.ActionOLD;
import com.looksee.models.Element;
import com.looksee.models.LookseeObject;
import com.looksee.models.PageState;
import com.looksee.models.Test;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Data transfer object for {@link Test} object that is designed to comply with
 * the data format for browser extensions
 *
 * invariant: path != null after parameterized construction
 */
@Getter
@Setter
@NoArgsConstructor
public class TestDto {
	private String key;
	private String name;
	private List<Object> path;

	/**
	 * Constructor for {@link TestDto}
	 * @param test the {@link Test} to convert
	 *
	 * precondition: test != null
	 */
	public TestDto(Test test){
		assert test != null;

		setKey(test.getKey());
		setName(test.getName());

		this.path = new ArrayList<Object>();

		List<LookseeObject> path_objects = test.getPathObjects();
		List<LookseeObject> ordered_path_objects = new ArrayList<LookseeObject>();
		//order by key
		for(String key : test.getPathKeys()){
			for(LookseeObject obj : path_objects){
				if(obj.getKey().equals(key)){
					ordered_path_objects.add(obj);
				}
			}
		}

		boolean first_page = true;
		for(int idx = 0; idx < ordered_path_objects.size(); idx++){
			if(ordered_path_objects.get(idx) instanceof PageState && first_page){
				first_page = false;
				this.path.add(new PageStateDto((PageState)ordered_path_objects.get(idx)));
			}
			else if(ordered_path_objects.get(idx) instanceof Element ){
				this.path.add(new ElementActionDto((Element)ordered_path_objects.get(idx), (ActionOLD)ordered_path_objects.get(++idx)));
			}
		}
	}
}
